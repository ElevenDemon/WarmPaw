package com.nuanzhua.pet.service.impl;
import com.nuanzhua.pet.service.CouponApplicationService;
import org.springframework.stereotype.Service;
import com.nuanzhua.pet.dto.CouponRequests.*;
import com.nuanzhua.pet.common.Db;
import com.nuanzhua.pet.service.CouponService;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** 业务服务实现；写操作在服务事务内完成，数据库约束提供最后一致性保护。 */
@Service
@org.springframework.transaction.annotation.Transactional
public class CouponApplicationServiceImpl implements CouponApplicationService {
    private final Db db; private final CouponService coupons;
    public CouponApplicationServiceImpl(Db db,CouponService coupons) { this.db=db; this.coupons=coupons; }

    /** 查询可领取优惠券中心。 */
    public Api<?> center(Actor actor) {
        actor.role("OWNER"); return Api.ok(db.queryForList("select * from coupon_template where active and issue_type='claim' and claim_ends_at>now() order by id"));
    }
    /** 查询当前登录人自己的业务数据。 */
    public Api<?> mine(Actor actor,int page,int size) {
        actor.role("OWNER"); return Api.ok(db.page("user_coupon","owner_id=?",List.of(actor.id()),page,size));
    }
    /** 按七步规则领取优惠券；失败原因同样持久化。 */
    public Api<?> claim(Actor actor,long id,Claim r,HttpServletRequest request) {
        return Api.ok(coupons.claim(id,actor,r.deviceId(),request.getRemoteAddr()));
    }
    /** 根据兑换码领取优惠券，仍执行完整领取规则。 */
    public Api<?> redeem(Actor actor,Redeem r,HttpServletRequest request) {
        long id=db.id("select id from coupon_template where redeem_code=?",r.code());
        return Api.ok(coupons.claim(id,actor,r.deviceId(),request.getRemoteAddr()));
    }
    /** 批量直发优惠券，区别于主动领取库存。 */
    public Api<?> push(Actor actor,long id,Push r) { return Api.ok(coupons.push(id,actor,r.ownerIds(),r.quantity())); }
}
