package com.nuanzhua.pet.service.impl;
import com.nuanzhua.pet.service.ConfigApplicationService;
import org.springframework.stereotype.Service;
import com.nuanzhua.pet.common.BusinessSupport;
import com.nuanzhua.pet.common.Db;
import com.nuanzhua.pet.dto.ConfigRequests;
import com.nuanzhua.pet.exception.BusinessException;
import com.nuanzhua.pet.service.AuthService;
import com.nuanzhua.pet.service.ConfigService;
import com.nuanzhua.pet.service.GradeRuleService;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;

import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.util.*;
import static com.nuanzhua.pet.service.ConfigService.values;

/** 业务服务实现；写操作在服务事务内完成，数据库约束提供最后一致性保护。 */
@Service
@org.springframework.transaction.annotation.Transactional
public class ConfigApplicationServiceImpl implements ConfigApplicationService {
    private final Db db; private final ConfigService config; private final AuthService auth; private final BusinessSupport support; private final GradeRuleService gradeRules;
    public ConfigApplicationServiceImpl(Db db,ConfigService config,AuthService auth,BusinessSupport support,GradeRuleService gradeRules) { this.db=db; this.config=config; this.auth=auth; this.support=support; this.gradeRules=gradeRules; }
    /** 更新服务价格和启用状态，不修改历史订单快照。 */
    public Api<?> price(Actor actor,long id,ConfigRequests.Price r) {
        return Api.ok(config.save(actor,"price:update","service_item",id,values("name",r.name(),"description",r.description(),"price",r.price(),"active",r.active()),Set.of()));
    }
    private long savePlan(Actor actor,Long id,ConfigRequests.Plan r) {
        return config.save(actor,"member:update","membership_plan",id,values("name",r.name(),"display_name",r.displayName(),"months",r.months(),"price",r.price(),"active",r.active()),Set.of());
    }
    /** 新增自定义会员套餐。 */
    public Api<?> addPlan(Actor actor,ConfigRequests.Plan r) { return Api.ok(savePlan(actor,null,r)); }
    /** 保存会员套餐；历史购买价格不受新配置影响。 */
    public Api<?> plan(Actor actor,long id,ConfigRequests.Plan r) { return Api.ok(savePlan(actor,id,r)); }
    /** 保存会员权益开关及折扣乘数。 */
    public Api<?> benefit(Actor actor,long id,ConfigRequests.Benefit r) {
        return Api.ok(config.save(actor,"member:update","membership_benefit",id,values("enabled",r.enabled(),"discount_rate",r.discountRate()),Set.of()));
    }
    private long saveGrade(Actor actor,Long id,ConfigRequests.Grade r) {
        return gradeRules.save(actor,id,r);
    }
    /** 新增等级规则，默认下个结算期生效。 */
    public Api<?> addGrade(Actor actor,ConfigRequests.Grade r) { return Api.ok(saveGrade(actor,null,r)); }
    /** 保存等级规则；未来生效规则独立留存，避免提前修改现行抽成。 */
    public Api<?> grade(Actor actor,long id,ConfigRequests.Grade r) { return Api.ok(saveGrade(actor,id,r)); }
    /** 按当前生效规则模拟符合条件的等级，不修改档案。 */
    public Api<?> simulate(Actor actor,int orders,BigDecimal rating,int complaints) {
        actor.require("sitter:read"); if(orders<0 || complaints<0 || rating.signum()<0 || rating.compareTo(BigDecimal.valueOf(5))>0) throw BusinessException.bad("模拟参数越界");
        return Api.ok(db.queryForList("select * from sitter_grade where active and effective_at<=now() and min_orders<=? and min_rating<=? and max_complaints>=? order by min_orders desc,min_rating desc limit 1",orders,rating,complaints));
    }
    /** 按当前生效等级试算平台与喂养师结算金额。 */
    public Api<?> estimate(Actor actor,BigDecimal amount) {
        actor.require("sitter:read"); if(amount.signum()<0 || amount.compareTo(new BigDecimal("999999"))>0) throw BusinessException.bad("金额超出范围");
        return Api.ok(db.queryForList("select id,code,commission_rate,dispatch_weight,round(?::numeric*commission_rate/100,2) as platform_amount,?::numeric-round(?::numeric*commission_rate/100,2) as sitter_amount from sitter_grade where active and effective_at<=now()",amount,amount,amount));
    }
    private long saveCoupon(Actor actor,Long id,ConfigRequests.Coupon r) {
        if(!r.claimStartsAt().isBefore(r.claimEndsAt())) throw BusinessException.bad("领取开始时间必须早于结束时间");
        return config.save(actor,"coupon:update","coupon_template",id,values("name",r.name(),"issue_type",r.issueType(),"amount",r.amount(),"threshold",r.threshold(),"service_codes",db.json(r.serviceCodes()),"valid_days",r.validDays(),"active",r.active(),"audience",r.audience(),"per_user_limit",r.perUserLimit(),"stock",r.stock(),"claim_starts_at",r.claimStartsAt(),"claim_ends_at",r.claimEndsAt(),"redeem_code",r.redeemCode()==null||r.redeemCode().isBlank()?null:r.redeemCode(),"limit_device",r.limitDevice(),"limit_phone",r.limitPhone(),"limit_ip",r.limitIp()),Set.of("service_codes"));
    }
    /** 新增优惠券模板及领取限制。 */
    public Api<?> addCoupon(Actor actor,ConfigRequests.Coupon r) { return Api.ok(saveCoupon(actor,null,r)); }
    /** 保存优惠券模板，参数来自受控DTO。 */
    public Api<?> coupon(Actor actor,long id,ConfigRequests.Coupon r) { return Api.ok(saveCoupon(actor,id,r)); }
    /** 保存消息模板，安全类进出提醒不可关闭。 */
    public Api<?> notice(Actor actor,long id,ConfigRequests.Notice r) {
        var t=db.one("select * from notification_template where id=?",id);
        if(!r.enabled() && Set.of("CHECKIN","CHECKOUT","EMERGENCY").contains(t.get("code"))) throw BusinessException.bad("进门、离门与异常提醒属于必达站内通知，不允许关闭");
        return Api.ok(config.save(actor,"msg:update","notification_template",id,values("enabled",r.enabled(),"channels",db.json(r.channels())),Set.of("channels")));
    }
    /** 启用或封禁宠主账号。 */
    public Api<?> ownerStatus(Actor actor,long id,ConfigRequests.Status r) {
        actor.require("user:risk"); db.one("select id from app_user where id=? and role='OWNER'",id);
        return Api.ok(config.save(actor,"user:risk","app_user",id,values("status",r.status()),Set.of()));
    }
    /** 更新会员账户启停状态。 */
    public Api<?> memberStatus(Actor actor,long id,ConfigRequests.MemberStatus r) {
        return Api.ok(config.save(actor,"member:update","membership",id,values("status",r.status()),Set.of()));
    }
    /** 批量结算，锁定订单后锁定结算，防止与退款并发冲突。 */
    public Api<?> settle(Actor actor,ConfigRequests.Batch r) { config.settle(actor,r.ids()); return Api.ok(true); }
    /** 更新受控角色权限集合。 */
    public Api<?> permissions(Actor actor,String role,ConfigRequests.Permissions r) { config.permissions(actor,role,r.permissions()); return Api.ok(true); }
    /** 新增客服或财务账号，初始密码以BCrypt摘要保存。 */
    @Transactional public Api<?> staff(Actor actor,ConfigRequests.Staff r) {
        actor.require("system:update"); long id=db.id("insert into app_user(username,password_hash,name,role) values (?,?,?,?) returning id",r.username(),auth.encodePassword(r.password()),r.name(),r.role());
        support.log(actor,"新增后台账号","staff",id,null,Map.of("username",r.username(),"role",r.role())); return Api.ok(id);
    }
}
