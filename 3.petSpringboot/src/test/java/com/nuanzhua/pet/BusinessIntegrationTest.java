package com.nuanzhua.pet;

import com.fasterxml.jackson.databind.*;
import com.nuanzhua.pet.common.Db;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** 使用真实PostgreSQL和独立随机schema，所有用例事务回滚，不向业务表写测试数据。 */
@SpringBootTest(properties={"app.demo-enabled=true","app.bootstrap-password=TestPass2026!","hualong.api-key="})
@AutoConfigureMockMvc
@Transactional
class BusinessIntegrationTest {
    private static final String SCHEMA="pet_test_"+UUID.randomUUID().toString().replace("-","").substring(0,12);
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        String url=System.getenv().getOrDefault("TEST_DB_URL","jdbc:postgresql://localhost:5432/pet_demo");
        r.add("spring.datasource.url",()->url+(url.contains("?")?"&":"?")+"currentSchema="+SCHEMA);
        r.add("spring.datasource.username",()->System.getenv().getOrDefault("TEST_DB_USERNAME","postgres"));
        r.add("spring.datasource.password",()->System.getenv().getOrDefault("TEST_DB_PASSWORD","root"));
        r.add("spring.flyway.schemas",()->SCHEMA);
        r.add("spring.flyway.default-schema",()->SCHEMA);
    }
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired Db db;
    @Autowired com.nuanzhua.pet.service.GradeRuleService gradeRules;
    @Autowired com.nuanzhua.pet.mapper.PetMapper petMapper;
    @Autowired com.nuanzhua.pet.mapper.SitterProfileMapper sitterMapper;
    @Autowired com.nuanzhua.pet.config.Bootstrap bootstrap;
    @Test void sitterCanSwitchBetweenOwnerAndSitterModes() throws Exception {
        String token=login("sitter");
        assertThat(call("GET","/api/auth/me",token,null,200).path("data").path("role").asText()).isEqualTo("SITTER");
        call("POST","/api/auth/switch-role",token,Map.of("role","OWNER"),200);
        var owner=call("GET","/api/auth/me",token,null,200).path("data");
        assertThat(owner.path("role").asText()).isEqualTo("OWNER");
        assertThat(owner.path("canSwitchToSitter").asBoolean()).isTrue();
        call("GET","/api/owner/pets",token,null,200);
        call("POST","/api/auth/switch-role",token,Map.of("role","SITTER"),200);
        assertThat(call("GET","/api/auth/me",token,null,200).path("data").path("role").asText()).isEqualTo("SITTER");
    }
    @Test void customerCanResumeAiAfterHumanHandoff() throws Exception {
        String owner=login("owner");
        long oldId=call("POST","/api/support/conversations",owner,Map.of(),200).path("data").asLong();
        call("POST","/api/support/conversations/"+oldId+"/handoff",owner,null,200);
        long newId=call("POST","/api/support/conversations/"+oldId+"/resume-ai",owner,null,200).path("data").asLong();
        assertThat(newId).isNotEqualTo(oldId);
        assertThat(db.one("select status from support_conversation where id=?",oldId).get("status")).isEqualTo("CLOSED");
        assertThat(db.one("select status from support_conversation where id=?",newId).get("status")).isEqualTo("WAITING_HUMAN");
        call("POST","/api/support/conversations/"+oldId+"/resume-ai",owner,null,409);
        String sitter=login("sitter");
        call("POST","/api/support/conversations/"+oldId+"/resume-ai",sitter,null,403);
    }
    @Autowired com.nuanzhua.pet.service.OperationsService operations;
    /** 喂养师客服复用会话、历史及转人工，并阻止跨账号读取和调用。 */
    @Test void sitterAiCustomerServiceKeepsConversationPrivate() throws Exception {
        String sitter=login("sitter"),owner=login("owner");
        long id=call("POST","/api/support/conversations",sitter,Map.of(),200).path("data").asLong();
        call("GET","/api/messages?conversationId="+id,owner,null,403);
        call("POST","/api/ai/chat",owner,Map.of("conversationId",id,"message","测试"),403);
        call("POST","/api/support/conversations/"+id+"/handoff",owner,null,403);
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        var payload=new java.util.concurrent.atomic.AtomicReference<JsonNode>();
        server.createContext("/api/ai/chat",exchange->{
            payload.set(mapper.readTree(exchange.getRequestBody()));
            byte[] reply="{\"reply\":\"请先核对服务时间与照护要求。\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");
            exchange.sendResponseHeaders(200,reply.length);
            try(var out=exchange.getResponseBody()){out.write(reply);}
        });
        server.start();
        try {
            var controller=new com.nuanzhua.pet.controller.AiChatController(db,
                    operations,
                    "http://127.0.0.1:"+server.getAddress().getPort(),"");
            var actor=new com.nuanzhua.pet.vo.Actor(db.id("select id from app_user where username='sitter'"),"喂养师","SITTER",Set.of());
            controller.chat(actor,new com.nuanzhua.pet.controller.AiChatController.ChatRequest(id,"接单需要注意什么？"));
            assertThat(payload.get().path("context").path("role").asText()).isEqualTo("SITTER");
            var history=call("GET","/api/messages?conversationId="+id,sitter,null,200).path("data").path("list");
            assertThat(history.size()).isEqualTo(2);
            assertThat(history.get(0).path("sender_type").asText()).isEqualTo("AI");
            call("POST","/api/support/conversations/"+id+"/handoff",sitter,null,200);
            call("POST","/api/ai/chat",sitter,Map.of("conversationId",id,"message","测试"),409);
            call("POST","/api/messages",sitter,Map.of("conversationId",id,"content","请人工协助","mediaKeys",List.of()),200);
        } finally { server.stop(0); }
    }
    /** 可选的真实模型联调：AI 服务需已启动，测试数据随事务回滚。 */
    @Test void liveAiCustomerChatPersistsHistory() throws Exception {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("TEST_LIVE_AI")));
        String owner=login("owner");
        long id=call("POST","/api/support/conversations",owner,Map.of(),200).path("data").asLong();
        var result=call("POST","/api/ai/chat",owner,Map.of("conversationId",id,"message","你好，请介绍你能提供什么帮助"),200).path("data");
        assertThat(result.path("assistantMessage").path("content").asText()).isNotBlank();
        var history=call("GET","/api/messages?conversationId="+id+"&size=100",owner,null,200).path("data").path("list");
        assertThat(history.size()).isEqualTo(2);
        assertThat(history.get(0).path("sender_type").asText()).isEqualTo("AI");
        assertThat(history.get(1).path("sender_type").asText()).isEqualTo("USER");
    }
    /** 审核与信息列表共享档案及最新姓名，资质ID独立，审核后两处状态一致。 */
    @Test void admissionAndSitterInformationShareProfile() throws Exception {
        String admin=login("admin"),sitter=login("sitter");
        long id=db.id("select id from sitter_profile where code='S1001'");
        db.update("update sitter_profile set status='PENDING',categories='[\"CAT\"]'::jsonb where id=?",id);
        db.update("update app_user set name='同源审核测试' where id=(select user_id from sitter_profile where id=?)",id);
        for(String module:List.of("audit","sitter")) {
            var row=call("GET","/api/admin/modules/"+module+"?keyword=同源审核测试&status=PENDING",admin,null,200).path("data").path("list").get(0);
            assertThat(row.path("id").asLong()).isEqualTo(id);
            assertThat(row.path("name").asText()).isEqualTo("同源审核测试");
        }
        long q=db.id("select id from sitter_qualification where sitter_id=? and category='CAT'",id);
        db.update("update sitter_qualification set status='PENDING' where id=?",q);
        var admission=Map.of("decision","APPROVE");
        call("POST","/api/admin/sitters/"+id+"/audit",sitter,admission,403);
        var missingQualification=call("POST","/api/admin/sitters/"+id+"/audit",admin,admission,400);
        assertThat(missingQualification.path("message").asText()).contains("分类资质");
        call("POST","/api/admin/qualifications/"+q+"/audit",admin,Map.of("approved",true,"realVerified",true,"trainingPassed",true,"practicePassed",false,"expiresAt",OffsetDateTime.now().plusYears(1),"note","猫类目审核通过"),200);
        call("POST","/api/admin/sitters/"+id+"/audit",admin,admission,200);
        for(String module:List.of("audit","sitter")) {
            var row=call("GET","/api/admin/modules/"+module+"?keyword=同源审核测试&status=ACTIVE",admin,null,200).path("data").path("list").get(0);
            assertThat(row.path("id").asLong()).isEqualTo(id);
        }
        var qualification=call("GET","/api/admin/modules/qualifications?keyword=同源审核测试",admin,null,200).path("data").path("list").get(0);
        assertThat(qualification.path("sitter_id").asLong()).isEqualTo(id);
        assertThat(call("GET","/api/admin/sitters/"+id,admin,null,200).path("data").path("qualifications").toString()).contains("APPROVED");
    }
    /** 驳回意见展示给申请人；修改资料后可用原账号重新提交并回到待审核。 */
    @Test void rejectedSitterCanResubmitApplication() throws Exception {
        String username="reapply_"+UUID.randomUUID().toString().replace("-","").substring(0,12);
        String phone="139"+String.format("%08d",Math.floorMod(System.nanoTime(),100_000_000));
        call("POST","/api/auth/register",null,Map.of("username",username,"password","TestPass2026!","name","复审喂养师","phone",phone),200);
        String token=login(username),admin=login("admin");
        var personal=new LinkedHashMap<String,Object>();
        personal.put("name","复审喂养师"); personal.put("phone",phone); personal.put("emergencyContact","家人 13900000001");
        personal.put("district","南山区"); personal.put("address","科技园 1 栋"); personal.put("longitude",113.95); personal.put("latitude",22.55);
        personal.put("radiusKm",5); personal.put("transport","步行");
        long id=call("POST","/api/sitter/application",token,Map.of("personal",personal,"categories",List.of("CAT")),200).path("data").asLong();

        call("POST","/api/admin/sitters/"+id+"/audit",admin,Map.of("decision","REJECT","note"," "),400);
        call("POST","/api/admin/sitters/"+id+"/audit",admin,Map.of("decision","REJECT","note","请补充可联系的紧急联系人信息"),200);
        var rejected=call("GET","/api/sitter/profile",token,null,200).path("data");
        assertThat(rejected.path("status").asText()).isEqualTo("REJECTED");
        assertThat(rejected.path("admission_review_note").asText()).contains("紧急联系人");

        personal.put("name","复审喂养师已修改"); personal.put("district","福田区"); personal.put("address","车公庙 2 栋");
        personal.put("longitude",114.05); personal.put("latitude",22.54);
        call("POST","/api/sitter/application",token,Map.of("personal",personal,"categories",List.of("DOG")),200);
        var resubmitted=call("GET","/api/sitter/profile",token,null,200).path("data");
        assertThat(resubmitted.path("status").asText()).isEqualTo("PENDING");
        assertThat(resubmitted.has("admission_review_note")).isFalse();
        assertThat(resubmitted.path("district").asText()).isEqualTo("福田区");
        assertThat(resubmitted.path("categories").toString()).contains("DOG");
        var approval=call("POST","/api/admin/sitters/"+id+"/audit",admin,Map.of("decision","APPROVE","note","资料已审核"),400);
        assertThat(approval.path("message").asText()).contains("分类资质");
    }
    /** 超级管理员拥有所有权限，重复初始化不能覆盖密码或重复创建账号。 */
    @Test void superAdminBootstrapIsIdempotent() throws Exception {
        String hash=db.one("select password_hash from app_user where username='admin'").get("password_hash").toString();
        bootstrap.run(new org.springframework.boot.DefaultApplicationArguments());
        assertThat(db.id("select count(*) from app_user where username='admin'")).isEqualTo(1);
        assertThat(db.one("select password_hash from app_user where username='admin'").get("password_hash")).isEqualTo(hash);
        var me=call("GET","/api/auth/me",login("admin"),null,200).path("data");
        assertThat(me.path("role").asText()).isEqualTo("ADMIN");
        assertThat(me.path("permissions").toString()).contains("*");
    }
    /** Swagger必须可匿名访问，包含中文说明、字段定义和Bearer授权入口。 */
    @Test void swaggerDocumentsAndAuthorization() throws Exception {
        var spec=call("GET","/v3/api-docs",null,null,200);
        assertThat(spec.path("components").path("securitySchemes").path("bearerAuth").path("scheme").asText()).isEqualTo("bearer");
        assertThat(spec.path("paths").path("/api/auth/login").path("post").path("security").size()).isZero();
        assertThat(spec.path("paths").path("/api/orders").path("post").path("summary").asText()).contains("创建订单");
        assertThat(spec.path("components").path("schemas").toString()).contains("同宠主下单幂等键");
        var ui=mvc.perform(MockMvcRequestBuilders.get("/swagger-ui/index.html")).andReturn();
        assertThat(ui.getResponse().getStatus()).isEqualTo(200);
    }
    /** 客服会话请求人工后更新队列状态，并阻止AI继续回复。 */
    @Test void aiCustomerChatStopsAfterHumanHandoff() throws Exception {
        String owner=login("owner");
        long conversationId=call("POST","/api/support/conversations",owner,Map.of(),200).path("data").asLong();
        call("POST","/api/support/conversations/"+conversationId+"/handoff",owner,null,200);
        assertThat(db.one("select status from support_conversation where id=?",conversationId).get("status"))
                .isEqualTo("HUMAN_REQUESTED");
        call("POST","/api/ai/chat",owner,Map.of("conversationId",conversationId,"message","请帮我查询退款"),409);
    }
    String login(String name) throws Exception { return call("POST","/api/auth/login",null,Map.of("username",name,"password","TestPass2026!"),200).path("data").path("token").asText(); }
    @Test void smartDispatchRanksAndAssignsOnce() throws Exception {
        String admin=login("admin"),owner=login("owner");
        long id=call("POST","/api/orders",owner,orderBody(owner),200).path("data").path("id").asLong();
        call("POST","/api/orders/"+id+"/demo-pay",owner,null,200);
        db.update("update sitter_profile set service_preferences=?", "熟悉大型犬和怕生宠物照护");
        call("GET","/api/admin/orders/"+id+"/dispatch-candidates",owner,null,403);
        var candidates=call("GET","/api/admin/orders/"+id+"/dispatch-candidates",admin,null,200).path("data");
        assertThat(candidates.size()).isPositive();
        var first=candidates.get(0);
        assertThat(first.path("servicePreferences").asText()).contains("大型犬");
        assertThat(first.path("historyScore").asDouble()).isZero();
        assertThat(first.path("distanceScore").asDouble()).isBetween(0.0,30.0);
        assertThat(first.path("gradeScore").asDouble()).isBetween(0.0,20.0);
        assertThat(first.path("loadScore").asDouble()).isBetween(0.0,10.0);
        var assigned=call("POST","/api/admin/orders/"+id+"/auto-dispatch",admin,null,200).path("data");
        assertThat(assigned.path("sitterId").asLong()).isEqualTo(first.path("sitterId").asLong());
        assertThat(db.id("select count(*) from dispatch_record where order_id=? and mode='AUTO'",id)).isEqualTo(1);
        call("POST","/api/admin/orders/"+id+"/auto-dispatch",admin,null,409);
    }
    JsonNode call(String method,String url,String token,Object body,int status) throws Exception {
        var request=MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method),url).contentType("application/json");
        if(token!=null) request.header("Authorization","Bearer "+token);
        if(body!=null) request.content(mapper.writeValueAsString(body));
        var result=mvc.perform(request).andReturn();
        assertThat(result.getResponse().getStatus()).withFailMessage("%s %s: %s",method,url,result.getResponse().getContentAsString()).isEqualTo(status);
        return mapper.readTree(result.getResponse().getContentAsString());
    }
    Map<String,Object> orderBody(String token) throws Exception {
        long pet=call("POST","/api/owner/pets",token,Map.of("name","年糕","category","CAT"),200).path("data").asLong();
        long address=call("POST","/api/owner/addresses",token,Map.of("contactName","测试宠主","phone","13800000001","district","南山区","address","科技园","longitude",113.95,"latitude",22.55),200).path("data").asLong();
        long base=db.id("select id from service_item where code='BASE'");
        var start=OffsetDateTime.now().plusMinutes(5);
        // 测试档期覆盖全天，避免测试执行时间影响派单。
        db.update("update sitter_schedule set start_time='00:00',end_time='23:59:59'");
        var map=new LinkedHashMap<String,Object>();
        map.put("requestKey",UUID.randomUUID().toString()); map.put("addressId",address); map.put("petIds",List.of(pet));
        map.put("serviceStart",start); map.put("serviceEnd",start.plusMinutes(30));
        map.put("items",List.of(Map.of("serviceId",base,"quantity",1)));
        map.put("agreementVersion","v1"); map.put("agreementAccepted",true); map.put("authorizationScope","仅限本次上门喂养"); return map;
    }
    @Test void baseMapperCrudAndPostgresJson() {
        long ownerId=db.id("select id from app_user where username='owner'");
        var pet=new com.nuanzhua.pet.entity.Pet(); pet.setOwnerId(ownerId); pet.setName("Mapper测试"); pet.setCategory("CAT");
        assertThat(petMapper.insert(pet)).isEqualTo(1);
        assertThat(pet.getId()).isPositive();
        assertThat(petMapper.selectById(pet.getId()).getName()).isEqualTo("Mapper测试");
        pet.setName("已更新"); petMapper.updateById(pet);
        assertThat(petMapper.selectById(pet.getId()).getName()).isEqualTo("已更新");
        petMapper.deleteById(pet.getId()); assertThat(petMapper.selectById(pet.getId())).isNull();
        long sitterId=db.id("select id from sitter_profile where code='S1001'");
        var sitter=sitterMapper.selectById(sitterId);
        assertThat(sitter.getCategories()).contains("CAT");
        sitter.setCategories("[\"CAT\"]"); sitterMapper.updateById(sitter);
        assertThat(sitterMapper.selectById(sitterId).getCategories()).isEqualTo("[\"CAT\"]");
    }
    /** 后台宠主详情包含关联档案，但不得泄漏认证信息或越权读取。 */
    @Test void ownerDetailsAndDemoCapabilities() throws Exception {
        String owner=login("owner"),admin=login("admin"),sitter=login("sitter");
        orderBody(owner);
        long id=db.id("select id from app_user where username='owner'");
        var detail=call("GET","/api/admin/owners/"+id,admin,null,200).path("data");
        assertThat(detail.path("pets").size()).isEqualTo(1);
        assertThat(detail.path("addresses").size()).isEqualTo(1);
        assertThat(detail.has("password_hash")).isFalse();
        assertThat(detail.path("order_summary").path("orders").asInt()).isZero();
        call("GET","/api/admin/owners/"+id,owner,null,403);
        call("GET","/api/admin/owners/"+id,sitter,null,403);
        assertThat(call("GET","/api/public/health",null,null,200).path("data").path("demoPaymentsEnabled").asBoolean()).isTrue();
    }
    @Test void annualGiftsArePaidOnce() throws Exception {
        String owner=login("owner"); long plan=db.id("select id from membership_plan where months=12");
        long p=call("POST","/api/membership/purchases",owner,Map.of("planId",plan),200).path("data").asLong();
        assertThat(db.id("select count(*) from user_coupon")).isZero();
        call("POST","/api/membership/purchases/"+p+"/demo-pay",owner,null,200);
        call("POST","/api/membership/purchases/"+p+"/demo-pay",owner,null,200);
        assertThat(db.id("select count(*) from user_coupon where amount=20 and threshold=60")).isEqualTo(2);
        assertThat(db.id("select count(*) from payment_record where purchase_id=?",p)).isEqualTo(1);
    }
    @Test void gradeRulesWaitUntilSettlementPeriod() throws Exception {
        String admin=login("admin"); long grade=db.id("select id from sitter_grade where code='L2'");
        var body=Map.ofEntries(Map.entry("code","L2"),Map.entry("name","资深"),Map.entry("minOrders",30),Map.entry("minRating",4.5),Map.entry("maxComplaints",1),Map.entry("commissionRate",25),Map.entry("dispatchWeight",75),Map.entry("highRiskEnabled",true),Map.entry("active",true));
        call("PUT","/api/admin/grades/"+grade,admin,body,200);
        assertThat(db.one("select commission_rate from sitter_grade where id=?",grade).get("commission_rate")).isEqualTo(new BigDecimal("18.00"));
        gradeRules.apply();
        assertThat(db.id("select count(*) from sitter_grade_revision where applied_at is null")).isEqualTo(1);
        db.update("update sitter_grade_revision set effective_at=now()-interval '1 second' where grade_id=?",grade);
        gradeRules.apply(); gradeRules.apply();
        assertThat(db.one("select commission_rate from sitter_grade where id=?",grade).get("commission_rate")).isEqualTo(new BigDecimal("25.00"));
        assertThat(db.id("select count(*) from sitter_grade_revision where applied_at is not null")).isEqualTo(1);
    }
    @Test void completeLifecycleSnapshotsAndAuthorization() throws Exception {
        String owner=login("owner"),admin=login("admin"),sitter=login("sitter");
        var body=orderBody(owner);
        long id=call("POST","/api/orders",owner,body,200).path("data").path("id").asLong();
        assertThat(call("POST","/api/orders",owner,body,200).path("data").path("id").asLong()).isEqualTo(id);
        long ownerId=db.id("select id from app_user where username='owner'");
        // 另一宠主不能读取订单，匿名和普通宠主不能访问后台。
        call("POST","/api/auth/register",null,Map.of("username","another","password","Another2026!","name","其他人","phone","13900000009"),200);
        String other=call("POST","/api/auth/login",null,Map.of("username","another","password","Another2026!"),200).path("data").path("token").asText();
        call("GET","/api/orders/"+id,other,null,403);
        call("GET","/api/admin/dashboard",null,null,401); call("GET","/api/admin/dashboard",owner,null,403);
        call("POST","/api/orders/"+id+"/demo-pay",owner,null,200); call("POST","/api/orders/"+id+"/demo-pay",owner,null,200);
        assertThat(db.id("select count(*) from payment_record where order_id=?",id)).isEqualTo(1);
        long sitterId=db.id("select id from sitter_profile where code='S1001'");
        assertThat(call("GET","/api/admin/orders/"+id+"/eligibility/"+sitterId,admin,null,200).path("data").path("eligible").asBoolean()).isTrue();
        call("POST","/api/admin/orders/"+id+"/assign",admin,Map.of("sitterId",sitterId,"reason","人工确认档期与资质"),200);
        db.update("update sitter_grade set commission_rate=40 where code='L2'");
        db.update("update service_item set price=99 where code='BASE'");
        call("POST","/api/orders/"+id+"/accept",sitter,null,200);
        call("POST","/api/orders/"+id+"/checkin",sitter,null,200);
        call("POST","/api/orders/"+id+"/complete",sitter,null,400);
        for(String code:List.of("FEED","WATER","CLEAN","PLAY","DOOR")) call("PUT","/api/orders/"+id+"/checklist",sitter,Map.of("code",code,"completed",true,"quantity",10,"unit","MINUTE"),200);
        call("POST","/api/orders/"+id+"/media",sitter,Map.of("kind","VIDEO","objectKey","orders/"+id+"/recording.mp4"),200);
        call("POST","/api/orders/"+id+"/complete",sitter,null,200);
        assertThat(db.one("select sitter_amount from settlement where order_id=?",id).get("sitter_amount")).isEqualTo(new BigDecimal("28.70"));
        assertThat(call("GET","/api/orders/"+id+"/report",owner,null,200).path("data").path("status").asText()).isEqualTo("PENDING_AI");
        assertThat(db.id("select count(*) from notification where user_id=? and order_id=? and code in ('CHECKIN','CHECKOUT')",ownerId,id)).isEqualTo(2);
        call("POST","/api/orders/"+id+"/review",owner,Map.of("score",4,"content","细心负责"),200);
        call("POST","/api/orders/"+id+"/review",owner,Map.of("score",5,"content","重复评价"),409);
        long settlement=db.id("select id from settlement where order_id=?",id);
        call("POST","/api/admin/settlements/batch-settle",admin,Map.of("ids",List.of(settlement)),200);
        call("POST","/api/admin/orders/"+id+"/demo-refund",admin,Map.of("reason","已结算禁止直接退款"),409);
        call("POST","/api/admin/ai/dispatch",admin,Map.of(),501);
    }
    /** 移动端真实上传、归属隔离、详情聚合和收益查询共同覆盖。 */
    @Test void mobileMediaAndQueriesAreAuthorized() throws Exception {
        String owner=login("owner"),sitter=login("sitter"),admin=login("admin");
        long id=call("POST","/api/orders",owner,orderBody(owner),200).path("data").path("id").asLong();
        call("POST","/api/orders/"+id+"/demo-pay",owner,null,200);
        long sitterId=db.id("select id from sitter_profile where code='S1001'");
        call("POST","/api/admin/orders/"+id+"/assign",admin,Map.of("sitterId",sitterId,"reason","移动端联调派单说明"),200);
        call("POST","/api/orders/"+id+"/accept",sitter,null,200);
        call("POST","/api/orders/"+id+"/checkin",sitter,null,200);
        byte[] bytes=new byte[]{0,0,0,16,102,116,121,112,105,115,111,109,0,0,0,0};
        var file=new org.springframework.mock.web.MockMultipartFile("file","sample.mp4","video/mp4",bytes);
        var forbidden=mvc.perform(MockMvcRequestBuilders.multipart("/api/orders/"+id+"/upload").file(file).header("Authorization","Bearer "+owner)).andReturn();
        assertThat(forbidden.getResponse().getStatus()).isEqualTo(403);
        var invalid=mvc.perform(MockMvcRequestBuilders.multipart("/api/orders/"+id+"/upload").file(new org.springframework.mock.web.MockMultipartFile("file","fake.mp4","video/mp4","not video".getBytes())).header("Authorization","Bearer "+sitter)).andReturn();
        assertThat(invalid.getResponse().getStatus()).isEqualTo(400);
        var uploaded=mvc.perform(MockMvcRequestBuilders.multipart("/api/orders/"+id+"/upload").file(file).header("Authorization","Bearer "+sitter)).andReturn();
        assertThat(uploaded.getResponse().getStatus()).isEqualTo(200);
        long media=mapper.readTree(uploaded.getResponse().getContentAsString()).path("data").path("id").asLong();
        var read=mvc.perform(MockMvcRequestBuilders.get("/api/orders/"+id+"/media/"+media+"/file").header("Authorization","Bearer "+owner)).andReturn();
        assertThat(read.getResponse().getContentAsByteArray()).isEqualTo(bytes);
        call("POST","/api/auth/register",null,Map.of("username","outsider","password","Another2026!","name","其他用户","phone","13900000008"),200);
        String other=call("POST","/api/auth/login",null,Map.of("username","outsider","password","Another2026!"),200).path("data").path("token").asText();
        call("GET","/api/orders/"+id+"/media/"+media+"/file",other,null,403);
        long ticket=call("POST","/api/emergencies",sitter,Map.of("orderId",id,"content","测试异常上报"),200).path("data").asLong();
        assertThat(call("GET","/api/emergencies/mine",owner,null,200).path("data").path("list").get(0).path("id").asLong()).isEqualTo(ticket);
        assertThat(call("GET","/api/emergencies/mine",other,null,200).path("data").path("total").asLong()).isZero();
        call("GET","/api/sitter/settlements",owner,null,403);
        for(String code:List.of("FEED","WATER","CLEAN","PLAY","DOOR")) call("PUT","/api/orders/"+id+"/checklist",sitter,Map.of("code",code,"completed",true,"quantity",10,"unit","MINUTE"),200);
        call("POST","/api/orders/"+id+"/complete",sitter,null,200);
        assertThat(call("GET","/api/sitter/settlements",sitter,null,200).path("data").path("total").asLong()).isEqualTo(1);
        var detail=call("GET","/api/orders/"+id,owner,null,200).path("data");
        assertThat(detail.path("dispatch").path("reason").asText()).isEqualTo("移动端联调派单说明");
        assertThat(detail.path("sitter").path("name").asText()).isNotBlank();
        assertThat(detail.has("settlement")).isFalse();
        assertThat(call("GET","/api/orders/"+id,sitter,null,200).path("data").path("settlement").path("sitter_amount").decimalValue()).isEqualByComparingTo("28.70");
        db.update("update service_media set expires_at=now()-interval '1 second' where id=?",media);
        call("GET","/api/orders/"+id+"/media/"+media+"/file",owner,null,404);
    }
    @Test void couponChainCancelAndFailureLog() throws Exception {
        String owner=login("owner"),admin=login("admin");
        long template=call("POST","/api/admin/coupons",admin,Map.ofEntries(
            Map.entry("name","新客券"),Map.entry("issueType","claim"),Map.entry("amount",10),Map.entry("threshold",0),
            Map.entry("serviceCodes",List.of("BASE")),Map.entry("validDays",30),Map.entry("active",true),Map.entry("audience","NEW"),
            Map.entry("perUserLimit",1),Map.entry("stock",1),Map.entry("claimStartsAt",OffsetDateTime.now().minusDays(1)),Map.entry("claimEndsAt",OffsetDateTime.now().plusDays(1))),200).path("data").asLong();
        var claim=call("POST","/api/coupons/"+template+"/claim",owner,Map.of("deviceId","test-device"),200).path("data");
        assertThat(claim.path("chain").size()).isEqualTo(7);
        long coupon=claim.path("couponId").asLong();
        var retry=call("POST","/api/coupons/"+template+"/claim",owner,Map.of(),200).path("data");
        assertThat(retry.path("failedStep").asInt()).isEqualTo(4);
        assertThat(db.id("select count(*) from coupon_claim_log where template_id=? and not success",template)).isEqualTo(1);
        var body=orderBody(owner); body.put("couponId",coupon);
        long id=call("POST","/api/orders",owner,body,200).path("data").path("id").asLong();
        assertThat(db.one("select payable_amount from pet_order where id=?",id).get("payable_amount")).isEqualTo(new BigDecimal("25.00"));
        assertThat(db.one("select status from user_coupon where id=?",coupon).get("status")).isEqualTo("LOCKED");
        call("POST","/api/orders/"+id+"/cancel",owner,Map.of("reason","行程调整"),200);
        assertThat(db.one("select status from user_coupon where id=?",coupon).get("status")).isEqualTo("AVAILABLE");
    }
    @Test void membershipRenewalAndHumanSupport() throws Exception {
        String owner=login("owner"),admin=login("admin");
        long plan=db.id("select id from membership_plan where months=1");
        long p=call("POST","/api/membership/purchases",owner,Map.of("planId",plan),200).path("data").asLong();
        call("POST","/api/membership/purchases/"+p+"/demo-pay",owner,null,200);
        var before=db.one("select expires_at from membership").get("expires_at");
        call("POST","/api/membership/purchases/"+p+"/demo-pay",owner,null,200);
        assertThat(db.one("select expires_at from membership").get("expires_at")).isEqualTo(before);
        var body=orderBody(owner);
        var o=call("POST","/api/orders",owner,body,200).path("data");
        assertThat(o.path("payable_amount").decimalValue()).isEqualByComparingTo("31.50");
        long conv=call("POST","/api/support/conversations",owner,Map.of(),200).path("data").asLong();
        call("POST","/api/messages",admin,Map.of("conversationId",conv,"content","未接管不可回复"),400);
        call("POST","/api/admin/conversations/"+conv+"/takeover",admin,null,200);
        call("POST","/api/messages",admin,Map.of("conversationId",conv,"content","人工客服为您服务"),200);
        call("GET","/api/messages?conversationId="+conv,owner,null,200);
    }
    @Test void adminListsValidationAndEmergencyConsent() throws Exception {
        String admin=login("admin"),owner=login("owner");
        for(String module:List.of("order","audit","finance","member","plans","benefits","sitter","grades","user","coupon","coupon-logs","price","review","complaints","msg","system","roles","staff","service","emergency","hospitals")) call("GET","/api/admin/modules/"+module+"?keyword=a&status=ACTIVE",admin,null,200);
        call("GET","/api/admin/dictionary",admin,null,200);
        call("GET","/api/admin/modules/order?size=10000",admin,null,400);
        call("POST","/api/admin/plans",admin,Map.of("name","非法价格","displayName","测试","months",1,"price",-1,"active",true),400);
        long order=call("POST","/api/orders",owner,orderBody(owner),200).path("data").path("id").asLong();
        long emergency=call("POST","/api/emergencies",owner,Map.of("orderId",order,"content","宠物呕吐需要帮助"),200).path("data").asLong();
        long hospital=call("POST","/api/admin/hospitals",admin,Map.of("name","测试医院","phone","075500000000","address","南山","longitude",113.95,"latitude",22.55,"emergency24h",true,"active",true),200).path("data").asLong();
        call("PUT","/api/admin/emergencies/"+emergency,admin,Map.of("status","ACCEPTED"),200);
        call("PUT","/api/admin/emergencies/"+emergency,admin,Map.of("status","TRANSPORTING","hospitalId",hospital),400);
        call("POST","/api/emergencies/"+emergency+"/consent",owner,null,200);
        call("PUT","/api/admin/emergencies/"+emergency,admin,Map.of("status","TRANSPORTING","hospitalId",hospital),200);
    }
}
