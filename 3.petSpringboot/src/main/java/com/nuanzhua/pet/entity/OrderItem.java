package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.*;

/** 订单服务明细快照实体，对应表 order_item；金额单位元，时间使用带时区时间。 */
@TableName("order_item")
public class OrderItem {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 服务订单ID。 */
    @TableField("order_id")
    private Long orderId;
    /** 价格配置ID。 */
    @TableField("service_id")
    private Long serviceId;
    /** 稳定业务代码。 */
    @TableField("code")
    private String code;
    /** 名称。 */
    @TableField("name")
    private String name;
    /** 下单时单价快照。 */
    @TableField("unit_price")
    private BigDecimal unitPrice;
    /** 数量。 */
    @TableField("quantity")
    private Integer quantity;
    /** 服务要求的资质类别。 */
    @TableField("required_category")
    private String requiredCategory;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { this.orderId=value; }
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long value) { this.serviceId=value; }
    public String getCode() { return code; }
    public void setCode(String value) { this.code=value; }
    public String getName() { return name; }
    public void setName(String value) { this.name=value; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal value) { this.unitPrice=value; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer value) { this.quantity=value; }
    public String getRequiredCategory() { return requiredCategory; }
    public void setRequiredCategory(String value) { this.requiredCategory=value; }
}
