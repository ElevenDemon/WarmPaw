package com.nuanzhua.pet.service.impl;
import com.nuanzhua.pet.common.BusinessSupport;
import com.nuanzhua.pet.common.Db;
import com.nuanzhua.pet.exception.BusinessException;
import com.nuanzhua.pet.service.AuthService;
import com.nuanzhua.pet.service.CouponService;
import com.nuanzhua.pet.vo.Actor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

/** 业务服务实现；写操作在服务事务内完成，数据库约束提供最后一致性保护。 */
@Service
public class CouponServiceImpl implements CouponService {
    private final Db db; private final BusinessSupport support;
    public CouponServiceImpl(Db db,BusinessSupport support) { this.db=db; this.support=support; }
    @Transactional
    /** 按七步规则领取优惠券；失败原因同样持久化。 */
    public Map<String,Object> claim(long templateId,Actor actor,String device,String ip) {
        actor.role("OWNER");
        var t=db.one("select * from coupon_template where id=? for update",templateId);
        var user=db.one("select * from app_user where id=?",actor.id());
        var chain=new ArrayList<Map<String,Object>>();
        String deviceHash=device==null?null:AuthService.hash(device);
        String phoneHash=user.get("phone")==null?null:AuthService.hash(user.get("phone").toString());
        String ipHash=AuthService.hash(ip);
        String[] reasons=new String[6];
        if(!Boolean.TRUE.equals(t.get("active")) || !"claim".equals(t.get("issue_type"))) reasons[0]="券未启用或不支持主动领取";
        Instant now=Instant.now();
        if(now.isBefore(((java.sql.Timestamp)t.get("claim_starts_at")).toInstant()) || !now.isBefore(((java.sql.Timestamp)t.get("claim_ends_at")).toInstant())) reasons[1]="不在领取时间窗内";
        boolean member=db.id("select count(*) from membership where owner_id=? and status='ACTIVE' and expires_at>now()",actor.id())>0;
        boolean isNew=db.id("select count(*) from pet_order where owner_id=? and paid_at is not null",actor.id())==0;
        boolean eligible=switch((String)t.get("audience")) {
            case "NEW" -> isNew; case "NORMAL" -> !member; case "MEMBER" -> member;
            case "SMALL" -> db.id("select count(*) from pet where owner_id=? and category='SMALL' and not deleted",actor.id())>0;
            default -> true;
        };
        if(!eligible) reasons[2]="不符合该券可领人群";
        if(db.id("select count(*) from user_coupon where template_id=? and owner_id=? and source='claim'",templateId,actor.id())>=Db.number(t,"per_user_limit")) reasons[3]="已达到每人领取上限";
        if(Db.number(t,"stock")<=0) reasons[4]="库存已抢光";
        if(Boolean.TRUE.equals(t.get("limit_device")) && deviceHash==null) reasons[5]="此券要求提供设备标识";
        for(var risk:List.of(Map.entry("limit_device","device_hash"),Map.entry("limit_phone","phone_hash"),Map.entry("limit_ip","ip_hash"))) {
            String value=switch(risk.getKey()) { case "limit_device" -> deviceHash; case "limit_phone" -> phoneHash; default -> ipHash; };
            if(Boolean.TRUE.equals(t.get(risk.getKey())) && (value==null || db.id("select count(*) from coupon_claim_log where template_id=? and success and source='claim' and "+risk.getValue()+"=?",templateId,value)>0)) reasons[5]="命中风控："+risk.getKey();
        }
        for(int i=0;i<6;i++) {
            chain.add(Map.of("step",i+1,"passed",reasons[i]==null,"reason",reasons[i]==null?"通过":reasons[i]));
            if(reasons[i]!=null) {
                log(templateId,actor.id(),"claim",false,i+1,reasons[i],chain,deviceHash,phoneHash,ipHash);
                return Map.of("success",false,"failedStep",i+1,"reason",reasons[i],"chain",chain,"nextAction","查看我的券包或联系平台核对领取规则");
            }
        }
        long coupon=credit(t,actor.id(),"claim");
        db.update("update coupon_template set stock=stock-1,issued=issued+1 where id=?",templateId);
        chain.add(Map.of("step",7,"passed",true,"reason","已入账"));
        log(templateId,actor.id(),"claim",true,null,"领取成功",chain,deviceHash,phoneHash,ipHash);
        return Map.of("success",true,"couponId",coupon,"chain",chain);
    }
    private void log(long template,long owner,String source,boolean success,Integer step,String reason,Object chain,String device,String phone,String ip) {
        db.update("insert into coupon_claim_log(template_id,owner_id,source,success,failed_step,reason,chain,device_hash,phone_hash,ip_hash) values (?,?,?,?,?,?,?::jsonb,?,?,?)",template,owner,source,success,step,reason,db.json(chain),device,phone,ip);
    }
    /** 按券模板生成用户券快照，调用者负责事务及库存管理。 */
    public long credit(Map<String,Object> t,long owner,String source) {
        long id=db.id("insert into user_coupon(template_id,owner_id,source,amount,threshold,service_codes,expires_at) values (?,?,?,?,?,?::jsonb,now()+make_interval(days=>?)) returning id",t.get("id"),owner,source,t.get("amount"),t.get("threshold"),t.get("service_codes").toString(),((Number)t.get("valid_days")).intValue());
        support.notify(owner,null,"COUPON","优惠券已到账",t.get("name").toString()); return id;
    }
    @Transactional
    /** 批量直发优惠券，区别于主动领取库存。 */
    public List<Long> push(long templateId,Actor actor,List<Long> owners,int quantity) {
        actor.require("coupon:update");
        var t=db.one("select * from coupon_template where id=? for update",templateId);
        if(!"push".equals(t.get("issue_type")) || !Boolean.TRUE.equals(t.get("active"))) throw BusinessException.bad("请选择启用中的后台直发券");
        var ids=new ArrayList<Long>();
        for(long owner:new HashSet<>(owners)) {
            db.one("select id from app_user where id=? and role='OWNER'",owner);
            for(int i=0;i<quantity;i++) ids.add(credit(t,owner,"push"));
            log(templateId,owner,"push",true,null,"后台直发，风控免校验",List.of(),null,null,null);
        }
        db.update("update coupon_template set issued=issued+? where id=?",ids.size(),templateId);
        support.log(actor,"优惠券直发","coupon",templateId,null,Map.of("owners",owners,"quantity",quantity)); return ids;
    }
}
