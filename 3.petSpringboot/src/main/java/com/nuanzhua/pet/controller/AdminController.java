package com.nuanzhua.pet.controller;

import com.nuanzhua.pet.service.AdminApplicationService;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;

import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.*;

/**
 * 运营查询REST接口；负责参数校验和服务调用，不直接访问数据库。
 */
@Tag(name = "运营查询", description = "运营查询相关接口")
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminApplicationService application;

    public AdminController(AdminApplicationService application) {
        this.application = application;
    }

    /**
     * 分页查询业务列表，表名与过滤列由服务端白名单提供。
     */
    @Operation(summary = "分页查询业务列表，表名与过滤列由服务端白名单提供")
    @GetMapping("/modules/{module}")
    public Api<?> list(@Parameter(hidden = true) @RequestAttribute Actor actor, @PathVariable String module,
                       @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
                       @RequestParam(defaultValue = "") String keyword, @RequestParam(defaultValue = "") String status) {
        return application.list(actor, module, page, size, keyword, status);
    }

    /**
     * 查询运营工作台汇总指标。
     */
    @Operation(summary = "查询运营工作台汇总指标")
    @GetMapping("/dashboard")
    public Api<?> dashboard(@Parameter(hidden = true) @RequestAttribute Actor actor) {
        return application.dashboard(actor);
    }

    /**
     * 查询当前schema的数据字典。
     */
    @Operation(summary = "查询当前schema的数据字典")
    @GetMapping("/dictionary")
    public Api<?> dictionary(@Parameter(hidden = true) @RequestAttribute Actor actor) {
        return application.dictionary(actor);
    }

    /**
     * 宠主关联档案，不返回密码或登录会话。
     */
    @Operation(summary = "查询宠主基本信息、宠物、地址和会员档案")
    @GetMapping("/owners/{id}")
    public Api<?> owner(@Parameter(hidden = true) @RequestAttribute Actor actor, @PathVariable long id) {
        return application.owner(actor, id);
    }
}
