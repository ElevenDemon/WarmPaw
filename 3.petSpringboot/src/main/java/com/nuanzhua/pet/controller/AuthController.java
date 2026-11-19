package com.nuanzhua.pet.controller;
import com.nuanzhua.pet.service.AuthApplicationService;
import com.nuanzhua.pet.dto.AuthRequests.*;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

/** 账号认证REST接口；负责参数校验和服务调用，不直接访问数据库。 */
@Tag(name="账号认证",description="账号认证相关接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthApplicationService application;
    public AuthController(AuthApplicationService application) { this.application=application; }
    /** 校验账号密码并生成有时效的随机Bearer会话。 */
    @SecurityRequirements
    @Operation(summary="校验账号密码并生成有时效的随机Bearer会话")
    @PostMapping("/login") public Api<?> login(@Valid @RequestBody Login r) { return application.login(r); }
    /** 注册宠主账号，使用BaseMapper持久化并校验账号和手机重复。 */
    @SecurityRequirements
    @Operation(summary="注册宠主账号，使用BaseMapper持久化并校验账号和手机重复")
    @PostMapping("/register") public Api<?> register(@Valid @RequestBody Register r) { return application.register(r); }
    /** 返回当前登录人及权限。 */
    @Operation(summary="返回当前登录人及权限")
    @GetMapping("/me") public Api<?> me(@Parameter(hidden=true) @RequestAttribute Actor actor) { return application.me(actor); }
    /** 在宠主和喂养师工作台之间切换当前登录会话角色。 */
    @PostMapping("/switch-role") public Api<?> switchRole(@Parameter(hidden=true) @RequestAttribute Actor actor,@Valid @RequestBody SwitchRole r,
            @RequestHeader("Authorization") String header) { return application.switchRole(actor,r.role(),header.substring(7)); }
    /** 删除当前登录会话，令牌立即失效。 */
    @Operation(summary="删除当前登录会话，令牌立即失效")
    @PostMapping("/logout") public Api<?> logout(@RequestHeader("Authorization") String header) { return application.logout(header); }
    public record SwitchRole(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Pattern(regexp="OWNER|SITTER") String role) {}
}
