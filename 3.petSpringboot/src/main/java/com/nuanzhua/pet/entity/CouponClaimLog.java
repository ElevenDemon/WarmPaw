package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.nuanzhua.pet.config.JsonTextTypeHandler;
import java.time.*;

/** 发券及领券校验日志实体，对应表 coupon_claim_log；金额单位元，时间使用带时区时间。 */
@TableName(value="coupon_claim_log",autoResultMap=true)
public class CouponClaimLog {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 券模板ID。 */
    @TableField("template_id")
    private Long templateId;
    /** 宠主账号ID。 */
    @TableField("owner_id")
    private Long ownerId;
    /** 来源claim主动领取或push后台直发。 */
    @TableField("source")
    private String source;
    /** 是否成功。 */
    @TableField("success")
    private Boolean success;
    /** 失败步骤1至6。 */
    @TableField("failed_step")
    private Integer failedStep;
    /** 业务说明或失败理由。 */
    @TableField("reason")
    private String reason;
    /** 校验链JSON，成功为7步。 */
    @TableField(value="chain",typeHandler=JsonTextTypeHandler.class)
    private String chain;
    /** 设备标识摘要。 */
    @TableField("device_hash")
    private String deviceHash;
    /** 手机号摘要。 */
    @TableField("phone_hash")
    private String phoneHash;
    /** 来源IP摘要。 */
    @TableField("ip_hash")
    private String ipHash;
    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long value) { this.templateId=value; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long value) { this.ownerId=value; }
    public String getSource() { return source; }
    public void setSource(String value) { this.source=value; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean value) { this.success=value; }
    public Integer getFailedStep() { return failedStep; }
    public void setFailedStep(Integer value) { this.failedStep=value; }
    public String getReason() { return reason; }
    public void setReason(String value) { this.reason=value; }
    public String getChain() { return chain; }
    public void setChain(String value) { this.chain=value; }
    public String getDeviceHash() { return deviceHash; }
    public void setDeviceHash(String value) { this.deviceHash=value; }
    public String getPhoneHash() { return phoneHash; }
    public void setPhoneHash(String value) { this.phoneHash=value; }
    public String getIpHash() { return ipHash; }
    public void setIpHash(String value) { this.ipHash=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
}
