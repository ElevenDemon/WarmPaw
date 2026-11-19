package com.nuanzhua.pet.controller;

import com.nuanzhua.pet.service.OrderApplicationService;
import com.nuanzhua.pet.service.DispatchService;
import com.nuanzhua.pet.dto.OrderRequests;
import com.nuanzhua.pet.entity.Review;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.*;

/**
 * 订单与服务REST接口；负责参数校验和服务调用，不直接访问数据库。
 */
@Tag(name = "订单与服务", description = "订单与服务相关接口")
@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderApplicationService application;
    private final DispatchService dispatch;

    public OrderController(OrderApplicationService application, DispatchService dispatch) {
        this.application = application;
        this.dispatch = dispatch;
    }

    /**
     * 创建订单；按服务端价格计算，冻结协议和价格快照，requestKey保证幂等。
     */
    @Operation(summary = "创建订单；按服务端价格计算，冻结协议和价格快照，requestKey保证幂等")
    @PostMapping("/orders")
    public Api<?> create(@Parameter(hidden = true) @RequestAttribute Actor actor, @Valid @RequestBody OrderRequests.Create r) {
        return application.create(actor, r);
    }

    /**
     * 分页查询业务列表，表名与过滤列由服务端白名单提供。
     */
    @Operation(summary = "分页查询业务列表，表名与过滤列由服务端白名单提供")
    @GetMapping("/orders")
    public Api<?> list(@Parameter(hidden = true) @RequestAttribute Actor actor, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) String status) {
        return application.list(actor, page, size, status);
    }

    /**
     * 查询业务详情，并校验访问权限。
     */
    @Operation(summary = "查询业务详情，并校验访问权限")
    @GetMapping("/orders/{id}")
    public Api<?> detail(@Parameter(hidden = true) @RequestAttribute Actor actor, @PathVariable long id) {
        return application.detail(actor, id);
    }

    /**
     * 取消待付款订单并释放锁定优惠券。
     */
    @Operation(summary = "取消待付款订单并释放锁定优惠券")
    @PostMapping("/orders/{id}/cancel")
    public Api<?> cancel(@Parameter(hidden = true) @RequestAttribute Actor actor, @PathVariable long id, @Valid @RequestBody OrderRequests.Reason r) {
        return application.cancel(actor, id, r);
    }

    /**
     * 本地演示支付入口；必须开启DEMO_ENABLED。
     */
    @Operation(summary = "本地演示支付入口；必须开启DEMO_ENABLED")
    @PostMapping("/orders/{id}/demo-pay")
    public Api<?> pay(@Parameter(hidden = true) @RequestAttribute Actor actor, @PathVariable long id) {
        return application.pay(actor, id);
    }

    /**
     * 人工派单；校验档期、半径、容量与资质，按订单再喂养师顺序加锁。
     */
    @Operation(summary = "人工派单；校验档期、半径、容量与资质，按订单再喂养师顺序加锁")
    @PostMapping("/admin/orders/{id}/assign")
    public Api<?> assign(@Parameter(hidden = true) @RequestAttribute Actor actor, @PathVariable long id, @Valid @RequestBody OrderRequests.Assign r) {
        return application.assign(actor, id, r);
    }

    /**
     * 返回人工派单准入结果及所有淘汰原因，不执行AI排序。
     */
    @Operation(summary = "返回人工派单准入结果及所有淘汰原因，不执行AI排序")
    @GetMapping("/admin/orders/{id}/eligibility/{sitterId}")
    public Api<?> eligibility(@Parameter(hidden = true) @RequestAttribute Actor actor, @PathVariable long id, @PathVariable long sitterId) {
        return application.eligibility(actor, id, sitterId);
    }

    @Operation(summary = "查询智能派单候选人及评分")
    @GetMapping("/admin/orders/{id}/dispatch-candidates")
    public Api<?> dispatchCandidates(@Parameter(hidden = true) @RequestAttribute Actor actor, @PathVariable long id) {
        return Api.ok(dispatch.candidates(id, actor));
    }

    @Operation(summary = "按评分自动派单")
    @PostMapping("/admin/orders/{id}/auto-dispatch")
    public Api<?> autoDispatch(@Parameter(hidden = true) @RequestAttribute Actor actor, @PathVariable long id) {
        return Api.ok(dispatch.autoAssign(id, actor));
    }

    @Operation(summary = "调用大模型从合格候选人中选择并派单")
    @PostMapping("/admin/orders/{id}/llm-dispatch")
    public Api<?> llmDispatch(@Parameter(hidden = true) @RequestAttribute Actor actor, @PathVariable long id) {
        return Api.ok(dispatch.llmAssign(id, actor));
    }

    /**
     * 喂养师确认接单；必须在接单期限内操作。
     */
    @Operation(summary = "喂养师确认接单；必须在接单期限内操作")
    @PostMapping("/orders/{id}/accept")
    public Api<?> accept(@Parameter(hidden = true) @RequestAttribute Actor actor, @PathVariable long id) {
        return application.accept(actor, id);
    }

    /**
     * 喂养师拒单，退回人工待派单。
     */
    @Operation(summary = "喂养师拒单，退回人工待派单")
    @PostMapping("/orders/{id}/reject")
    public Api<?> reject(@Parameter(hidden = true) @RequestAttribute Actor actor, @PathVariable long id) {
        return application.reject(actor, id);
    }

    /**
     * 喂养师进门打卡；创建标准清单并通知宠主。
     */
    @Operation(summary = "喂养师进门打卡；创建标准清单并通知宠主")
    @PostMapping("/orders/{id}/checkin")
    public Api<?> checkin(@Parameter(hidden = true) @RequestAttribute Actor actor, @PathVariable long id) {
        return application.checkin(actor, id);
    }

    /**
     * 保存标准服务清单；陪玩至少10分钟且单位为MINUTE。
     */
    @Operation(summary = "保存标准服务清单；陪玩至少10分钟且单位为MINUTE")
    @PutMapping("/orders/{id}/checklist")
    public Api<?> checklist(@Parameter(hidden = true) @RequestAttribute Actor actor, @PathVariable long id, @Valid @RequestBody OrderRequests.Checklist r) {
        return application.checklist(actor, id, r);
    }

    /**
     * 登记服务媒体元数据，不处理文件上传或生成播放地址。
     */
    @Operation(summary = "登记服务媒体元数据，不处理文件上传或生成播放地址")
    @PostMapping("/orders/{id}/media")
    public Api<?> media(@Parameter(hidden = true) @RequestAttribute Actor actor, @PathVariable long id, @Valid @RequestBody OrderRequests.Media r) {
        return application.media(actor, id, r);
    }

    /**
     * 查询未删除且未过期的订单媒体元数据。
     */
    @Operation(summary = "查询未删除且未过期的订单媒体元数据")
    @GetMapping("/orders/{id}/media")
    public Api<?> mediaList(@Parameter(hidden = true) @RequestAttribute Actor actor, @PathVariable long id) {
        return application.mediaList(actor, id);
    }

    /**
     * 软删除指定订单媒体；仅限有权限的订单参与人。
     */
    @Operation(summary = "软删除指定订单媒体；仅限有权限的订单参与人")
    @DeleteMapping("/orders/{id}/media/{mediaId}")
    public Api<?> deleteMedia(@Parameter(hidden = true) @RequestAttribute Actor actor, @PathVariable long id, @PathVariable long mediaId) {
        return application.deleteMedia(actor, id, mediaId);
    }

    /**
     * 完成服务；校验清单和录像，原子创建待处理报告、结算及离门通知。
     */
    @Operation(summary = "完成服务；校验清单和录像，原子创建待处理报告、结算及离门通知")
    @PostMapping("/orders/{id}/complete")
    public Api<?> complete(@Parameter(hidden = true) @RequestAttribute Actor actor, @PathVariable long id) {
        return application.complete(actor, id);
    }

    /**
     * 读取订单报告；当前仅返回原始快照及PENDING_AI状态。
     */
    @Operation(summary = "读取订单报告；当前仅返回原始快照及PENDING_AI状态")
    @GetMapping("/orders/{id}/report")
    public Api<?> report(@Parameter(hidden = true) @RequestAttribute Actor actor, @PathVariable long id) {
        return application.report(actor, id);
    }

    /**
     * 订单完成后评价，单订单只允许一次并更新综合评分。
     */
    @Operation(summary = "订单完成后评价，单订单只允许一次并更新综合评分")
    @PostMapping("/orders/{id}/review")
    public Api<?> review(@Parameter(hidden = true) @RequestAttribute Actor actor, @PathVariable long id, @Valid @RequestBody OrderRequests.Review r) {
        return application.review(actor, id, r);
    }

    /**
     * 本地演示退款入口；需要财务退款权限。
     */
    @Operation(summary = "本地演示退款入口；需要财务退款权限")
    @PostMapping("/admin/orders/{id}/demo-refund")
    public Api<?> refund(@Parameter(hidden = true) @RequestAttribute Actor actor, @PathVariable long id, @Valid @RequestBody OrderRequests.Reason r) {
        return application.refund(actor, id, r);
    }

    /**
     * AI功能预留；当前返回HTTP 501，后续由Python实现。
     */
    @Operation(summary = "AI功能预留；当前返回HTTP 501，后续由Python实现")
    @PostMapping({"/admin/ai/dispatch", "/ai/report", "/ai/route"})
    public Api<?> ai() {
        return application.ai();
    }
}
