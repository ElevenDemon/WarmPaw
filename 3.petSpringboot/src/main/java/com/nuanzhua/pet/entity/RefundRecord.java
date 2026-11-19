package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.*;

/** 退款记录实体，对应表 refund_record；金额单位元，时间使用带时区时间。 */
@TableName("refund_record")
public class RefundRecord {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 服务订单ID。 */
    @TableField("order_id")
    private Long orderId;
    /** 金额，人民币元。 */
    @TableField("amount")
    private BigDecimal amount;
    /** 业务说明或失败理由。 */
    @TableField("reason")
    private String reason;
    /** 操作人账号ID。 */
    @TableField("operator_id")
    private Long operatorId;
    /** 支付渠道，当前DEMO。 */
    @TableField("provider")
    private String provider;
    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { this.orderId=value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { this.amount=value; }
    public String getReason() { return reason; }
    public void setReason(String value) { this.reason=value; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long value) { this.operatorId=value; }
    public String getProvider() { return provider; }
    public void setProvider(String value) { this.provider=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
}
