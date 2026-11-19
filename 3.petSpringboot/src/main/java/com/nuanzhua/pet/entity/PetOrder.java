package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.nuanzhua.pet.config.JsonTextTypeHandler;
import java.math.BigDecimal;
import java.time.*;

/** 服务订单实体，对应表 pet_order；金额单位元，时间使用带时区时间。 */
@TableName(value="pet_order",autoResultMap=true)
public class PetOrder {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 业务订单号。 */
    @TableField("order_no")
    private String orderNo;
    /** 同宠主下单幂等键。 */
    @TableField("request_key")
    private String requestKey;
    /** 宠主账号ID。 */
    @TableField("owner_id")
    private Long ownerId;
    /** 喂养师档案ID。 */
    @TableField("sitter_id")
    private Long sitterId;
    /** 业务状态，枚举见本表CHECK约束。 */
    @TableField("status")
    private String status;
    /** 预约服务开始。 */
    @TableField("service_start")
    private OffsetDateTime serviceStart;
    /** 预约服务结束。 */
    @TableField("service_end")
    private OffsetDateTime serviceEnd;
    /** 下单地址JSON快照。 */
    @TableField(value="address_snapshot",typeHandler=JsonTextTypeHandler.class)
    private String addressSnapshot;
    /** 下单宠物JSON快照。 */
    @TableField(value="pet_snapshot",typeHandler=JsonTextTypeHandler.class)
    private String petSnapshot;
    /** 备注。 */
    @TableField("notes")
    private String notes;
    /** 原价总额。 */
    @TableField("total_amount")
    private BigDecimal totalAmount;
    /** 会员减免额。 */
    @TableField("member_discount")
    private BigDecimal memberDiscount;
    /** 优惠券减免额。 */
    @TableField("coupon_discount")
    private BigDecimal couponDiscount;
    /** 实际应付金额。 */
    @TableField("payable_amount")
    private BigDecimal payableAmount;
    /** 本次锁定或核销的用户券ID。 */
    @TableField("coupon_id")
    private Long couponId;
    /** 平台抽成百分数，18表示18%。 */
    @TableField("commission_rate")
    private BigDecimal commissionRate;
    /** 服务协议版本及授权范围JSON快照。 */
    @TableField(value="signed_agreement",typeHandler=JsonTextTypeHandler.class)
    private String signedAgreement;
    /** 人工派单原因JSON。 */
    @TableField(value="dispatch_reason",typeHandler=JsonTextTypeHandler.class)
    private String dispatchReason;
    /** 接单截止时间。 */
    @TableField("accept_deadline")
    private OffsetDateTime acceptDeadline;
    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    /** 支付时间。 */
    @TableField("paid_at")
    private OffsetDateTime paidAt;
    /** 进门打卡时间。 */
    @TableField("checkin_at")
    private OffsetDateTime checkinAt;
    /** 服务完成离门时间。 */
    @TableField("checkout_at")
    private OffsetDateTime checkoutAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String value) { this.orderNo=value; }
    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String value) { this.requestKey=value; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long value) { this.ownerId=value; }
    public Long getSitterId() { return sitterId; }
    public void setSitterId(Long value) { this.sitterId=value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status=value; }
    public OffsetDateTime getServiceStart() { return serviceStart; }
    public void setServiceStart(OffsetDateTime value) { this.serviceStart=value; }
    public OffsetDateTime getServiceEnd() { return serviceEnd; }
    public void setServiceEnd(OffsetDateTime value) { this.serviceEnd=value; }
    public String getAddressSnapshot() { return addressSnapshot; }
    public void setAddressSnapshot(String value) { this.addressSnapshot=value; }
    public String getPetSnapshot() { return petSnapshot; }
    public void setPetSnapshot(String value) { this.petSnapshot=value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { this.notes=value; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal value) { this.totalAmount=value; }
    public BigDecimal getMemberDiscount() { return memberDiscount; }
    public void setMemberDiscount(BigDecimal value) { this.memberDiscount=value; }
    public BigDecimal getCouponDiscount() { return couponDiscount; }
    public void setCouponDiscount(BigDecimal value) { this.couponDiscount=value; }
    public BigDecimal getPayableAmount() { return payableAmount; }
    public void setPayableAmount(BigDecimal value) { this.payableAmount=value; }
    public Long getCouponId() { return couponId; }
    public void setCouponId(Long value) { this.couponId=value; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal value) { this.commissionRate=value; }
    public String getSignedAgreement() { return signedAgreement; }
    public void setSignedAgreement(String value) { this.signedAgreement=value; }
    public String getDispatchReason() { return dispatchReason; }
    public void setDispatchReason(String value) { this.dispatchReason=value; }
    public OffsetDateTime getAcceptDeadline() { return acceptDeadline; }
    public void setAcceptDeadline(OffsetDateTime value) { this.acceptDeadline=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
    public OffsetDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(OffsetDateTime value) { this.paidAt=value; }
    public OffsetDateTime getCheckinAt() { return checkinAt; }
    public void setCheckinAt(OffsetDateTime value) { this.checkinAt=value; }
    public OffsetDateTime getCheckoutAt() { return checkoutAt; }
    public void setCheckoutAt(OffsetDateTime value) { this.checkoutAt=value; }
}
