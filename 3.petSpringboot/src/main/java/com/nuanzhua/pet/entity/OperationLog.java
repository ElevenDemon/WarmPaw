package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.nuanzhua.pet.config.JsonTextTypeHandler;
import java.time.*;

/** 后台操作审计实体，对应表 operation_log；金额单位元，时间使用带时区时间。 */
@TableName(value="operation_log",autoResultMap=true)
public class OperationLog {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 操作人账号，定时任务可空。 */
    @TableField("actor_id")
    private Long actorId;
    /** 操作名称。 */
    @TableField("action")
    private String action;
    /** 业务资源类型。 */
    @TableField("resource")
    private String resource;
    /** 业务资源ID。 */
    @TableField("resource_id")
    private Long resourceId;
    /** 变更前JSON快照。 */
    @TableField(value="before_data",typeHandler=JsonTextTypeHandler.class)
    private String beforeData;
    /** 变更后JSON快照。 */
    @TableField(value="after_data",typeHandler=JsonTextTypeHandler.class)
    private String afterData;
    /** 结果JSON。 */
    @TableField("result")
    private String result;
    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long value) { this.actorId=value; }
    public String getAction() { return action; }
    public void setAction(String value) { this.action=value; }
    public String getResource() { return resource; }
    public void setResource(String value) { this.resource=value; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long value) { this.resourceId=value; }
    public String getBeforeData() { return beforeData; }
    public void setBeforeData(String value) { this.beforeData=value; }
    public String getAfterData() { return afterData; }
    public void setAfterData(String value) { this.afterData=value; }
    public String getResult() { return result; }
    public void setResult(String value) { this.result=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
}
