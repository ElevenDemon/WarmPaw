package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.*;

/** 会员权益实体，对应表 membership_benefit；金额单位元，时间使用带时区时间。 */
@TableName("membership_benefit")
public class MembershipBenefit {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 稳定业务代码。 */
    @TableField("code")
    private String code;
    /** 名称。 */
    @TableField("name")
    private String name;
    /** 开关。 */
    @TableField("enabled")
    private Boolean enabled;
    /** 优惠乘数，例如0.900为九折。 */
    @TableField("discount_rate")
    private BigDecimal discountRate;
    /** 最后更新时间（由业务显式更新）。 */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public String getCode() { return code; }
    public void setCode(String value) { this.code=value; }
    public String getName() { return name; }
    public void setName(String value) { this.name=value; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean value) { this.enabled=value; }
    public BigDecimal getDiscountRate() { return discountRate; }
    public void setDiscountRate(BigDecimal value) { this.discountRate=value; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime value) { this.updatedAt=value; }
}
