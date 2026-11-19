package com.nuanzhua.pet.vo;
import com.nuanzhua.pet.exception.BusinessException;

import java.util.Set;

/** 统一响应及登录上下文视图，不包含密码或令牌摘要。 */
public record Actor(long id, String name, String role, Set<String> permissions) {
    /** 新增客服或财务账号，初始密码以BCrypt摘要保存。 */
    public boolean staff() { return Set.of("ADMIN","SUPPORT","FINANCE").contains(role); }
    /** 断言当前角色具备业务权限，支持管理员通配符。 */
    public void require(String permission) {
        if (!permissions.contains("*") && !permissions.contains(permission)) throw BusinessException.forbidden();
    }
    /** 断言账号属于指定角色。 */
    public void role(String expected) { if (!role.equals(expected)) throw BusinessException.forbidden(); }
}
