package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.nuanzhua.pet.config.JsonTextTypeHandler;
import java.time.*;

/** 喂养报告与待处理原始数据实体，对应表 feeding_report；金额单位元，时间使用带时区时间。 */
@TableName(value="feeding_report",autoResultMap=true)
public class FeedingReport {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 服务订单ID。 */
    @TableField("order_id")
    private Long orderId;
    /** 业务状态，枚举见本表CHECK约束。 */
    @TableField("status")
    private String status;
    /** 检查清单、媒体等原始快照，供后续Python读取。 */
    @TableField(value="source_snapshot",typeHandler=JsonTextTypeHandler.class)
    private String sourceSnapshot;
    /** 文本内容。 */
    @TableField(value="content",typeHandler=JsonTextTypeHandler.class)
    private String content;
    /** 最后更新时间（由业务显式更新）。 */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { this.orderId=value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status=value; }
    public String getSourceSnapshot() { return sourceSnapshot; }
    public void setSourceSnapshot(String value) { this.sourceSnapshot=value; }
    public String getContent() { return content; }
    public void setContent(String value) { this.content=value; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime value) { this.updatedAt=value; }
}
