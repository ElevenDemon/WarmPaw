package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.nuanzhua.pet.config.JsonTextTypeHandler;
import java.time.*;

/** 消息模板实体，对应表 notification_template；金额单位元，时间使用带时区时间。 */
@TableName(value="notification_template",autoResultMap=true)
public class NotificationTemplate {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 稳定业务代码。 */
    @TableField("code")
    private String code;
    /** 名称。 */
    @TableField("name")
    private String name;
    /** 开关。 */
    @TableField("enabled")
    private Boolean enabled;
    /** 配置投递渠道JSON，目前仅站内落地。 */
    @TableField(value="channels",typeHandler=JsonTextTypeHandler.class)
    private String channels;
    /** 最后更新时间（由业务显式更新）。 */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public String getCode() { return code; }
    public void setCode(String value) { this.code=value; }
    public String getName() { return name; }
    public void setName(String value) { this.name=value; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean value) { this.enabled=value; }
    public String getChannels() { return channels; }
    public void setChannels(String value) { this.channels=value; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime value) { this.updatedAt=value; }
}
