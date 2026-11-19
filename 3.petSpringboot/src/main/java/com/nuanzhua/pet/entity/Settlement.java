package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.*;

/** 喂养师结算实体，对应表 settlement；金额单位元，时间使用带时区时间。 */
@TableName("settlement")
public class Settlement {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 服务订单ID。 */
    @TableField("order_id")
    private Long orderId;
    /** 喂养师档案ID。 */
    @TableField("sitter_id")
    private Long sitterId;
    /** 计费基数，按订单实付金额。 */
    @TableField("gross_amount")
    private BigDecimal grossAmount;
    /** 平台抽成百分数，18表示18%。 */
    @TableField("commission_rate")
    private BigDecimal commissionRate;
    /** 平台抽成金额。 */
    @TableField("platform_amount")
    private BigDecimal platformAmount;
    /** 喂养师应得金额。 */
    @TableField("sitter_amount")
    private BigDecimal sitterAmount;
    /** 业务状态，枚举见本表CHECK约束。 */
    @TableField("status")
    private String status;
    /** 财务确认结算时间。 */
    @TableField("settled_at")
    private OffsetDateTime settledAt;
    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { this.orderId=value; }
    public Long getSitterId() { return sitterId; }
    public void setSitterId(Long value) { this.sitterId=value; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal value) { this.grossAmount=value; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal value) { this.commissionRate=value; }
    public BigDecimal getPlatformAmount() { return platformAmount; }
    public void setPlatformAmount(BigDecimal value) { this.platformAmount=value; }
    public BigDecimal getSitterAmount() { return sitterAmount; }
    public void setSitterAmount(BigDecimal value) { this.sitterAmount=value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status=value; }
    public OffsetDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(OffsetDateTime value) { this.settledAt=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
}
