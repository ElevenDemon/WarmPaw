package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.OffsetDateTime;

/** 统一账号实体，对应表 app_user。 */
@TableName("app_user")
public class AppUser {
    /** 数据库自增主键。 */
    @TableId(type=IdType.AUTO) public Long id;
    /** 登录账号。 */
    public String username;
    /** BCrypt密码摘要；禁止序列化到接口响应。 */
    @JsonIgnore public String passwordHash;
    /** 名称。 */
    public String name;
    /** 手机号码。 */
    public String phone;
    /** 账号角色。 */
    public String role;
    /** 业务状态，枚举见本表CHECK约束。 */
    public String status;
    /** 创建时间。 */
    public OffsetDateTime createdAt;
    /** 最后更新时间（由业务显式更新）。 */
    public OffsetDateTime updatedAt;
}
