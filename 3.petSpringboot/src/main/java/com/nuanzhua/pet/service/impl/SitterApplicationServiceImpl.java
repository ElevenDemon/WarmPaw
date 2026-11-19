package com.nuanzhua.pet.service.impl;
import com.nuanzhua.pet.service.SitterApplicationService;
import org.springframework.stereotype.Service;
import com.nuanzhua.pet.common.BusinessSupport;
import com.nuanzhua.pet.common.Db;
import com.nuanzhua.pet.dto.SitterRequests;
import com.nuanzhua.pet.service.SitterService;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** 业务服务实现；写操作在服务事务内完成，数据库约束提供最后一致性保护。 */
@Service
@org.springframework.transaction.annotation.Transactional
public class SitterApplicationServiceImpl implements SitterApplicationService {
    private final Db db; private final SitterService sitters; private final BusinessSupport support;
    public SitterApplicationServiceImpl(Db db,SitterService sitters,BusinessSupport support) { this.db=db; this.sitters=sitters; this.support=support; }
    private long self(Actor actor) { actor.role("SITTER"); return db.id("select id from sitter_profile where user_id=?",actor.id()); }
    /** 提交入驻申请或执行到期等级规则。 */
    public Api<?> apply(Actor actor,SitterRequests.Application r) { return Api.ok(sitters.apply(actor,r)); }
    /** 查询当前登录人自己的业务数据。 */
    public Api<?> mine(Actor actor) { return Api.ok(sitters.detail(self(actor))); }
    /** 保存喂养师可自主编辑的信息，后台专有字段不允许自改。 */
    public Api<?> personal(Actor actor,SitterRequests.Personal r) {
        long id=self(actor); var before=sitters.detail(id); sitters.personal(id,r); support.log(actor,"喂养师修改个人资料","sitter",id,before,r); return Api.ok(true);
    }
    /** 提交分类资质材料并进入待审核状态。 */
    public Api<?> qualification(Actor actor,SitterRequests.Qualification r) {
        long id=self(actor);
        db.one("select id from sitter_profile where id=? for update",id);
        return Api.ok(db.id("insert into sitter_qualification(sitter_id,category,materials,expires_at) values (?,?,?::jsonb,now()) on conflict(sitter_id,category) do update set materials=excluded.materials,status='PENDING',real_verified=false,training_passed=false,practice_passed=false,updated_at=now() returning id",id,r.category(),db.json(r.materials())));
    }
    /** 汇总喂养师本月待结算和已结算收入。 */
    public Api<?> income(Actor actor) {
        return Api.ok(db.one("select count(*) as orders,coalesce(sum(sitter_amount),0) as income,coalesce(sum(sitter_amount) filter(where status='SETTLED'),0) as settled from settlement where sitter_id=? and status<>'REVERSED' and created_at>=date_trunc('month',now())",self(actor)));
    }
    /** 查询业务详情，并校验访问权限。 */
    public Api<?> detail(Actor actor,long id) {
        if(!actor.permissions().contains("audit:read")) actor.require("sitter:read");
        return Api.ok(sitters.detail(id));
    }
    /** 审核后返回共享档案，信息页无需维护另一份状态。 */
    public Api<?> admission(Actor actor,long id,SitterRequests.AdmissionAudit r) {
        sitters.admission(id,actor,r); return Api.ok(sitters.detail(id));
    }
    /** 事务保存业务配置或档案，并记录变更审计。 */
    public Api<?> save(Actor actor,long id,SitterRequests.Profile r) { sitters.save(id,actor,r); return Api.ok(sitters.detail(id)); }
    /** 审核分类资质及有效期。 */
    public Api<?> audit(Actor actor,long id,SitterRequests.Audit r) { sitters.audit(id,actor,r); return Api.ok(Map.of("success",true)); }
}
