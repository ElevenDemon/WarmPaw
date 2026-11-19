package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;

/** 人工客服会话实体，对应表 support_conversation；金额单位元，时间使用带时区时间。 */
@TableName("support_conversation")
public class SupportConversation {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 宠主账号ID。 */
    @TableField("owner_id")
    private Long ownerId;
    /** 服务订单ID。 */
    @TableField("order_id")
    private Long orderId;
    /** 接管人工客服账号ID。 */
    @TableField("agent_id")
    private Long agentId;
    /** 业务状态，枚举见本表CHECK约束。 */
    @TableField("status")
    private String status;
    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long value) { this.ownerId=value; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { this.orderId=value; }
    public Long getAgentId() { return agentId; }
    public void setAgentId(Long value) { this.agentId=value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
}
