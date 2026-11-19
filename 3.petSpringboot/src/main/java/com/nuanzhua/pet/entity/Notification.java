package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;

/** 站内通知实体，对应表 notification；金额单位元，时间使用带时区时间。 */
@TableName("notification")
public class Notification {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 关联统一账号。 */
    @TableField("user_id")
    private Long userId;
    /** 服务订单ID。 */
    @TableField("order_id")
    private Long orderId;
    /** 稳定业务代码。 */
    @TableField("code")
    private String code;
    /** 标题。 */
    @TableField("title")
    private String title;
    /** 文本内容。 */
    @TableField("content")
    private String content;
    /** 已读时间，空为未读。 */
    @TableField("read_at")
    private OffsetDateTime readAt;
    /** 站内投递状态。 */
    @TableField("delivery_status")
    private String deliveryStatus;
    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { this.userId=value; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { this.orderId=value; }
    public String getCode() { return code; }
    public void setCode(String value) { this.code=value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { this.title=value; }
    public String getContent() { return content; }
    public void setContent(String value) { this.content=value; }
    public OffsetDateTime getReadAt() { return readAt; }
    public void setReadAt(OffsetDateTime value) { this.readAt=value; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String value) { this.deliveryStatus=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
}
