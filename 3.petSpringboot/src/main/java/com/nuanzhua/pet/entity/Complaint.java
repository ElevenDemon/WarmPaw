package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;

/** 投诉工单实体，对应表 complaint；金额单位元，时间使用带时区时间。 */
@TableName("complaint")
public class Complaint {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 服务订单ID。 */
    @TableField("order_id")
    private Long orderId;
    /** 宠主账号ID。 */
    @TableField("owner_id")
    private Long ownerId;
    /** 文本内容。 */
    @TableField("content")
    private String content;
    /** 业务状态，枚举见本表CHECK约束。 */
    @TableField("status")
    private String status;
    /** 响应截止时间。 */
    @TableField("response_due_at")
    private OffsetDateTime responseDueAt;
    /** 处理结果。 */
    @TableField("resolution")
    private String resolution;
    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { this.orderId=value; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long value) { this.ownerId=value; }
    public String getContent() { return content; }
    public void setContent(String value) { this.content=value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status=value; }
    public OffsetDateTime getResponseDueAt() { return responseDueAt; }
    public void setResponseDueAt(OffsetDateTime value) { this.responseDueAt=value; }
    public String getResolution() { return resolution; }
    public void setResolution(String value) { this.resolution=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
}
