package com.nuanzhua.pet.service.impl;
import com.nuanzhua.pet.common.BusinessSupport;
import com.nuanzhua.pet.common.Db;
import com.nuanzhua.pet.dto.OrderRequests;
import com.nuanzhua.pet.exception.BusinessException;
import com.nuanzhua.pet.service.DispatchService;
import com.nuanzhua.pet.vo.Actor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.*;
import java.util.*;
import static com.nuanzhua.pet.common.Db.number;

/** 派单准入、候选评分和事务派单。 */
@Service
public class DispatchServiceImpl implements DispatchService {
    private static final Logger log=LoggerFactory.getLogger(DispatchServiceImpl.class);
    private final Db db; private final BusinessSupport support; private final ObjectMapper mapper;
    private final RestClient aiClient; private final String aiModel; private final String aiApiKey;
    private final TransactionTemplate transaction;
    public DispatchServiceImpl(Db db,BusinessSupport support,ObjectMapper mapper,
        @Value("${hualong.base-url:https://api.hualong-ai.com/v1}") String baseUrl,
        @Value("${hualong.api-key:}") String aiApiKey,
        @Value("${hualong.model:gpt-6.1-sol}") String aiModel,
        org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.db=db; this.support=support; this.mapper=mapper; this.aiApiKey=aiApiKey; this.aiModel=aiModel;
        this.aiClient=RestClient.builder().baseUrl(baseUrl).defaultHeader("Authorization","Bearer "+aiApiKey).build();
        this.transaction=new TransactionTemplate(transactionManager);
    }
    private Map<String,Object> json(Object value) {
        try { return mapper.readValue(value.toString(),new TypeReference<>() {}); }
        catch(Exception e) { throw new IllegalStateException("快照格式错误",e); }
    }
    /** 逐项校验喂养师接单条件，并返回可复核的原因列表。 */
    public List<String> violations(Map<String,Object> o,long sitterId) {
        var s=db.one("select s.*,u.status as user_status,g.high_risk_enabled,(g.active and g.effective_at<=now()) as grade_enabled from sitter_profile s join app_user u on u.id=s.user_id join sitter_grade g on g.id=s.grade_id where s.id=?",sitterId);
        var reasons=new ArrayList<String>();
        if(!"ACTIVE".equals(s.get("status")) || !"ACTIVE".equals(s.get("user_status"))) reasons.add("账号未启用或不在接单状态");
        if(!Boolean.TRUE.equals(s.get("grade_enabled"))) reasons.add("等级未启用或尚未生效");
        var start=((java.sql.Timestamp)o.get("service_start")).toInstant().atZone(ZoneId.of("Asia/Shanghai"));
        var end=((java.sql.Timestamp)o.get("service_end")).toInstant().atZone(ZoneId.of("Asia/Shanghai"));
        var schedules=db.queryForList("select * from sitter_schedule where sitter_id=? and weekday=?",sitterId,start.getDayOfWeek().getValue());
        if(schedules.isEmpty()) { reasons.add("未设置当日档期"); return reasons; }
        var day=schedules.getFirst();
        if(Boolean.TRUE.equals(day.get("rest"))) reasons.add("当日休息");
        var addr=json(o.get("address_snapshot"));
        double lon=((Number)(day.get("longitude")==null?s.get("longitude"):day.get("longitude"))).doubleValue();
        double lat=((Number)(day.get("latitude")==null?s.get("latitude"):day.get("latitude"))).doubleValue();
        if(distance(lon,lat,((Number)addr.get("longitude")).doubleValue(),((Number)addr.get("latitude")).doubleValue())>((Number)s.get("radius_km")).doubleValue()) reasons.add("超出服务半径");
        if(!start.toLocalDate().equals(end.toLocalDate()) || start.toLocalTime().isBefore(((java.sql.Time)day.get("start_time")).toLocalTime()) || end.toLocalTime().isAfter(((java.sql.Time)day.get("end_time")).toLocalTime())) reasons.add("服务时间不在当日档期内");
        long count=db.id("select count(*) from pet_order where sitter_id=? and id<>? and status in ('PENDING_ACCEPT','ACCEPTED','SERVICING','COMPLETED') and (service_start at time zone 'Asia/Shanghai')::date=?",sitterId,o.get("id"),start.toLocalDate());
        if(count>=number(day,"capacity")) reasons.add("当日容量已用尽");
        if(db.id("select count(*) from pet_order where sitter_id=? and id<>? and status in ('PENDING_ACCEPT','ACCEPTED','SERVICING') and service_start<? and service_end>?",sitterId,o.get("id"),end.toOffsetDateTime(),start.toOffsetDateTime())>0) reasons.add("与现有订单时间冲突");
        var required=new HashSet<>(db.queryForList("select required_category from order_item where order_id=? and required_category is not null",String.class,o.get("id")));
        required.addAll(db.queryForList("select distinct p->>'category' from pet_order o,jsonb_array_elements(o.pet_snapshot) p where o.id=?",String.class,o.get("id")));
        for(String cat:required) {
            boolean risk=Set.of("WALK","MEDICINE","DEWORM").contains(cat);
            if(risk && !Boolean.TRUE.equals(s.get("high_risk_enabled"))) reasons.add(cat+"：当前等级不可执行高风险服务");
            if(db.id("select count(*) from sitter_qualification where sitter_id=? and category=? and status='APPROVED' and real_verified and training_passed and expires_at>now() and (?=false or practice_passed)",sitterId,cat,risk)==0) reasons.add(cat+"：认证缺失、过期或尚未通过");
            if(!risk && db.id("select count(*) from sitter_profile where id=? and categories ?? ?",sitterId,cat)==0) reasons.add(cat+"：不在擅长类目中");
        }
        return reasons;
    }
    /** 使用Haversine公式计算球面距离，返回公里。 */
    public static double distance(double lon1,double lat1,double lon2,double lat2) {
        double a=Math.pow(Math.sin(Math.toRadians(lat2-lat1)/2),2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.pow(Math.sin(Math.toRadians(lon2-lon1)/2),2);
        return 6371*2*Math.atan2(Math.sqrt(a),Math.sqrt(Math.max(0,1-a)));
    }
    @Transactional
    /** 人工派单；校验档期、半径、容量与资质，按订单再喂养师顺序加锁。 */
    public void assign(long orderId,Actor actor,OrderRequests.Assign r) {
        actor.require("dispatch:update"); var o=support.order(orderId,actor,true);
        BusinessSupport.state(o,"DISPATCHING","PENDING_ACCEPT","ACCEPTED");
        // 所有派单先锁订单再锁喂养师，避免并发派单超容量。
        db.one("select id from sitter_profile where id=? for update",r.sitterId());
        var reasons=violations(o,r.sitterId());
        if(!reasons.isEmpty()) throw BusinessException.bad(String.join("；",reasons));
        var s=db.one("select s.user_id,g.commission_rate from sitter_profile s join sitter_grade g on g.id=s.grade_id where s.id=?",r.sitterId());
        db.update("update dispatch_record set status='REASSIGNED',responded_at=now() where order_id=? and status='PENDING'",orderId);
        db.update("insert into dispatch_record(order_id,sitter_id,operator_id,reason) values (?,?,?,?)",orderId,r.sitterId(),actor.id(),r.reason());
        db.update("update pet_order set sitter_id=?,status='PENDING_ACCEPT',dispatch_reason=?::jsonb,accept_deadline=now()+interval '60 seconds',commission_rate=coalesce(commission_rate,?) where id=?",r.sitterId(),db.json(Map.of("mode","MANUAL","reason",r.reason())),s.get("commission_rate"),orderId);
        support.notify(number(s,"user_id"),orderId,"DISPATCH","新任务待接单","请在60秒内接单");
        support.event(orderId,actor,"MANUAL_DISPATCH",Map.of("sitterId",r.sitterId(),"reason",r.reason()));
        support.log(actor,"人工派单","order",orderId,o,r);
    }
    /** 仅对已完成的同宠物服务且无该宠主差评加分。差评按一至二星判定。 */
    private List<Map<String,Object>> rank(Map<String,Object> order) {
        var result=new ArrayList<Map<String,Object>>();
        var address=json(order.get("address_snapshot"));
        var start=((java.sql.Timestamp)order.get("service_start")).toInstant().atZone(ZoneId.of("Asia/Shanghai"));
        var sitters=db.queryForList("select s.id,s.radius_km,s.service_preferences,g.dispatch_weight,coalesce(sc.longitude,s.longitude) as lon,coalesce(sc.latitude,s.latitude) as lat from sitter_profile s join sitter_grade g on g.id=s.grade_id left join sitter_schedule sc on sc.sitter_id=s.id and sc.weekday=? where s.status='ACTIVE' order by s.id",start.getDayOfWeek().getValue());
        for(var sitter:sitters) {
            long id=number(sitter,"id");
            if(!violations(order,id).isEmpty()) continue;
            if(db.id("select count(*) from review where owner_id=? and sitter_id=? and score<=2",order.get("owner_id"),id)>0) continue;
            long familiar=db.id("select count(*) from pet_order h where h.owner_id=? and h.sitter_id=? and h.status='COMPLETED' and exists (select 1 from jsonb_array_elements(h.pet_snapshot) hp join jsonb_array_elements(?::jsonb) cp on hp->>'id'=cp->>'id')",order.get("owner_id"),id,order.get("pet_snapshot").toString());
            double km=distance(((Number)sitter.get("lon")).doubleValue(),((Number)sitter.get("lat")).doubleValue(),((Number)address.get("longitude")).doubleValue(),((Number)address.get("latitude")).doubleValue());
            var reviews=db.one("select count(*) as n,coalesce(sum(score),0) as total from review where sitter_id=?",id);
            long reviewCount=number(reviews,"n");
            double adjustedRating=(((Number)reviews.get("total")).doubleValue()+20)/(reviewCount+5);
            long load=db.id("select count(*) from pet_order where sitter_id=? and id<>? and status in ('PENDING_ACCEPT','ACCEPTED','SERVICING')",id,order.get("id"));
            double history=familiar>0?40:0;
            double location=30*Math.max(0,1-km/((Number)sitter.get("radius_km")).doubleValue());
            double grade=20*(0.5*((Number)sitter.get("dispatch_weight")).doubleValue()/100+0.5*adjustedRating/5);
            double workload=10.0/(1+load);
            var candidate=new LinkedHashMap<String,Object>();
            candidate.put("sitterId",id); candidate.put("servedSamePet",familiar>0);
            candidate.put("servicePreferences",Objects.toString(sitter.get("service_preferences"),""));
            candidate.put("distanceKm",round(km)); candidate.put("reviewCount",reviewCount);
            candidate.put("adjustedRating",round(adjustedRating)); candidate.put("activeOrders",load);
            candidate.put("historyScore",round(history)); candidate.put("distanceScore",round(location));
            candidate.put("gradeScore",round(grade)); candidate.put("loadScore",round(workload));
            candidate.put("totalScore",round(history+location+grade+workload));
            result.add(candidate);
        }
        result.sort(Comparator.<Map<String,Object>>comparingDouble(x->((Number)x.get("totalScore")).doubleValue()).reversed().thenComparingLong(x->number(x,"sitterId")));
        return result;
    }
    private static double round(double value) { return Math.round(value*100.0)/100.0; }
    public List<Map<String,Object>> candidates(long orderId,Actor actor) {
        actor.require("dispatch:update");
        var order=support.order(orderId,actor,false); BusinessSupport.state(order,"DISPATCHING");
        return rank(order);
    }
    @Transactional
    public Map<String,Object> autoAssign(long orderId,Actor actor) {
        actor.require("dispatch:update");
        var order=support.order(orderId,actor,true); BusinessSupport.state(order,"DISPATCHING");
        var ranked=rank(order);
        if(ranked.isEmpty()) throw BusinessException.conflict("没有符合条件的喂养师，请人工处理");
        {
            var candidate=ranked.getFirst();
            long sitterId=number(candidate,"sitterId");
            db.one("select id from sitter_profile where id=? for update",sitterId);
            if(!violations(order,sitterId).isEmpty() || db.id("select count(*) from review where owner_id=? and sitter_id=? and score<=2",order.get("owner_id"),sitterId)>0) throw BusinessException.conflict("候选人状态已变化，请重新派单");
            var sitter=db.one("select s.user_id,g.commission_rate from sitter_profile s join sitter_grade g on g.id=s.grade_id where s.id=?",sitterId);
            db.update("insert into dispatch_record(order_id,sitter_id,operator_id,mode,reason) values (?,?,?,'AUTO',?)",orderId,sitterId,actor.id(),"综合评分自动派单");
            db.update("update pet_order set sitter_id=?,status='PENDING_ACCEPT',dispatch_reason=?::jsonb,accept_deadline=now()+interval '60 seconds',commission_rate=coalesce(commission_rate,?) where id=?",sitterId,db.json(Map.of("mode","AUTO","score",candidate)),sitter.get("commission_rate"),orderId);
            support.notify(number(sitter,"user_id"),orderId,"DISPATCH","新任务待接单","请在60秒内接单");
            support.event(orderId,actor,"AUTO_DISPATCH",candidate);
            support.log(actor,"智能派单","order",orderId,order,candidate);
            return candidate;
        }
    }
    public Map<String,Object> llmAssign(long orderId,Actor actor) {
        actor.require("dispatch:update");
        var order=db.one("select * from pet_order where id=?",orderId);
        BusinessSupport.state(order,"PENDING_PAYMENT","DISPATCHING");
        return selectAndAssign(order,actor);
    }
    private Map<String,Object> selectAndAssign(Map<String,Object> order,Actor actor) {
        if(aiApiKey==null || aiApiKey.isBlank()) throw BusinessException.conflict("未配置大模型 API Key");
        var candidates=rank(order);
        if(candidates.isEmpty()) throw BusinessException.conflict("没有符合条件的喂养师，请人工处理");
        var prompt=new LinkedHashMap<String,Object>();
        prompt.put("order",Map.of("orderId",order.get("id"),"serviceStart",order.get("service_start").toString(),"serviceEnd",order.get("service_end").toString(),"petSnapshot",order.get("pet_snapshot").toString(),"notes",Objects.toString(order.get("notes"),"")));
        prompt.put("candidates",candidates);
        prompt.put("weights",Map.of("history",40,"distance",30,"gradeAndRating",20,"currentLoad",10));
        prompt.put("instruction","只可从 candidates 中选择一个 sitterId。先遵守所有硬性准入条件；若订单备注包含特殊照护要求（例如大型犬、怕生、性格较凶、用药或特殊行为），优先匹配 servicePreferences 中明确有相关经验的喂养师，并将匹配度作为重要因素；没有明确经验时不要推断。再综合总分、同宠服务、距离、等级评分和当前负载。servicePreferences 是自述，只用于推荐，不能替代资质条件。不得虚构信息。仅返回JSON对象：{\"sitterId\":整数,\"reason\":\"简短中文理由\"}。");
        Map<String,Object> response;
        try {
            response=aiClient.post().uri("/chat/completions").body(Map.of(
                "model",aiModel,"temperature",0,
                "messages",List.of(
                    Map.of("role","system","content","你是宠物上门服务派单助手。只能在后端提供的合格候选人中选择，必须严格遵守JSON输出格式。"),
                    Map.of("role","user","content",mapper.writeValueAsString(prompt))
                )
            )).retrieve().body(new ParameterizedTypeReference<Map<String,Object>>() {});
        } catch(Exception e) {
            log.warn("大模型派单调用失败 orderId={} error={}",order.get("id"),e.getMessage());
            throw BusinessException.conflict("大模型派单暂不可用，请稍后重试或人工派单");
        }
        try {
            String content=Objects.toString(((Map<?,?>)((Map<?,?>)((List<?>)response.get("choices")).getFirst()).get("message")).get("content"),"").trim();
            if(content.startsWith("\u0060\u0060\u0060"))
                content=content.replaceFirst("^\u0060\u0060\u0060(?:json)?\\s*","").replaceFirst("\\s*\u0060\u0060\u0060$","");
            var selection=mapper.readValue(content,new TypeReference<Map<String,Object>>() {});
            long sitterId=((Number)selection.get("sitterId")).longValue();
            var candidate=candidates.stream().filter(c->number(c,"sitterId")==sitterId).findFirst()
                .orElseThrow(()->BusinessException.conflict("大模型返回了候选名单之外的喂养师，请重试"));
            String reason=Objects.toString(selection.get("reason"),"大模型综合候选条件后推荐");
            return transaction.execute(status->commitLlmAssignment(order,actor,candidate,reason));
        } catch(BusinessException e) {
            throw e;
        } catch(Exception e) {
            log.warn("大模型派单响应无法解析 orderId={} error={}",order.get("id"),e.getMessage());
            throw BusinessException.conflict("大模型派单结果格式异常，请重试或人工派单");
        }
    }
    private Map<String,Object> commitLlmAssignment(Map<String,Object> snapshot,Actor actor,Map<String,Object> candidate,String reason) {
        long orderId=number(snapshot,"id"), sitterId=number(candidate,"sitterId");
        var order=db.one("select * from pet_order where id=? for update",orderId);
        BusinessSupport.state(order,"PENDING_PAYMENT","DISPATCHING");
        db.one("select id from sitter_profile where id=? for update",sitterId);
        var violations=violations(order,sitterId);
        if(!violations.isEmpty() || db.id("select count(*) from review where owner_id=? and sitter_id=? and score<=2",order.get("owner_id"),sitterId)>0)
            throw BusinessException.conflict("候选喂养师状态已变化，请重新派单");
        var sitter=db.one("select s.user_id,g.commission_rate from sitter_profile s join sitter_grade g on g.id=s.grade_id where s.id=?",sitterId);
        db.update("update dispatch_record set status='REASSIGNED',responded_at=now() where order_id=? and status='PENDING'",orderId);
        db.update("insert into dispatch_record(order_id,sitter_id,operator_id,mode,reason) values (?,?,?,'LLM',?)",orderId,sitterId,actor==null?null:actor.id(),reason);
        db.update("update pet_order set sitter_id=?,status='PENDING_ACCEPT',dispatch_reason=?::jsonb,accept_deadline=now()+interval '60 seconds',commission_rate=coalesce(commission_rate,?) where id=?",sitterId,db.json(Map.of("mode","LLM","reason",reason,"candidate",candidate)),sitter.get("commission_rate"),orderId);
        support.notify(number(sitter,"user_id"),orderId,"DISPATCH","新任务待接单","请在60秒内接单");
        support.event(orderId,actor,"LLM_DISPATCH",Map.of("sitterId",sitterId,"reason",reason,"score",candidate));
        support.log(actor,"大模型智能派单","order",orderId,order,Map.of("sitterId",sitterId,"reason",reason,"score",candidate));
        var result=new LinkedHashMap<>(candidate); result.put("reason",reason); result.put("mode","LLM");
        return result;
    }
    @Scheduled(fixedDelay=10000)
    public void dispatchPendingOrders() {
        if(aiApiKey==null || aiApiKey.isBlank()) return;
        var orders=db.queryForList("select id from pet_order where status in ('PENDING_PAYMENT','DISPATCHING') and sitter_id is null order by created_at limit 10");
        for(var row:orders) {
            long id=number(row,"id");
            try {
                var order=db.one("select * from pet_order where id=?",id);
                selectAndAssign(order,null);
            } catch(Exception e) {
                log.warn("自动大模型派单未完成 orderId={} error={}",id,e.getMessage());
            }
        }
    }
    @Scheduled(fixedDelay=15000) @Transactional
    /** 定时回收超时接单、过期券和会话。 */
    public void expire() {
        var rows=db.queryForList("select id from pet_order where status='PENDING_ACCEPT' and accept_deadline<now() for update skip locked");
        for(var row:rows) {
            long id=number(row,"id");
            db.update("update pet_order set status='DISPATCHING',sitter_id=null,accept_deadline=null where id=?",id);
            db.update("update dispatch_record set status='TIMEOUT',responded_at=now() where order_id=? and status='PENDING'",id);
            support.event(id,null,"ACCEPT_TIMEOUT",Map.of("next","MANUAL_DISPATCH"));
        }
        db.update("update user_coupon set status='EXPIRED' where status='AVAILABLE' and expires_at<=now()");
        db.update("delete from auth_session where expires_at<now()");
    }
}
