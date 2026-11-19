package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.*;

/** 会员购买单实体，对应表 membership_purchase；金额单位元，时间使用带时区时间。 */
@TableName("membership_purchase")
public class MembershipPurchase {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 宠主账号ID。 */
    @TableField("owner_id")
    private Long ownerId;
    /** 会员套餐ID。 */
    @TableField("plan_id")
    private Long planId;
    /** 购买时套餐名快照。 */
    @TableField("plan_name")
    private String planName;
    /** 套餐月数。 */
    @TableField("months")
    private Integer months;
    /** 金额，人民币元。 */
    @TableField("amount")
    private BigDecimal amount;
    /** 业务状态，枚举见本表CHECK约束。 */
    @TableField("status")
    private String status;
    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    /** 支付时间。 */
    @TableField("paid_at")
    private OffsetDateTime paidAt;
    /** 会员赠券模板ID。 */
    @TableField("gift_coupon_template_id")
    private Long giftCouponTemplateId;
    /** 赠券张数。 */
    @TableField("gift_coupon_quantity")
    private Integer giftCouponQuantity;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long value) { this.ownerId=value; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long value) { this.planId=value; }
    public String getPlanName() { return planName; }
    public void setPlanName(String value) { this.planName=value; }
    public Integer getMonths() { return months; }
    public void setMonths(Integer value) { this.months=value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { this.amount=value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
    public OffsetDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(OffsetDateTime value) { this.paidAt=value; }
    public Long getGiftCouponTemplateId() { return giftCouponTemplateId; }
    public void setGiftCouponTemplateId(Long value) { this.giftCouponTemplateId=value; }
    public Integer getGiftCouponQuantity() { return giftCouponQuantity; }
    public void setGiftCouponQuantity(Integer value) { this.giftCouponQuantity=value; }
}
