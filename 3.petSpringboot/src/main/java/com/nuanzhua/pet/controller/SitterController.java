package com.nuanzhua.pet.controller;
import com.nuanzhua.pet.service.SitterApplicationService;
import com.nuanzhua.pet.dto.SitterRequests;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

/** 喂养师与资质REST接口；负责参数校验和服务调用，不直接访问数据库。 */
@Tag(name="喂养师与资质",description="喂养师与资质相关接口")
@RestController @RequestMapping("/api")
public class SitterController {
    private final SitterApplicationService application;
    /** 入驻审核与喂养师信息使用同一个档案ID。 */
    @Operation(summary="通过或驳回喂养师入驻申请，驳回意见可用于再次申请")
    @PostMapping("/admin/sitters/{id}/audit")
    public Api<?> admission(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id,
        @Valid @RequestBody SitterRequests.AdmissionAudit r) { return application.admission(actor,id,r); }
    public SitterController(SitterApplicationService application) { this.application=application; }
    /** 提交入驻申请或执行到期等级规则。 */
    @Operation(summary="提交入驻申请或执行到期等级规则")
    @PostMapping("/sitter/application") public Api<?> apply(@Parameter(hidden=true) @RequestAttribute Actor actor,@Valid @RequestBody SitterRequests.Application r) { return application.apply(actor,r); }
    /** 查询当前登录人自己的业务数据。 */
    @Operation(summary="查询当前登录人自己的业务数据")
    @GetMapping("/sitter/profile") public Api<?> mine(@Parameter(hidden=true) @RequestAttribute Actor actor) { return application.mine(actor); }
    /** 保存喂养师可自主编辑的信息，后台专有字段不允许自改。 */
    @Operation(summary="保存喂养师可自主编辑的信息，后台专有字段不允许自改")
    @PutMapping("/sitter/profile") public Api<?> personal(@Parameter(hidden=true) @RequestAttribute Actor actor,@Valid @RequestBody SitterRequests.Personal r) { return application.personal(actor,r); }
    /** 提交分类资质材料并进入待审核状态。 */
    @Operation(summary="提交分类资质材料并进入待审核状态")
    @PostMapping("/sitter/qualifications") public Api<?> qualification(@Parameter(hidden=true) @RequestAttribute Actor actor,@Valid @RequestBody SitterRequests.Qualification r) { return application.qualification(actor,r); }
    /** 汇总喂养师本月待结算和已结算收入。 */
    @Operation(summary="汇总喂养师本月待结算和已结算收入")
    @GetMapping("/sitter/income") public Api<?> income(@Parameter(hidden=true) @RequestAttribute Actor actor) { return application.income(actor); }
    /** 查询业务详情，并校验访问权限。 */
    @Operation(summary="查询业务详情，并校验访问权限")
    @GetMapping("/admin/sitters/{id}") public Api<?> detail(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id) { return application.detail(actor,id); }
    /** 事务保存业务配置或档案，并记录变更审计。 */
    @Operation(summary="事务保存业务配置或档案，并记录变更审计")
    @PutMapping("/admin/sitters/{id}") public Api<?> save(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id,@Valid @RequestBody SitterRequests.Profile r) { return application.save(actor,id,r); }
    /** 审核分类资质及有效期。 */
    @Operation(summary="审核分类资质及有效期")
    @PostMapping("/admin/qualifications/{id}/audit") public Api<?> audit(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id,@Valid @RequestBody SitterRequests.Audit r) { return application.audit(actor,id,r); }
}
