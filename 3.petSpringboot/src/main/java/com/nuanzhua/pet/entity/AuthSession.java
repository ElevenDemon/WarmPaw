package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;

/** 登录会话实体，对应表 auth_session；金额单位元，时间使用带时区时间。 */
@TableName("auth_session")
public class AuthSession {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 关联统一账号。 */
    @TableField("user_id")
    private Long userId;
    /** 随机Bearer令牌的SHA-256摘要。 */
    @TableField("token_hash")
    private String tokenHash;
    /** 到期时间。 */
    @TableField("expires_at")
    private OffsetDateTime expiresAt;
    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { this.userId=value; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String value) { this.tokenHash=value; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime value) { this.expiresAt=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
}
