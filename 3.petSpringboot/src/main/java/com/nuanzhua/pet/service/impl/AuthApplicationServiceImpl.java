package com.nuanzhua.pet.service.impl;
import com.nuanzhua.pet.service.AuthApplicationService;
import org.springframework.stereotype.Service;
import com.nuanzhua.pet.dto.AuthRequests.*;
import com.nuanzhua.pet.service.AuthService;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;

import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

/** 业务服务实现；写操作在服务事务内完成，数据库约束提供最后一致性保护。 */
@Service
@org.springframework.transaction.annotation.Transactional
public class AuthApplicationServiceImpl implements AuthApplicationService {
    private final AuthService auth;
    public AuthApplicationServiceImpl(AuthService auth) { this.auth=auth; }

    /** 校验账号密码并生成有时效的随机Bearer会话。 */
    public Api<?> login(Login r) { return Api.ok(auth.login(r.username(),r.password())); }
    /** 注册宠主账号，使用BaseMapper持久化并校验账号和手机重复。 */
    public Api<?> register(Register r) { return Api.ok(auth.register(r.username(),r.password(),r.name(),r.phone())); }
    /** 返回当前登录人及权限。 */
    public Api<?> me(Actor actor) {
        var result=new java.util.LinkedHashMap<String,Object>();
        result.put("id",actor.id()); result.put("name",actor.name()); result.put("role",actor.role()); result.put("permissions",actor.permissions());
        result.put("canSwitchToSitter",auth.hasSitterProfile(actor.id()));
        return Api.ok(result);
    }
    /** 仅允许普通宠主/喂养师切换会话角色，且喂养师身份必须有有效档案。 */
    public Api<?> switchRole(Actor actor,String role,String token) {
        if(!java.util.Set.of("OWNER","SITTER").contains(actor.role())) throw com.nuanzhua.pet.exception.BusinessException.forbidden();
        if("SITTER".equals(role) && !auth.hasSitterProfile(actor.id())) throw com.nuanzhua.pet.exception.BusinessException.forbidden();
        auth.switchRole(token,role);
        return Api.ok(auth.authenticate(token));
    }
    /** 删除当前登录会话，令牌立即失效。 */
    public Api<?> logout(String header) { auth.logout(header.substring(7)); return Api.ok(true); }
}
