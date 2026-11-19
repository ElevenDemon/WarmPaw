package com.nuanzhua.pet.controller;
import com.nuanzhua.pet.service.CouponApplicationService;
import com.nuanzhua.pet.dto.CouponRequests.*;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.*;

/** 优惠券REST接口；负责参数校验和服务调用，不直接访问数据库。 */
@Tag(name="优惠券",description="优惠券相关接口")
@RestController
@RequestMapping("/api")
public class CouponController {
    private final CouponApplicationService application;
    public CouponController(CouponApplicationService application) { this.application=application; }
    /** 查询可领取优惠券中心。 */
    @Operation(summary="查询可领取优惠券中心")
    @GetMapping("/coupons/center") public Api<?> center(@Parameter(hidden=true) @RequestAttribute Actor actor) { return application.center(actor); }
    /** 查询当前登录人自己的业务数据。 */
    @Operation(summary="查询当前登录人自己的业务数据")
    @GetMapping("/coupons/mine") public Api<?> mine(@Parameter(hidden=true) @RequestAttribute Actor actor,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size) { return application.mine(actor,page,size); }
    /** 按七步规则领取优惠券；失败原因同样持久化。 */
    @Operation(summary="按七步规则领取优惠券；失败原因同样持久化")
    @PostMapping("/coupons/{id}/claim") public Api<?> claim(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id,@Valid @RequestBody Claim r,HttpServletRequest request) { return application.claim(actor,id,r,request); }
    /** 根据兑换码领取优惠券，仍执行完整领取规则。 */
    @Operation(summary="根据兑换码领取优惠券，仍执行完整领取规则")
    @PostMapping("/coupons/redeem") public Api<?> redeem(@Parameter(hidden=true) @RequestAttribute Actor actor,@Valid @RequestBody Redeem r,HttpServletRequest request) { return application.redeem(actor,r,request); }
    /** 批量直发优惠券，区别于主动领取库存。 */
    @Operation(summary="批量直发优惠券，区别于主动领取库存")
    @PostMapping("/admin/coupons/{id}/push") public Api<?> push(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id,@Valid @RequestBody Push r) { return application.push(actor,id,r); }
}
