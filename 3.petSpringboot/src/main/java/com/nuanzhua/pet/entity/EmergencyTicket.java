package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;

/** 紧急送医工单实体，对应表 emergency_ticket；金额单位元，时间使用带时区时间。 */
@TableName("emergency_ticket")
public class EmergencyTicket {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 服务订单ID。 */
    @TableField("order_id")
    private Long orderId;
    /** 上报人账号ID。 */
    @TableField("reporter_id")
    private Long reporterId;
    /** 说明。 */
    @TableField("description")
    private String description;
    /** 业务状态，枚举见本表CHECK约束。 */
    @TableField("status")
    private String status;
    /** 喂养师档案ID。 */
    @TableField("sitter_id")
    private Long sitterId;
    /** 合作医院ID。 */
    @TableField("hospital_id")
    private Long hospitalId;
    /** 宠主是否明确同意送医。 */
    @TableField("owner_consent")
    private Boolean ownerConsent;
    /** 响应截止时间。 */
    @TableField("response_due_at")
    private OffsetDateTime responseDueAt;
    /** 处理结果。 */
    @TableField("resolution")
    private String resolution;
    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    /** 最后更新时间（由业务显式更新）。 */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { this.orderId=value; }
    public Long getReporterId() { return reporterId; }
    public void setReporterId(Long value) { this.reporterId=value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { this.description=value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status=value; }
    public Long getSitterId() { return sitterId; }
    public void setSitterId(Long value) { this.sitterId=value; }
    public Long getHospitalId() { return hospitalId; }
    public void setHospitalId(Long value) { this.hospitalId=value; }
    public Boolean getOwnerConsent() { return ownerConsent; }
    public void setOwnerConsent(Boolean value) { this.ownerConsent=value; }
    public OffsetDateTime getResponseDueAt() { return responseDueAt; }
    public void setResponseDueAt(OffsetDateTime value) { this.responseDueAt=value; }
    public String getResolution() { return resolution; }
    public void setResolution(String value) { this.resolution=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime value) { this.updatedAt=value; }
}
