package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.nuanzhua.pet.config.JsonTextTypeHandler;
import java.math.BigDecimal;
import java.time.*;

/** 优惠券模板实体，对应表 coupon_template；金额单位元，时间使用带时区时间。 */
@TableName(value="coupon_template",autoResultMap=true)
public class CouponTemplate {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 名称。 */
    @TableField("name")
    private String name;
    /** push直发或claim主动领取。 */
    @TableField("issue_type")
    private String issueType;
    /** 金额，人民币元。 */
    @TableField("amount")
    private BigDecimal amount;
    /** 最低消费门槛，元。 */
    @TableField("threshold")
    private BigDecimal threshold;
    /** 可用服务代码JSON数组，空表示不限。 */
    @TableField(value="service_codes",typeHandler=JsonTextTypeHandler.class)
    private String serviceCodes;
    /** 有效天数。 */
    @TableField("valid_days")
    private Integer validDays;
    /** 是否启用。 */
    @TableField("active")
    private Boolean active;
    /** 适用人群ALL/NEW/NORMAL/MEMBER/SMALL。 */
    @TableField("audience")
    private String audience;
    /** 每人主动领取上限。 */
    @TableField("per_user_limit")
    private Integer perUserLimit;
    /** 主动领取剩余库存。 */
    @TableField("stock")
    private Integer stock;
    /** 累计发放数量。 */
    @TableField("issued")
    private Integer issued;
    /** 领取开始时间。 */
    @TableField("claim_starts_at")
    private OffsetDateTime claimStartsAt;
    /** 领取截止时间。 */
    @TableField("claim_ends_at")
    private OffsetDateTime claimEndsAt;
    /** 兑换码，唯一。 */
    @TableField("redeem_code")
    private String redeemCode;
    /** 同设备限制开关。 */
    @TableField("limit_device")
    private Boolean limitDevice;
    /** 同手机号限制开关。 */
    @TableField("limit_phone")
    private Boolean limitPhone;
    /** 同IP限制开关。 */
    @TableField("limit_ip")
    private Boolean limitIp;
    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public String getName() { return name; }
    public void setName(String value) { this.name=value; }
    public String getIssueType() { return issueType; }
    public void setIssueType(String value) { this.issueType=value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { this.amount=value; }
    public BigDecimal getThreshold() { return threshold; }
    public void setThreshold(BigDecimal value) { this.threshold=value; }
    public String getServiceCodes() { return serviceCodes; }
    public void setServiceCodes(String value) { this.serviceCodes=value; }
    public Integer getValidDays() { return validDays; }
    public void setValidDays(Integer value) { this.validDays=value; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean value) { this.active=value; }
    public String getAudience() { return audience; }
    public void setAudience(String value) { this.audience=value; }
    public Integer getPerUserLimit() { return perUserLimit; }
    public void setPerUserLimit(Integer value) { this.perUserLimit=value; }
    public Integer getStock() { return stock; }
    public void setStock(Integer value) { this.stock=value; }
    public Integer getIssued() { return issued; }
    public void setIssued(Integer value) { this.issued=value; }
    public OffsetDateTime getClaimStartsAt() { return claimStartsAt; }
    public void setClaimStartsAt(OffsetDateTime value) { this.claimStartsAt=value; }
    public OffsetDateTime getClaimEndsAt() { return claimEndsAt; }
    public void setClaimEndsAt(OffsetDateTime value) { this.claimEndsAt=value; }
    public String getRedeemCode() { return redeemCode; }
    public void setRedeemCode(String value) { this.redeemCode=value; }
    public Boolean getLimitDevice() { return limitDevice; }
    public void setLimitDevice(Boolean value) { this.limitDevice=value; }
    public Boolean getLimitPhone() { return limitPhone; }
    public void setLimitPhone(Boolean value) { this.limitPhone=value; }
    public Boolean getLimitIp() { return limitIp; }
    public void setLimitIp(Boolean value) { this.limitIp=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
}
