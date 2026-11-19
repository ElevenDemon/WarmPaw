package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.*;

/** 支付记录实体，对应表 payment_record；金额单位元，时间使用带时区时间。 */
@TableName("payment_record")
public class PaymentRecord {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 服务订单ID。 */
    @TableField("order_id")
    private Long orderId;
    /** 会员购买单ID。 */
    @TableField("purchase_id")
    private Long purchaseId;
    /** 交易流水号。 */
    @TableField("trade_no")
    private String tradeNo;
    /** 支付渠道，当前DEMO。 */
    @TableField("provider")
    private String provider;
    /** 金额，人民币元。 */
    @TableField("amount")
    private BigDecimal amount;
    /** 业务状态，枚举见本表CHECK约束。 */
    @TableField("status")
    private String status;
    /** 支付时间。 */
    @TableField("paid_at")
    private OffsetDateTime paidAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { this.orderId=value; }
    public Long getPurchaseId() { return purchaseId; }
    public void setPurchaseId(Long value) { this.purchaseId=value; }
    public String getTradeNo() { return tradeNo; }
    public void setTradeNo(String value) { this.tradeNo=value; }
    public String getProvider() { return provider; }
    public void setProvider(String value) { this.provider=value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { this.amount=value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status=value; }
    public OffsetDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(OffsetDateTime value) { this.paidAt=value; }
}
