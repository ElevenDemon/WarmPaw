package com.nuanzhua.pet.service.impl;
import com.nuanzhua.pet.common.BusinessSupport;
import com.nuanzhua.pet.common.Db;
import com.nuanzhua.pet.service.CouponService;
import com.nuanzhua.pet.service.MemberService;
import com.nuanzhua.pet.vo.Actor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.Map;

/** 业务服务实现；写操作在服务事务内完成，数据库约束提供最后一致性保护。 */
@Service
public class MemberServiceImpl implements MemberService {
    private final Db db; private final BusinessSupport support; private final CouponService coupons;
    public MemberServiceImpl(Db db,BusinessSupport support,CouponService coupons) { this.db=db; this.support=support; this.coupons=coupons; }
    /** 按当前套餐创建待付款会员购买单，冻结价格、期限及赠券。 */
    @Transactional public long purchase(Actor actor,long planId) {
        actor.role("OWNER"); var plan=db.one("select * from membership_plan where id=? and active for share",planId);
        return db.id("insert into membership_purchase(owner_id,plan_id,plan_name,months,amount,gift_coupon_template_id,gift_coupon_quantity) values (?,?,?,?,?,?,?) returning id",actor.id(),planId,plan.get("name"),plan.get("months"),plan.get("price"),plan.get("gift_coupon_template_id"),plan.get("gift_coupon_quantity"));
    }
    /** 幂等处理演示支付，不接入真实微信交易。 */
    @Transactional public void payDemo(Actor actor,long purchaseId) {
        actor.role("OWNER"); db.one("select id from app_user where id=? for update",actor.id());
        var p=db.one("select * from membership_purchase where id=? and owner_id=? for update",purchaseId,actor.id());
        if("PAID".equals(p.get("status"))) return;
        db.update("insert into payment_record(purchase_id,trade_no,provider,amount) values (?,?,'DEMO',?)",purchaseId,"DEMO-MEMBER-"+purchaseId,p.get("amount"));
        db.update("update membership_purchase set status='PAID',paid_at=now() where id=?",purchaseId);
        var rows=db.queryForList("select * from membership where owner_id=? for update",actor.id());
        OffsetDateTime start=OffsetDateTime.now();
        if(!rows.isEmpty() && "ACTIVE".equals(rows.getFirst().get("status"))) {
            var expiry=((java.sql.Timestamp)rows.getFirst().get("expires_at")).toInstant().atOffset(ZoneOffset.UTC);
            if(expiry.isAfter(start)) start=expiry;
        }
        OffsetDateTime end=start.plusMonths(((Number)p.get("months")).intValue());
        db.update("insert into membership(owner_id,plan_id,starts_at,expires_at) values (?,?,now(),?) on conflict(owner_id) do update set plan_id=excluded.plan_id,expires_at=excluded.expires_at,status='ACTIVE',updated_at=now()",actor.id(),p.get("plan_id"),end);
        if(p.get("gift_coupon_template_id")!=null && Db.number(p,"gift_coupon_quantity")>0) {
            var template=db.one("select * from coupon_template where id=? for update",p.get("gift_coupon_template_id"));
            int count=((Number)p.get("gift_coupon_quantity")).intValue();
            for(int i=0;i<count;i++) coupons.credit(template,actor.id(),"push");
            db.update("update coupon_template set issued=issued+? where id=?",count,template.get("id"));
            db.update("insert into coupon_claim_log(template_id,owner_id,source,success,reason,chain) values (?,?,'push',true,?,'[]'::jsonb)",template.get("id"),actor.id(),"会员购买赠券，购买单号："+purchaseId);
        }
        support.notify(actor.id(),null,"MEMBERSHIP","会员已开通 / 续期","有效期至："+end);
        support.log(actor,"会员演示支付","membership_purchase",purchaseId,null,Map.of("expiresAt",end.toString()));
    }
}
