package com.nuanzhua.pet.controller;
import com.nuanzhua.pet.service.MemberApplicationService;
import com.nuanzhua.pet.dto.MemberRequests.*;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

/** 会员与公开配置REST接口；负责参数校验和服务调用，不直接访问数据库。 */
@Tag(name="会员与公开配置",description="会员与公开配置相关接口")
@RestController @RequestMapping("/api")
public class MemberController {
    private final MemberApplicationService application;
    public MemberController(MemberApplicationService application) { this.application=application; }
    /** 公开查询服务价格、会员套餐及已启用权益。 */
    @SecurityRequirements
    @Operation(summary="公开查询服务价格、会员套餐及已启用权益")
    @GetMapping("/public/catalog") public Api<?> catalog() { return application.catalog(); }
    /** 公开查询应用健康状态和AI开关。 */
    @SecurityRequirements
    @Operation(summary="公开查询应用健康状态和AI开关")
    @GetMapping("/public/health") public Api<?> health() { return application.health(); }
    /** 查询当前登录人自己的业务数据。 */
    @Operation(summary="查询当前登录人自己的业务数据")
    @GetMapping("/membership/mine") public Api<?> mine(@Parameter(hidden=true) @RequestAttribute Actor actor) { return application.mine(actor); }
    /** 按当前套餐创建待付款会员购买单，冻结价格、期限及赠券。 */
    @Operation(summary="按当前套餐创建待付款会员购买单，冻结价格、期限及赠券")
    @PostMapping("/membership/purchases") public Api<?> purchase(@Parameter(hidden=true) @RequestAttribute Actor actor,@Valid @RequestBody Purchase r) { return application.purchase(actor,r); }
    /** 本地演示支付入口；必须开启DEMO_ENABLED。 */
    @Operation(summary="本地演示支付入口；必须开启DEMO_ENABLED")
    @PostMapping("/membership/purchases/{id}/demo-pay") public Api<?> pay(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id) { return application.pay(actor,id); }
}
