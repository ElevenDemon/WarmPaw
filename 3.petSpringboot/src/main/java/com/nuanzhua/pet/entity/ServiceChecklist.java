package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.*;

/** 服务检查清单实体，对应表 service_checklist；金额单位元，时间使用带时区时间。 */
@TableName("service_checklist")
public class ServiceChecklist {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 服务订单ID。 */
    @TableField("order_id")
    private Long orderId;
    /** 稳定业务代码。 */
    @TableField("code")
    private String code;
    /** 是否完成。 */
    @TableField("completed")
    private Boolean completed;
    /** 数量。 */
    @TableField("quantity")
    private BigDecimal quantity;
    /** 计量单位，陪玩必须MINUTE。 */
    @TableField("unit")
    private String unit;
    /** 备注。 */
    @TableField("notes")
    private String notes;
    /** 最后更新时间（由业务显式更新）。 */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { this.orderId=value; }
    public String getCode() { return code; }
    public void setCode(String value) { this.code=value; }
    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean value) { this.completed=value; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal value) { this.quantity=value; }
    public String getUnit() { return unit; }
    public void setUnit(String value) { this.unit=value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { this.notes=value; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime value) { this.updatedAt=value; }
}
