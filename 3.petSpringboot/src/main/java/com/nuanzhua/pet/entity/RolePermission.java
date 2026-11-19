package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;

/** 角色权限实体，对应表 role_permission；金额单位元，时间使用带时区时间。 */
@TableName("role_permission")
public class RolePermission {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 账号角色。 */
    @TableField("role")
    private String role;
    /** 权限代码，管理员为*。 */
    @TableField("permission")
    private String permission;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public String getRole() { return role; }
    public void setRole(String value) { this.role=value; }
    public String getPermission() { return permission; }
    public void setPermission(String value) { this.permission=value; }
}
