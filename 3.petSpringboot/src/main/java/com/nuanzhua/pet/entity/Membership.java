package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.*;

/** 宠主会员账户实体，对应表 membership；金额单位元，时间使用带时区时间。 */
@TableName("membership")
public class Membership {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 宠主账号ID。 */
    @TableField("owner_id")
    private Long ownerId;
    /** 会员套餐ID。 */
    @TableField("plan_id")
    private Long planId;
    /** 开始时间。 */
    @TableField("starts_at")
    private OffsetDateTime startsAt;
    /** 到期时间。 */
    @TableField("expires_at")
    private OffsetDateTime expiresAt;
    /** 业务状态，枚举见本表CHECK约束。 */
    @TableField("status")
    private String status;
    /** 累计会员优惠金额。 */
    @TableField("saved_amount")
    private BigDecimal savedAmount;
    /** 最后更新时间（由业务显式更新）。 */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long value) { this.ownerId=value; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long value) { this.planId=value; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(OffsetDateTime value) { this.startsAt=value; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime value) { this.expiresAt=value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status=value; }
    public BigDecimal getSavedAmount() { return savedAmount; }
    public void setSavedAmount(BigDecimal value) { this.savedAmount=value; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime value) { this.updatedAt=value; }
}
