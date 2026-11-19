package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;

/** 人工派单记录实体，对应表 dispatch_record；金额单位元，时间使用带时区时间。 */
@TableName("dispatch_record")
public class DispatchRecord {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 服务订单ID。 */
    @TableField("order_id")
    private Long orderId;
    /** 喂养师档案ID。 */
    @TableField("sitter_id")
    private Long sitterId;
    /** 操作人账号ID。 */
    @TableField("operator_id")
    private Long operatorId;
    /** 派单模式，当前MANUAL。 */
    @TableField("mode")
    private String mode;
    /** 业务说明或失败理由。 */
    @TableField("reason")
    private String reason;
    /** 业务状态，枚举见本表CHECK约束。 */
    @TableField("status")
    private String status;
    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    /** 响应时间。 */
    @TableField("responded_at")
    private OffsetDateTime respondedAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { this.orderId=value; }
    public Long getSitterId() { return sitterId; }
    public void setSitterId(Long value) { this.sitterId=value; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long value) { this.operatorId=value; }
    public String getMode() { return mode; }
    public void setMode(String value) { this.mode=value; }
    public String getReason() { return reason; }
    public void setReason(String value) { this.reason=value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
    public OffsetDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(OffsetDateTime value) { this.respondedAt=value; }
}
