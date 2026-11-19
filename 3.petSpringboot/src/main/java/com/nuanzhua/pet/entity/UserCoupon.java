package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.nuanzhua.pet.config.JsonTextTypeHandler;
import java.math.BigDecimal;
import java.time.*;

/** 宠主持券快照实体，对应表 user_coupon；金额单位元，时间使用带时区时间。 */
@TableName(value="user_coupon",autoResultMap=true)
public class UserCoupon {
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
    /** 金额，人民币元。 */
    @TableField("amount")
    private BigDecimal amount;
    /** 最低消费门槛，元。 */
    @TableField("threshold")
    private BigDecimal threshold;
    /** 可用服务代码JSON数组，空表示不限。 */
    @TableField(value="service_codes",typeHandler=JsonTextTypeHandler.class)
    private String serviceCodes;
    /** 业务状态，枚举见本表CHECK约束。 */
    @TableField("status")
    private String status;
    /** 到期时间。 */
    @TableField("expires_at")
    private OffsetDateTime expiresAt;
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
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { this.amount=value; }
    public BigDecimal getThreshold() { return threshold; }
    public void setThreshold(BigDecimal value) { this.threshold=value; }
    public String getServiceCodes() { return serviceCodes; }
    public void setServiceCodes(String value) { this.serviceCodes=value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status=value; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime value) { this.expiresAt=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
}
