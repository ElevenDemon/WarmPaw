package com.nuanzhua.pet.service.impl;
import com.nuanzhua.pet.service.MemberApplicationService;
import org.springframework.stereotype.Service;
import com.nuanzhua.pet.dto.MemberRequests.*;
import com.nuanzhua.pet.common.Db;
import com.nuanzhua.pet.exception.BusinessException;
import com.nuanzhua.pet.service.MemberService;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;

import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** 业务服务实现；写操作在服务事务内完成，数据库约束提供最后一致性保护。 */
@Service
@org.springframework.transaction.annotation.Transactional
public class MemberApplicationServiceImpl implements MemberApplicationService {
    private final Db db; private final MemberService members; private final boolean demo;
    public MemberApplicationServiceImpl(Db db,MemberService members,@Value("${app.demo-enabled}") boolean demo) { this.db=db; this.members=members; this.demo=demo; }

    /** 公开查询服务价格、会员套餐及已启用权益。 */
    public Api<?> catalog() {
        return Api.ok(Map.of("services",db.queryForList("select * from service_item where active order by id"),"plans",db.queryForList("select * from membership_plan where active order by id"),"benefits",db.queryForList("select * from membership_benefit where enabled order by id")));
    }
    /** 公开查询应用健康状态和AI开关。 */
    public Api<?> health() { return Api.ok(Map.of("status","UP","aiEnabled",false,"demoPaymentsEnabled",demo)); }
    /** 查询当前登录人自己的业务数据。 */
    public Api<?> mine(Actor actor) { actor.role("OWNER"); return Api.ok(db.queryForList("select * from membership where owner_id=?",actor.id())); }
    /** 按当前套餐创建待付款会员购买单，冻结价格、期限及赠券。 */
    public Api<?> purchase(Actor actor,Purchase r) { return Api.ok(members.purchase(actor,r.planId())); }
    /** 本地演示支付入口；必须开启DEMO_ENABLED。 */
    public Api<?> pay(Actor actor,long id) {
        if(!demo) throw new BusinessException(HttpStatus.NOT_IMPLEMENTED,"演示支付未启用，微信支付待配置"); members.payDemo(actor,id); return Api.ok(true);
    }
}
