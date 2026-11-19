package com.nuanzhua.pet.controller;
import com.nuanzhua.pet.service.ConfigApplicationService;
import com.nuanzhua.pet.dto.ConfigRequests;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import java.math.*;
import java.util.*;
import static com.nuanzhua.pet.service.ConfigService.values;

/** 后台业务配置REST接口；负责参数校验和服务调用，不直接访问数据库。 */
@Tag(name="后台业务配置",description="后台业务配置相关接口")
@RestController @RequestMapping("/api/admin")
public class ConfigController {
    private final ConfigApplicationService application;
    public ConfigController(ConfigApplicationService application) { this.application=application; }
    /** 更新服务价格和启用状态，不修改历史订单快照。 */
    @Operation(summary="更新服务价格和启用状态，不修改历史订单快照")
    @PutMapping("/prices/{id}") public Api<?> price(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id,@Valid @RequestBody ConfigRequests.Price r) { return application.price(actor,id,r); }
    /** 新增自定义会员套餐。 */
    @Operation(summary="新增自定义会员套餐")
    @PostMapping("/plans") public Api<?> addPlan(@Parameter(hidden=true) @RequestAttribute Actor actor,@Valid @RequestBody ConfigRequests.Plan r) { return application.addPlan(actor,r); }
    /** 保存会员套餐；历史购买价格不受新配置影响。 */
    @Operation(summary="保存会员套餐；历史购买价格不受新配置影响")
    @PutMapping("/plans/{id}") public Api<?> plan(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id,@Valid @RequestBody ConfigRequests.Plan r) { return application.plan(actor,id,r); }
    /** 保存会员权益开关及折扣乘数。 */
    @Operation(summary="保存会员权益开关及折扣乘数")
    @PutMapping("/benefits/{id}") public Api<?> benefit(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id,@Valid @RequestBody ConfigRequests.Benefit r) { return application.benefit(actor,id,r); }
    /** 新增等级规则，默认下个结算期生效。 */
    @Operation(summary="新增等级规则，默认下个结算期生效")
    @PostMapping("/grades") public Api<?> addGrade(@Parameter(hidden=true) @RequestAttribute Actor actor,@Valid @RequestBody ConfigRequests.Grade r) { return application.addGrade(actor,r); }
    /** 保存等级规则；未来生效规则独立留存，避免提前修改现行抽成。 */
    @Operation(summary="保存等级规则；未来生效规则独立留存，避免提前修改现行抽成")
    @PutMapping("/grades/{id}") public Api<?> grade(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id,@Valid @RequestBody ConfigRequests.Grade r) { return application.grade(actor,id,r); }
    /** 按当前生效规则模拟符合条件的等级，不修改档案。 */
    @Operation(summary="按当前生效规则模拟符合条件的等级，不修改档案")
    @GetMapping("/grades/simulate") public Api<?> simulate(@Parameter(hidden=true) @RequestAttribute Actor actor,@RequestParam int orders,@RequestParam BigDecimal rating,@RequestParam int complaints) { return application.simulate(actor,orders,rating,complaints); }
    /** 按当前生效等级试算平台与喂养师结算金额。 */
    @Operation(summary="按当前生效等级试算平台与喂养师结算金额")
    @GetMapping("/grades/estimate") public Api<?> estimate(@Parameter(hidden=true) @RequestAttribute Actor actor,@RequestParam BigDecimal amount) { return application.estimate(actor,amount); }
    /** 新增优惠券模板及领取限制。 */
    @Operation(summary="新增优惠券模板及领取限制")
    @PostMapping("/coupons") public Api<?> addCoupon(@Parameter(hidden=true) @RequestAttribute Actor actor,@Valid @RequestBody ConfigRequests.Coupon r) { return application.addCoupon(actor,r); }
    /** 保存优惠券模板，参数来自受控DTO。 */
    @Operation(summary="保存优惠券模板，参数来自受控DTO")
    @PutMapping("/coupons/{id}") public Api<?> coupon(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id,@Valid @RequestBody ConfigRequests.Coupon r) { return application.coupon(actor,id,r); }
    /** 保存消息模板，安全类进出提醒不可关闭。 */
    @Operation(summary="保存消息模板，安全类进出提醒不可关闭")
    @PutMapping("/notification-templates/{id}") public Api<?> notice(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id,@Valid @RequestBody ConfigRequests.Notice r) { return application.notice(actor,id,r); }
    /** 启用或封禁宠主账号。 */
    @Operation(summary="启用或封禁宠主账号")
    @PutMapping("/owners/{id}/status") public Api<?> ownerStatus(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id,@Valid @RequestBody ConfigRequests.Status r) { return application.ownerStatus(actor,id,r); }
    /** 更新会员账户启停状态。 */
    @Operation(summary="更新会员账户启停状态")
    @PutMapping("/memberships/{id}/status") public Api<?> memberStatus(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id,@Valid @RequestBody ConfigRequests.MemberStatus r) { return application.memberStatus(actor,id,r); }
    /** 批量结算，锁定订单后锁定结算，防止与退款并发冲突。 */
    @Operation(summary="批量结算，锁定订单后锁定结算，防止与退款并发冲突")
    @PostMapping("/settlements/batch-settle") public Api<?> settle(@Parameter(hidden=true) @RequestAttribute Actor actor,@Valid @RequestBody ConfigRequests.Batch r) { return application.settle(actor,r); }
    /** 更新受控角色权限集合。 */
    @Operation(summary="更新受控角色权限集合")
    @PutMapping("/roles/{role}/permissions") public Api<?> permissions(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable String role,@Valid @RequestBody ConfigRequests.Permissions r) { return application.permissions(actor,role,r); }
    /** 新增客服或财务账号，初始密码以BCrypt摘要保存。 */
    @Operation(summary="新增客服或财务账号，初始密码以BCrypt摘要保存")
    @PostMapping("/staff") public Api<?> staff(@Parameter(hidden=true) @RequestAttribute Actor actor,@Valid @RequestBody ConfigRequests.Staff r) { return application.staff(actor,r); }
}
