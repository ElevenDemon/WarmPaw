package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.*;

/** 服务价格配置实体，对应表 service_item；金额单位元，时间使用带时区时间。 */
@TableName("service_item")
public class ServiceItem {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 稳定业务代码。 */
    @TableField("code")
    private String code;
    /** 名称。 */
    @TableField("name")
    private String name;
    /** 说明。 */
    @TableField("description")
    private String description;
    /** 单价，人民币元。 */
    @TableField("price")
    private BigDecimal price;
    /** 服务要求的资质类别。 */
    @TableField("required_category")
    private String requiredCategory;
    /** 是否启用。 */
    @TableField("active")
    private Boolean active;
    /** 最后更新时间（由业务显式更新）。 */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public String getCode() { return code; }
    public void setCode(String value) { this.code=value; }
    public String getName() { return name; }
    public void setName(String value) { this.name=value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { this.description=value; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal value) { this.price=value; }
    public String getRequiredCategory() { return requiredCategory; }
    public void setRequiredCategory(String value) { this.requiredCategory=value; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean value) { this.active=value; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime value) { this.updatedAt=value; }
}
