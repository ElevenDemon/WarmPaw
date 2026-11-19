package com.nuanzhua.pet.controller;

import com.nuanzhua.pet.service.MobileApplicationService;
import com.nuanzhua.pet.vo.*;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 移动端补充接口，不允许客户端自行指定用户归属。 */
@RestController @RequestMapping("/api") @Tag(name="移动端补充查询")
public class MobileController {
    private final MobileApplicationService application;
    public MobileController(MobileApplicationService application) { this.application=application; }
    @GetMapping("/emergencies/mine") @Operation(summary="查询当前用户参与订单的应急工单")
    public Api<?> emergencies(@RequestAttribute Actor actor,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) { return Api.ok(application.emergencies(actor,page,size)); }
    @GetMapping("/sitter/settlements") @Operation(summary="查询当前喂养师结算明细")
    public Api<?> settlements(@RequestAttribute Actor actor,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) { return Api.ok(application.settlements(actor,page,size)); }
}
