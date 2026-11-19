package com.nuanzhua.pet.service.impl;
import com.nuanzhua.pet.common.BusinessSupport;
import com.nuanzhua.pet.common.Db;
import com.nuanzhua.pet.dto.SitterRequests;
import com.nuanzhua.pet.exception.BusinessException;
import com.nuanzhua.pet.service.SitterService;
import com.nuanzhua.pet.vo.Actor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** 业务服务实现；写操作在服务事务内完成，数据库约束提供最后一致性保护。 */
@Service
public class SitterServiceImpl implements SitterService {
    private final Db db; private final BusinessSupport support;
    public SitterServiceImpl(Db db,BusinessSupport support) { this.db=db; this.support=support; }
    /** 通过或驳回待审核申请；驳回意见保留供喂养师修改后重新申请。 */
    @Transactional public void admission(long id,Actor actor,SitterRequests.AdmissionAudit r) {
        actor.require("audit:update");
        boolean approved=r.decision()==SitterRequests.AdmissionDecision.APPROVE;
        if(!approved && (r.note()==null || r.note().isBlank())) throw BusinessException.bad("驳回申请时请填写需要补充或修改的内容");
        var before=db.one("select * from sitter_profile where id=? for update",id);
        if(!"PENDING".equals(before.get("status"))) throw BusinessException.conflict("仅待审核的入驻申请可以处理；已驳回申请需由喂养师修改后重新提交");
        // 所有申请的基础类目必须具备有效资质，高风险扩展类目仍由独立资质审核控制。
        if(r.decision()==SitterRequests.AdmissionDecision.APPROVE && db.id("select count(*) from sitter_profile s cross join lateral jsonb_array_elements_text(s.categories) c(category) where s.id=? and not exists (select 1 from sitter_qualification q where q.sitter_id=s.id and q.category=c.category and q.status='APPROVED' and q.real_verified and q.training_passed and q.expires_at>now())",id)>0)
            throw BusinessException.bad("请先在分类资质中审核通过所有申请类目，并确认资质未过期");
        String note=r.note()==null || r.note().isBlank()?null:r.note().trim();
        db.update("update sitter_profile set status=?,admission_review_note=?,updated_at=now() where id=?",approved?"ACTIVE":"REJECTED",note,id);
        support.log(actor,approved?"通过喂养师入驻申请":"驳回喂养师入驻申请","sitter",id,before,Map.of("decision",r.decision().name(),"note",note==null?"":note));
    }
    /** 查询业务详情，并校验访问权限。 */
    public Map<String,Object> detail(long id) {
        var s=db.one("select s.*,u.name,u.phone,g.code as grade_code,g.commission_rate,g.dispatch_weight from sitter_profile s join app_user u on u.id=s.user_id join sitter_grade g on g.id=s.grade_id where s.id=?",id);
        s.put("schedule",db.queryForList("select * from sitter_schedule where sitter_id=? order by weekday",id));
        s.put("qualifications",db.queryForList("select * from sitter_qualification where sitter_id=?",id)); return s;
    }
    /** 保存喂养师可自主编辑的信息，后台专有字段不允许自改。 */
    @Transactional public void personal(long id,SitterRequests.Personal r) {
        var s=db.one("select * from sitter_profile where id=? for update",id);
        db.update("update app_user set name=?,phone=?,updated_at=now() where id=?",r.name(),r.phone(),s.get("user_id"));
        db.update("update sitter_profile set emergency_contact=?,avatar_url=?,notes=?,service_preferences=coalesce(?,''),district=?,address=?,longitude=?,latitude=?,radius_km=?,transport=?,updated_at=now() where id=?",r.emergencyContact(),r.avatarUrl(),r.notes(),r.servicePreferences(),r.district(),r.address(),r.longitude(),r.latitude(),r.radiusKm(),r.transport(),id);
    }
    /** 事务保存业务配置或档案，并记录变更审计。 */
    @Transactional public void save(long id,Actor actor,SitterRequests.Profile r) {
        actor.require("sitter:update");
        if(r.schedule().stream().map(SitterRequests.Day::weekday).distinct().count()!=7) throw BusinessException.bad("每日档期必须覆盖周一至周日且不能重复");
        for(var day:r.schedule()) {
            if(!day.startTime().isBefore(day.endTime())) throw BusinessException.bad("档期开始时间须早于结束时间");
            if((day.longitude()==null)!=(day.latitude()==null)) throw BusinessException.bad("当日经纬度必须同时填写或同时留空");
            if(day.address()!=null && !day.address().isBlank() && day.longitude()==null) throw BusinessException.bad("当日地址需同时填写经纬度");
        }
        var before=detail(id); db.one("select id from sitter_grade where id=? and active and effective_at<=now()",r.gradeId());
        personal(id,r.personal());
        db.update("update sitter_profile set grade_id=?,status=?,settlement_method=?,categories=?::jsonb where id=?",r.gradeId(),r.status(),r.settlementMethod(),db.json(r.categories()),id);
        for(var day:r.schedule()) db.update("insert into sitter_schedule(sitter_id,weekday,rest,start_time,end_time,capacity,district,address,longitude,latitude) values (?,?,?,?,?,?,?,?,?,?) on conflict(sitter_id,weekday) do update set rest=excluded.rest,start_time=excluded.start_time,end_time=excluded.end_time,capacity=excluded.capacity,district=excluded.district,address=excluded.address,longitude=excluded.longitude,latitude=excluded.latitude",id,day.weekday(),day.rest(),java.sql.Time.valueOf(day.startTime()),java.sql.Time.valueOf(day.endTime()),day.capacity(),day.district(),day.address(),day.longitude(),day.latitude());
        support.log(actor,"保存喂养师完整档案","sitter",id,before,detail(id));
    }
    /** 提交入驻申请或执行到期等级规则。 */
    @Transactional public long apply(Actor actor,SitterRequests.Application r) {
        db.one("select id from app_user where id=? for update",actor.id());
        var p=r.personal();
        long id;
        Map<String,Object> before=null;
        if("OWNER".equals(actor.role())) {
            if(db.id("select count(*) from sitter_profile where user_id=?",actor.id())>0) throw BusinessException.conflict("此账号已有喂养师档案，请前往“我的档案”查看，无需重复申请");
            if(db.id("select count(*) from pet_order where owner_id=?",actor.id())>0) throw BusinessException.bad("此账号已有宠主订单，暂不能转为喂养师；请使用单独的喂养师账号申请");
            long grade=db.id("select id from sitter_grade where code='L1'");
            id=db.id("insert into sitter_profile(user_id,code,grade_id,district,address,longitude,latitude,radius_km,transport,categories) values (?,?,?,?,?,?,?,?,?,?::jsonb) returning id",actor.id(),"S"+UUID.randomUUID().toString().replace("-","").substring(0,16),grade,p.district(),p.address(),p.longitude(),p.latitude(),p.radiusKm(),p.transport(),db.json(r.categories()));
            personal(id,p);
            db.update("update app_user set role='SITTER' where id=?",actor.id());
        } else if("SITTER".equals(actor.role())) {
            var profiles=db.queryForList("select * from sitter_profile where user_id=? for update",actor.id());
            if(profiles.isEmpty()) throw BusinessException.conflict("未找到喂养师档案，请联系客服协助处理");
            before=profiles.getFirst();
            if(!"REJECTED".equals(before.get("status"))) throw BusinessException.conflict("只有已驳回的申请可以重新提交；当前申请状态为"+before.get("status"));
            id=((Number)before.get("id")).longValue();
            db.update("update sitter_profile set status='PENDING',admission_review_note=null,categories=?::jsonb,updated_at=now() where id=?",db.json(r.categories()),id);
            personal(id,p);
        } else {
            throw BusinessException.forbidden("请使用宠主账号提交喂养师入驻申请");
        }
        support.log(actor,before==null?"申请喂养师入驻":"重新提交喂养师入驻申请","sitter",id,before,r);
        return id;
    }
    /** 审核分类资质及有效期。 */
    @Transactional public void audit(long qualificationId,Actor actor,SitterRequests.Audit r) {
        actor.require("audit:update");
        // 与入驻审核统一按档案、资质顺序加锁，避免审核时读取到并发变更的资质。
        long sitterId=db.id("select sitter_id from sitter_qualification where id=?",qualificationId);
        db.one("select id from sitter_profile where id=? for update",sitterId);
        var before=db.one("select * from sitter_qualification where id=? for update",qualificationId);
        boolean risk=Set.of("WALK","MEDICINE","DEWORM").contains(before.get("category"));
        if(r.approved() && (!r.realVerified() || !r.trainingPassed() || (risk && !r.practicePassed()))) throw BusinessException.bad("审核通过须实名、培训均通过；高风险类目还须实操通过");
        db.update("update sitter_qualification set status=?,real_verified=?,training_passed=?,practice_passed=?,expires_at=?,reviewer_id=?,review_note=?,updated_at=now() where id=?",r.approved()?"APPROVED":"REJECTED",r.realVerified(),r.trainingPassed(),r.practicePassed(),r.expiresAt(),actor.id(),r.note(),qualificationId);
        support.log(actor,"审核喂养师资质","qualification",qualificationId,before,r);
    }
}
