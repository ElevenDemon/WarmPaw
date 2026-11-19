package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.nuanzhua.pet.config.JsonTextTypeHandler;
import java.time.*;

/** 订单事件流水实体，对应表 order_event；金额单位元，时间使用带时区时间。 */
@TableName(value="order_event",autoResultMap=true)
public class OrderEvent {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 服务订单ID。 */
    @TableField("order_id")
    private Long orderId;
    /** 操作人账号，定时任务可空。 */
    @TableField("actor_id")
    private Long actorId;
    /** 事件代码。 */
    @TableField("event")
    private String event;
    /** 事件详情JSON。 */
    @TableField(value="detail",typeHandler=JsonTextTypeHandler.class)
    private String detail;
    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { this.orderId=value; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long value) { this.actorId=value; }
    public String getEvent() { return event; }
    public void setEvent(String value) { this.event=value; }
    public String getDetail() { return detail; }
    public void setDetail(String value) { this.detail=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
}
