package com.nuanzhua.pet.controller;
import com.nuanzhua.pet.service.OperationsApplicationService;
import com.nuanzhua.pet.dto.OperationsRequests.*;

import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.*;

/** 客服通知及应急REST接口；负责参数校验和服务调用，不直接访问数据库。 */
@Tag(name="客服通知及应急",description="客服通知及应急相关接口")
@RestController @RequestMapping("/api")
public class OperationsController {
    private final OperationsApplicationService application;
    public OperationsController(OperationsApplicationService application) { this.application=application; }
    /** 创建应急工单并向宠主和运营人员发送站内提醒。 */
    @Operation(summary="创建应急工单并向宠主和运营人员发送站内提醒")
    @PostMapping("/emergencies") public Api<?> emergency(@Parameter(hidden=true) @RequestAttribute Actor actor,@Valid @RequestBody Ticket r) { return application.emergency(actor,r); }
    /** 记录宠主明确同意紧急送医。 */
    @Operation(summary="记录宠主明确同意紧急送医")
    @PostMapping("/emergencies/{id}/consent") public Api<?> consent(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id) { return application.consent(actor,id); }
    /** 推进应急工单；送医前必须有宠主同意和目标医院。 */
    @Operation(summary="推进应急工单；送医前必须有宠主同意和目标医院")
    @PutMapping("/admin/emergencies/{id}") public Api<?> updateEmergency(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id,@Valid @RequestBody EmergencyUpdate r) { return application.updateEmergency(actor,id,r); }
    /** 按直线距离查询最近的合作医院。 */
    @Operation(summary="按直线距离查询最近的合作医院")
    @GetMapping("/hospitals/nearby") public Api<?> hospitals(@Parameter(hidden=true) @RequestAttribute Actor actor,@RequestParam double longitude,@RequestParam double latitude) { return application.hospitals(actor,longitude,latitude); }
    /** 新增合作医院。 */
    @Operation(summary="新增合作医院")
    @PostMapping("/admin/hospitals") public Api<?> hospital(@Parameter(hidden=true) @RequestAttribute Actor actor,@Valid @RequestBody Hospital r) { return application.hospital(actor,r); }
    /** 创建关联订单的投诉工单。 */
    @Operation(summary="创建关联订单的投诉工单")
    @PostMapping("/complaints") public Api<?> complaint(@Parameter(hidden=true) @RequestAttribute Actor actor,@Valid @RequestBody Ticket r) { return application.complaint(actor,r); }
    /** 处理或关闭投诉并保存解决说明。 */
    @Operation(summary="处理或关闭投诉并保存解决说明")
    @PutMapping("/admin/complaints/{id}") public Api<?> resolve(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id,@Valid @RequestBody ComplaintUpdate r) { return application.resolve(actor,id,r); }
    /** 查询或创建客服会话并校验归属。 */
    @Operation(summary="查询或创建客服会话并校验归属")
    @PostMapping("/support/conversations") public Api<?> conversation(@Parameter(hidden=true) @RequestAttribute Actor actor,@Valid @RequestBody Conversation r) { return application.conversation(actor,r); }
    /** 宠主请求人工客服接管当前会话。 */
    @Operation(summary="宠主请求人工客服接管当前会话")
    @PostMapping("/support/conversations/{id}/handoff") public Api<?> handoff(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id) { return application.requestHuman(actor,id); }
    @PostMapping("/support/conversations/{id}/resume-ai") public Api<?> resumeAi(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id) { return application.resumeAi(actor,id); }
    /** 分页查询可访问的客服会话。 */
    @Operation(summary="分页查询可访问的客服会话")
    @GetMapping("/support/conversations") public Api<?> conversations(@Parameter(hidden=true) @RequestAttribute Actor actor,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size) { return application.conversations(actor,page,size); }
    /** 人工接管客服会话，防止多个客服同时接管。 */
    @Operation(summary="人工接管客服会话，防止多个客服同时接管")
    @PostMapping("/admin/conversations/{id}/takeover") public Api<?> takeover(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id) { return application.takeover(actor,id); }
    /** 关闭人工客服会话。 */
    @Operation(summary="关闭人工客服会话")
    @PostMapping("/admin/conversations/{id}/close") public Api<?> close(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id) { return application.close(actor,id); }
    /** 发送客服或订单消息；人工回复前必须接管会话。 */
    @Operation(summary="发送客服或订单消息；人工回复前必须接管会话")
    @PostMapping("/messages") public Api<?> send(@Parameter(hidden=true) @RequestAttribute Actor actor,@Valid @RequestBody Message r) { return application.send(actor,r); }
    /** 分页查询客服或订单消息，必须且只能指定一种会话。 */
    @Operation(summary="分页查询客服或订单消息，必须且只能指定一种会话")
    @GetMapping("/messages") public Api<?> messages(@Parameter(hidden=true) @RequestAttribute Actor actor,@RequestParam(required=false) Long conversationId,@RequestParam(required=false) Long orderId,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) { return application.messages(actor,conversationId,orderId,page,size); }
    /** 分页查询当前账号的站内通知。 */
    @Operation(summary="分页查询当前账号的站内通知")
    @GetMapping("/notifications") public Api<?> notifications(@Parameter(hidden=true) @RequestAttribute Actor actor,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) { return application.notifications(actor,page,size); }
    /** 标记当前账号全部通知已读。 */
    @Operation(summary="标记当前账号全部通知已读")
    @PostMapping("/notifications/read-all") public Api<?> readAll(@Parameter(hidden=true) @RequestAttribute Actor actor) { return application.readAll(actor); }
    /** 标记自己的通知已读。 */
    @Operation(summary="标记自己的通知已读")
    @PostMapping("/notifications/{id}/read") public Api<?> read(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id) { return application.read(actor,id); }
}
