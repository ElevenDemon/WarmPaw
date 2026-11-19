package com.nuanzhua.pet.service.impl;
import com.nuanzhua.pet.common.BusinessSupport;
import com.nuanzhua.pet.common.Db;
import com.nuanzhua.pet.dto.ConfigRequests;
import com.nuanzhua.pet.service.ConfigService;
import com.nuanzhua.pet.service.GradeRuleService;
import com.nuanzhua.pet.vo.Actor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

/** 业务服务实现；写操作在服务事务内完成，数据库约束提供最后一致性保护。 */
@Service
public class GradeRuleServiceImpl implements GradeRuleService {
    private final Db db; private final ConfigService config; private final BusinessSupport support; private final ObjectMapper mapper;
    public GradeRuleServiceImpl(Db db,ConfigService config,BusinessSupport support,ObjectMapper mapper) { this.db=db; this.config=config; this.support=support; this.mapper=mapper; }
    /** 计算上海时区下一个半月结算期：每月1日或16日00:00。 */
    public static OffsetDateTime nextPeriod() {
        var now=ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        return (now.getDayOfMonth()<16?now.withDayOfMonth(16):now.plusMonths(1).withDayOfMonth(1)).toLocalDate().atStartOfDay(now.getZone()).toOffsetDateTime();
    }
    private LinkedHashMap<String,Object> fields(ConfigRequests.Grade r,OffsetDateTime at) {
        return ConfigService.values("code",r.code(),"name",r.name(),"min_orders",r.minOrders(),"min_rating",r.minRating(),"max_complaints",r.maxComplaints(),"commission_rate",r.commissionRate(),"dispatch_weight",r.dispatchWeight(),"high_risk_enabled",r.highRiskEnabled(),"active",r.active(),"effective_at",at);
    }
    /** 事务保存业务配置或档案，并记录变更审计。 */
    @Transactional public long save(Actor actor,Long id,ConfigRequests.Grade r) {
        actor.require("sitter:rule"); var effective=r.effectiveAt()==null?nextPeriod():r.effectiveAt();
        if(id==null) return config.save(actor,"sitter:rule","sitter_grade",null,fields(r,effective),Set.of());
        db.one("select id from sitter_grade where id=? for update",id);
        if(!effective.isAfter(OffsetDateTime.now())) return config.save(actor,"sitter:rule","sitter_grade",id,fields(r,effective),Set.of());
        // 同一等级仅保留一个待生效版本，新保存替代之前尚未生效的计划。
        db.update("delete from sitter_grade_revision where grade_id=? and applied_at is null",id);
        db.update("insert into sitter_grade_revision(grade_id,rule_data,effective_at,operator_id) values (?,?::jsonb,?,?)",id,db.json(r),effective,actor.id());
        support.log(actor,"保存下期等级规则","sitter_grade",id,null,Map.of("rule",r,"effectiveAt",effective)); return id;
    }
    /** 提交入驻申请或执行到期等级规则。 */
    @Scheduled(fixedDelay=15000) @Transactional public void apply() {
        var revisions=db.queryForList("select id,grade_id from sitter_grade_revision where applied_at is null and effective_at<=now() order by grade_id,id");
        for(var candidate:revisions) {
            // 与保存操作保持同一锁顺序：等级 -> 待生效版本。
            db.one("select id from sitter_grade where id=? for update",candidate.get("grade_id"));
            var pending=db.queryForList("select * from sitter_grade_revision where id=? and applied_at is null and effective_at<=now() for update",candidate.get("id"));
            if(pending.isEmpty()) continue;
            var rev=pending.getFirst();
            try {
                var r=mapper.readValue(rev.get("rule_data").toString(),ConfigRequests.Grade.class);
                var values=fields(r,((java.sql.Timestamp)rev.get("effective_at")).toInstant().atOffset(ZoneOffset.UTC));
                var args=new ArrayList<>(values.values()); args.add(rev.get("grade_id"));
                db.update("update sitter_grade set "+String.join(",",values.keySet().stream().map(k->k+"=?").toList())+",updated_at=now() where id=?",args.toArray());
                db.update("update sitter_grade_revision set applied_at=now() where id=?",rev.get("id"));
                support.log(null,"等级规则按期生效","sitter_grade",Db.number(rev,"grade_id"),null,r);
            } catch(com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException(e); }
        }
    }
}
