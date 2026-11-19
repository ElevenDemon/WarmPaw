package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.*;

/** 会员套餐实体，对应表 membership_plan；金额单位元，时间使用带时区时间。 */
@TableName("membership_plan")
public class MembershipPlan {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 名称。 */
    @TableField("name")
    private String name;
    /** 展示名称。 */
    @TableField("display_name")
    private String displayName;
    /** 套餐月数。 */
    @TableField("months")
    private Integer months;
    /** 单价，人民币元。 */
    @TableField("price")
    private BigDecimal price;
    /** 是否启用。 */
    @TableField("active")
    private Boolean active;
    /** 最后更新时间（由业务显式更新）。 */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
    /** 会员赠券模板ID。 */
    @TableField("gift_coupon_template_id")
    private Long giftCouponTemplateId;
    /** 赠券张数。 */
    @TableField("gift_coupon_quantity")
    private Integer giftCouponQuantity;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public String getName() { return name; }
    public void setName(String value) { this.name=value; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { this.displayName=value; }
    public Integer getMonths() { return months; }
    public void setMonths(Integer value) { this.months=value; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal value) { this.price=value; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean value) { this.active=value; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime value) { this.updatedAt=value; }
    public Long getGiftCouponTemplateId() { return giftCouponTemplateId; }
    public void setGiftCouponTemplateId(Long value) { this.giftCouponTemplateId=value; }
    public Integer getGiftCouponQuantity() { return giftCouponQuantity; }
    public void setGiftCouponQuantity(Integer value) { this.giftCouponQuantity=value; }
}
