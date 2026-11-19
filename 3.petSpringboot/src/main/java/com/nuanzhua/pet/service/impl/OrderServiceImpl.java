package com.nuanzhua.pet.service.impl;
import com.nuanzhua.pet.common.BusinessSupport;
import com.nuanzhua.pet.common.Db;
import com.nuanzhua.pet.dto.OrderRequests;
import com.nuanzhua.pet.entity.Review;
import com.nuanzhua.pet.exception.BusinessException;
import com.nuanzhua.pet.service.OrderService;
import com.nuanzhua.pet.vo.Actor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;
import static com.nuanzhua.pet.common.Db.number;
import static com.nuanzhua.pet.common.BusinessSupport.state;

/** 业务服务实现；写操作在服务事务内完成，数据库约束提供最后一致性保护。 */
@Service
public class OrderServiceImpl implements OrderService {
    private final Db db; private final BusinessSupport support;
    public OrderServiceImpl(Db db,BusinessSupport support) { this.db=db; this.support=support; }
    static BigDecimal money(BigDecimal x) { return x.setScale(2,RoundingMode.HALF_UP); }
    @Transactional
    /** 创建订单；按服务端价格计算，冻结协议和价格快照，requestKey保证幂等。 */
    public Map<String,Object> create(Actor actor,OrderRequests.Create r) {
        actor.role("OWNER");
        // 同一宠主串行建单，幂等键不会产生重复订单或重复锁券。
        db.one("select id from app_user where id=? for update",actor.id());
        var old=db.queryForList("select * from pet_order where owner_id=? and request_key=?",actor.id(),r.requestKey());
        if(!old.isEmpty()) return old.getFirst();
        if(!r.serviceStart().isBefore(r.serviceEnd()) || Duration.between(r.serviceStart(),r.serviceEnd()).compareTo(Duration.ofHours(12))>0)
            throw BusinessException.bad("服务开始必须早于结束，单次时段不超过12小时");
        var address=db.one("select * from owner_address where id=? and owner_id=?",r.addressId(),actor.id());
        var pets=new ArrayList<Map<String,Object>>();
        if(new HashSet<>(r.petIds()).size()!=r.petIds().size()) throw BusinessException.bad("宠物不能重复");
        for(long id:r.petIds()) pets.add(db.one("select * from pet where id=? and owner_id=? and not deleted",id,actor.id()));
        var items=new ArrayList<Map<String,Object>>(); var codes=new HashSet<String>(); BigDecimal total=BigDecimal.ZERO;
        for(var requested:r.items()) {
            var item=db.one("select * from service_item where id=? and active=true for share",requested.serviceId());
            if(!codes.add((String)item.get("code"))) throw BusinessException.bad("服务项不能重复");
            item.put("quantity",requested.quantity()); items.add(item);
            total=total.add(((BigDecimal)item.get("price")).multiply(BigDecimal.valueOf(requested.quantity())));
        }
        if(!codes.contains("BASE") || items.stream().anyMatch(x -> "BASE".equals(x.get("code")) && number(x,"quantity")!=1))
            throw BusinessException.bad("必须且只能包含一次基础上门服务");
        int extra=items.stream().filter(x->"EXTRA_PET".equals(x.get("code"))).mapToInt(x->((Number)x.get("quantity")).intValue()).sum();
        if(extra!=pets.size()-1) throw BusinessException.bad("增加宠物服务的数量必须等于宠物数减一");
        BigDecimal memberDiscount=BigDecimal.ZERO;
        if(db.id("select count(*) from membership where owner_id=? and status='ACTIVE' and expires_at>now()",actor.id())>0) {
            var rates=db.queryForList("select discount_rate from membership_benefit where code='SERVICE_DISCOUNT' and enabled",BigDecimal.class);
            if(!rates.isEmpty()) memberDiscount=money(total.multiply(BigDecimal.ONE.subtract(rates.getFirst())));
        }
        BigDecimal couponDiscount=BigDecimal.ZERO;
        if(r.couponId()!=null) {
            var coupon=db.one("select * from user_coupon where id=? and owner_id=? and status='AVAILABLE' and expires_at>now() for update",r.couponId(),actor.id());
            if(total.subtract(memberDiscount).compareTo((BigDecimal)coupon.get("threshold"))<0) throw BusinessException.bad("未满足优惠券门槛");
            if(db.id("select count(*) from user_coupon c where c.id=? and (jsonb_array_length(c.service_codes)=0 or exists(select 1 from jsonb_array_elements_text(c.service_codes) t where t.value=any(?)))",r.couponId(),codes.toArray(String[]::new))==0)
                throw BusinessException.bad("优惠券不适用于本次服务");
            couponDiscount=((BigDecimal)coupon.get("amount")).min(total.subtract(memberDiscount));
        }
        long id=db.id("insert into pet_order(order_no,request_key,owner_id,service_start,service_end,address_snapshot,pet_snapshot,notes,total_amount,member_discount,coupon_discount,payable_amount,coupon_id,signed_agreement) values (?,?,?,?,?,?::jsonb,?::jsonb,?,?,?,?,?,?,?::jsonb) returning id",
            "WP"+UUID.randomUUID().toString().replace("-",""),r.requestKey(),actor.id(),r.serviceStart(),r.serviceEnd(),db.json(address),db.json(pets),r.notes(),total,memberDiscount,couponDiscount,total.subtract(memberDiscount).subtract(couponDiscount),r.couponId(),
            db.json(Map.of("version",r.agreementVersion(),"scope",r.authorizationScope(),"acceptedAt",OffsetDateTime.now().toString(),"signerId",actor.id())));
        for(var item:items) db.update("insert into order_item(order_id,service_id,code,name,unit_price,quantity,required_category) values (?,?,?,?,?,?,?)",id,item.get("id"),item.get("code"),item.get("name"),item.get("price"),item.get("quantity"),item.get("required_category"));
        if(r.couponId()!=null) db.update("update user_coupon set status='LOCKED' where id=?",r.couponId());
        support.event(id,actor,"CREATED",Map.of()); return support.order(id,actor,false);
    }
    @Transactional
    /** 取消待付款订单并释放锁定优惠券。 */
    public void cancel(long id,Actor actor,String reason) {
        if(actor.staff()) actor.require("order:update");
        else actor.role("OWNER");
        var o=support.order(id,actor,true); state(o,"PENDING_PAYMENT");
        db.update("update pet_order set status='CANCELLED',coupon_id=null where id=?",id);
        if(o.get("coupon_id")!=null) db.update("update user_coupon set status=case when expires_at>now() then 'AVAILABLE' else 'EXPIRED' end where id=?",o.get("coupon_id"));
        support.event(id,actor,"CANCELLED",Map.of("reason",reason));
    }
    @Transactional
    /** 幂等处理演示支付，不接入真实微信交易。 */
    public void payDemo(long id,Actor actor) {
        actor.role("OWNER"); var o=support.order(id,actor,true);
        if(!db.queryForList("select id from payment_record where order_id=?",id).isEmpty()) return;
        state(o,"PENDING_PAYMENT");
        db.update("insert into payment_record(order_id,trade_no,provider,amount) values (?,?,'DEMO',?)",id,"DEMO-ORDER-"+id,o.get("payable_amount"));
        db.update("update pet_order set status='DISPATCHING',paid_at=now() where id=?",id);
        if(o.get("coupon_id")!=null) db.update("update user_coupon set status='USED' where id=?",o.get("coupon_id"));
        db.update("update membership set saved_amount=saved_amount+? where owner_id=?",o.get("member_discount"),actor.id());
        support.event(id,actor,"PAID",Map.of("provider","DEMO"));
    }
    @Transactional
    /** 处理接单或拒单，仅允许当前被分配的喂养师操作。 */
    public void respond(long id,Actor actor,boolean accept) {
        actor.role("SITTER"); var o=support.order(id,actor,true); state(o,"PENDING_ACCEPT");
        if(!((java.sql.Timestamp)o.get("accept_deadline")).toInstant().isAfter(Instant.now())) throw BusinessException.conflict("接单已超时");
        db.update("update dispatch_record set status=?,responded_at=now() where order_id=? and status='PENDING'",accept?"ACCEPTED":"REJECTED",id);
        db.update("update pet_order set status=?,sitter_id=?,accept_deadline=null where id=?",accept?"ACCEPTED":"DISPATCHING",accept?o.get("sitter_id"):null,id);
        support.event(id,actor,accept?"ACCEPTED":"REJECTED",Map.of());
    }
    @Transactional
    /** 喂养师进门打卡；创建标准清单并通知宠主。 */
    public void checkin(long id,Actor actor) {
        actor.role("SITTER"); var o=support.order(id,actor,true); state(o,"ACCEPTED");
        Instant start=((java.sql.Timestamp)o.get("service_start")).toInstant();
        if(Instant.now().isBefore(start.minus(Duration.ofMinutes(30)))) throw BusinessException.bad("仅允许提前30分钟内进门打卡");
        db.update("update pet_order set status='SERVICING',checkin_at=now() where id=?",id);
        for(String code:List.of("FEED","WATER","CLEAN","PLAY","DOOR")) db.update("insert into service_checklist(order_id,code) values (?,?) on conflict do nothing",id,code);
        support.event(id,actor,"CHECKIN",Map.of()); support.notify(number(o,"owner_id"),id,"CHECKIN","喂养师已进门","进门时间："+OffsetDateTime.now());
    }
    @Transactional
    /** 保存标准服务清单；陪玩至少10分钟且单位为MINUTE。 */
    public void checklist(long id,Actor actor,OrderRequests.Checklist r) {
        actor.role("SITTER"); var o=support.order(id,actor,true); state(o,"SERVICING");
        if("PLAY".equals(r.code()) && r.completed() && (r.quantity()==null || r.quantity().compareTo(BigDecimal.TEN)<0 || !"MINUTE".equals(r.unit())))
            throw BusinessException.bad("陪玩至少10分钟，unit=MINUTE");
        db.update("insert into service_checklist(order_id,code,completed,quantity,unit,notes) values (?,?,?,?,?,?) on conflict(order_id,code) do update set completed=excluded.completed,quantity=excluded.quantity,unit=excluded.unit,notes=excluded.notes,updated_at=now()",id,r.code(),r.completed(),r.quantity(),r.unit(),r.notes());
    }
    @Transactional
    /** 登记服务媒体元数据，不处理文件上传或生成播放地址。 */
    public long media(long id,Actor actor,OrderRequests.Media r) {
        actor.role("SITTER"); var o=support.order(id,actor,true); state(o,"SERVICING");
        if(r.objectKey().contains("..") || r.objectKey().startsWith("/") || r.objectKey().contains("://")) throw BusinessException.bad("objectKey 必须是受控存储中的相对对象键");
        if("LIVE".equals(r.kind()) && db.id("select count(*) from order_item where order_id=? and code='LIVE'",id)==0) throw BusinessException.bad("订单未购买直播服务");
        return db.id("insert into service_media(order_id,uploader_id,kind,object_key,expires_at) values (?,?,?,?,now()+interval '7 days') returning id",id,actor.id(),r.kind(),r.objectKey());
    }
    @Transactional
    /** 完成服务；校验清单和录像，原子创建待处理报告、结算及离门通知。 */
    public void complete(long id,Actor actor) {
        actor.role("SITTER"); var o=support.order(id,actor,true); state(o,"SERVICING");
        if(db.id("select count(*) from service_checklist where order_id=? and completed and code in ('FEED','WATER','CLEAN','PLAY','DOOR')",id)!=5) throw BusinessException.bad("请完成全部五项检查清单");
        if(db.id("select count(*) from service_media where order_id=? and kind='VIDEO' and not deleted and expires_at>now()",id)==0) throw BusinessException.bad("请先登记服务录像");
        db.update("update pet_order set status='COMPLETED',checkout_at=now() where id=?",id);
        var snapshot=Map.of("checklist",db.queryForList("select * from service_checklist where order_id=?",id),"media",db.queryForList("select id,kind,object_key from service_media where order_id=? and not deleted",id),"checkinAt",o.get("checkin_at"),"checkoutAt",OffsetDateTime.now().toString());
        db.update("insert into feeding_report(order_id,source_snapshot) values (?,?::jsonb)",id,db.json(snapshot));
        BigDecimal gross=(BigDecimal)o.get("payable_amount");
        BigDecimal fee=money(gross.multiply((BigDecimal)o.get("commission_rate")).movePointLeft(2));
        db.update("insert into settlement(order_id,sitter_id,gross_amount,commission_rate,platform_amount,sitter_amount) values (?,?,?,?,?,?)",id,o.get("sitter_id"),gross,o.get("commission_rate"),fee,gross.subtract(fee));
        db.update("update sitter_profile set completed_orders=completed_orders+1 where id=?",o.get("sitter_id"));
        support.event(id,actor,"COMPLETED",Map.of("reportStatus","PENDING_AI"));
        support.notify(number(o,"owner_id"),id,"CHECKOUT","喂养师已离门","离门时间："+OffsetDateTime.now()+"；AI报告待后续Python服务处理。");
    }
    @Transactional
    /** 订单完成后评价，单订单只允许一次并更新综合评分。 */
    public long review(long id,Actor actor,OrderRequests.Review r) {
        actor.role("OWNER"); var o=support.order(id,actor,true); state(o,"COMPLETED");
        if(db.id("select count(*) from review where order_id=?",id)>0) throw BusinessException.conflict("订单已评价");
        long review=db.id("insert into review(order_id,owner_id,sitter_id,score,tags,content) values (?,?,?,?,?::jsonb,?) returning id",id,actor.id(),o.get("sitter_id"),r.score(),db.json(r.tags()==null?List.of():r.tags()),r.content());
        db.update("update sitter_profile set rating=(select round(avg(score)::numeric,2) from review where sitter_id=?) where id=?",o.get("sitter_id"),o.get("sitter_id"));
        support.event(id,actor,"REVIEWED",Map.of("score",r.score())); return review;
    }
    @Transactional
    /** 退款并回退券和未结算记录；已结算订单禁止直接退款。 */
    public void refundDemo(long id,Actor actor,String reason) {
        actor.require("finance:refund"); var o=support.order(id,actor,true); state(o,"DISPATCHING","PENDING_ACCEPT","ACCEPTED","COMPLETED");
        if(db.id("select count(*) from payment_record where order_id=? and provider='DEMO'",id)==0) throw BusinessException.bad("这里只能退款演示支付；真实支付尚未接入");
        if(db.id("select count(*) from settlement where order_id=? and status='SETTLED'",id)>0) throw BusinessException.conflict("订单已结算，须走线下追偿与财务对账");
        db.update("insert into refund_record(order_id,amount,reason,operator_id,provider) values (?,?,?,?,'DEMO')",id,o.get("payable_amount"),reason,actor.id());
        db.update("update pet_order set status='REFUNDED',coupon_id=null,accept_deadline=null where id=?",id);
        db.update("update dispatch_record set status='CANCELLED',responded_at=now() where order_id=? and status='PENDING'",id);
        db.update("update settlement set status='REVERSED' where order_id=?",id);
        if("COMPLETED".equals(o.get("status"))) db.update("update sitter_profile set completed_orders=greatest(0,completed_orders-1) where id=?",o.get("sitter_id"));
        if(o.get("coupon_id")!=null) db.update("update user_coupon set status=case when expires_at>now() then 'AVAILABLE' else 'EXPIRED' end where id=?",o.get("coupon_id"));
        db.update("update membership set saved_amount=greatest(0,saved_amount-?) where owner_id=?",o.get("member_discount"),o.get("owner_id"));
        support.event(id,actor,"REFUNDED",Map.of("reason",reason)); support.log(actor,"退款","order",id,o,Map.of("reason",reason));
    }
}
