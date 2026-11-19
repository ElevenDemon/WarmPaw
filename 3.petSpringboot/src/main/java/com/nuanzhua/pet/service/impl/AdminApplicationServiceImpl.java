package com.nuanzhua.pet.service.impl;

import com.nuanzhua.pet.service.AdminApplicationService;
import org.springframework.stereotype.Service;
import com.nuanzhua.pet.common.Db;
import com.nuanzhua.pet.exception.BusinessException;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;

import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 业务服务实现；写操作在服务事务内完成，数据库约束提供最后一致性保护。
 */
@Service
@org.springframework.transaction.annotation.Transactional
public class AdminApplicationServiceImpl implements AdminApplicationService {
    private final Db db;

    public AdminApplicationServiceImpl(Db db) {
        this.db = db;
    }

    private record Module(String permission, String source) {
    }

    private static final Map<String, Module> MODULES = Map.ofEntries(
            Map.entry("order", new Module("order:read", "pet_order")),
            // 入驻审核与喂养师信息均从同一份档案、账号和等级数据实时查询。
            Map.entry("audit", new Module("audit:read", "(select s.*,u.name,u.phone,g.name as grade_name,g.commission_rate from sitter_profile s join app_user u on s.user_id=u.id join sitter_grade g on s.grade_id=g.id) t")),
            Map.entry("qualifications", new Module("audit:read", "(select q.*,u.name,u.phone,s.code as sitter_code,s.status as sitter_status from sitter_qualification q join sitter_profile s on s.id=q.sitter_id join app_user u on u.id=s.user_id) t")),
            Map.entry("finance", new Module("finance:read", "settlement")),
            Map.entry("refund", new Module("finance:read", "refund_record")),
            Map.entry("member", new Module("member:read", "membership")),
            Map.entry("plans", new Module("member:read", "membership_plan")),
            Map.entry("benefits", new Module("member:read", "membership_benefit")),
            Map.entry("sitter", new Module("sitter:read", "(select s.*,u.name,u.phone,g.name as grade_name,g.commission_rate from sitter_profile s join app_user u on s.user_id=u.id join sitter_grade g on s.grade_id=g.id) t")),
            Map.entry("grades", new Module("sitter:read", "sitter_grade")),
            Map.entry("grade-revisions", new Module("sitter:read", "sitter_grade_revision")),
            Map.entry("user", new Module("user:read", "(select id,username,name,phone,status,created_at from app_user where role='OWNER') t")),
            Map.entry("coupon", new Module("coupon:read", "coupon_template")),
            Map.entry("coupon-logs", new Module("coupon:read", "coupon_claim_log")),
            Map.entry("price", new Module("price:read", "service_item")),
            Map.entry("review", new Module("review:read", "review")),
            Map.entry("complaints", new Module("review:read", "complaint")),
            Map.entry("msg", new Module("msg:read", "notification_template")),
            Map.entry("system", new Module("system:read", "operation_log")),
            Map.entry("roles", new Module("system:read", "role_permission")),
            Map.entry("staff", new Module("system:read", "(select id,username,name,role,status,created_at from app_user where role in ('ADMIN','SUPPORT','FINANCE')) t")),
            Map.entry("service", new Module("service:read", "support_conversation")),
            Map.entry("emergency", new Module("emergency:read", "emergency_ticket")),
            Map.entry("hospitals", new Module("emergency:read", "hospital"))
    );

    /**
     * 分页查询业务列表，表名与过滤列由服务端白名单提供。
     */
    public Api<?> list(Actor actor, String module,
                       int page, int size,
                       String keyword, String status) {
        var m = MODULES.get(module);
        if (m == null) throw BusinessException.bad("未知后台模块");
        actor.require(m.permission());
        if (keyword.length() > 100 || status.length() > 30) throw BusinessException.bad("查询条件过长");
        String where = "true";
        var args = new ArrayList<Object>();
        if (!keyword.isBlank()) {
            where += " and row_to_json(t)::text ilike ?";
            args.add("%" + keyword + "%");
        }
        // JSON字段提取避免对无status列的配置表产生SQL错误。
        if (!status.isBlank()) {
            where += " and to_jsonb(t)->>'status'=?";
            args.add(status);
        }
        return Api.ok(db.page("(select * from " + m.source() + ") t", where, args, page, size));
    }

    /**
     * 查询运营工作台汇总指标。
     */
    public Api<?> dashboard(Actor actor) {
        actor.require("dash:read");
        return Api.ok(Map.of("orders", db.id("select count(*) from pet_order"), "gmv", db.one("select coalesce(sum(payable_amount),0) as value from pet_order where paid_at is not null and status<>'REFUNDED'").get("value"),
                "manualDispatchPending", db.id("select count(*) from pet_order where status='DISPATCHING'"), "openComplaints", db.id("select count(*) from complaint where status<>'RESOLVED'"), "aiEnabled", false));
    }

    /**
     * 查询当前schema的数据字典。
     */
    public Api<?> owner(Actor actor, long id) {
        actor.require("user:read");
        var owner = db.one("select id,username,name,phone,status,created_at from app_user where id=? and role='OWNER'", id);
        owner.put("pets", db.queryForList("select * from pet where owner_id=? and not deleted order by id", id));
        owner.put("addresses", db.queryForList("select * from owner_address where owner_id=? order by id", id));
        owner.put("membership", db.queryForList("select m.*,p.display_name as plan_name from membership m join membership_plan p on p.id=m.plan_id where m.owner_id=?", id));
        owner.put("order_summary", db.one("select count(*) as orders,coalesce(sum(payable_amount) filter(where paid_at is not null and status<>'REFUNDED'),0) as paid_amount from pet_order where owner_id=?", id));
        return Api.ok(owner);
    }

    /**
     * 查询当前schema的数据字典。
     */
    public Api<?> dictionary(Actor actor) {
        actor.require("dict:read");
        return Api.ok(db.queryForList("select c.table_name,c.column_name,c.data_type,c.is_nullable,c.column_default,c.character_maximum_length,c.numeric_precision,c.numeric_scale from information_schema.columns c where c.table_schema=current_schema() and c.table_name<>'flyway_schema_history' order by c.table_name,c.ordinal_position"));
    }
}
