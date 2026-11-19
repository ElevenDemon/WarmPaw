package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.nuanzhua.pet.config.JsonTextTypeHandler;
import java.time.*;

/** 订单评价实体，对应表 review；金额单位元，时间使用带时区时间。 */
@TableName(value="review",autoResultMap=true)
public class Review {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 服务订单ID。 */
    @TableField("order_id")
    private Long orderId;
    /** 宠主账号ID。 */
    @TableField("owner_id")
    private Long ownerId;
    /** 喂养师档案ID。 */
    @TableField("sitter_id")
    private Long sitterId;
    /** 评分1至5。 */
    @TableField("score")
    private Integer score;
    /** 评价标签JSON数组。 */
    @TableField(value="tags",typeHandler=JsonTextTypeHandler.class)
    private String tags;
    /** 文本内容。 */
    @TableField("content")
    private String content;
    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { this.orderId=value; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long value) { this.ownerId=value; }
    public Long getSitterId() { return sitterId; }
    public void setSitterId(Long value) { this.sitterId=value; }
    public Integer getScore() { return score; }
    public void setScore(Integer value) { this.score=value; }
    public String getTags() { return tags; }
    public void setTags(String value) { this.tags=value; }
    public String getContent() { return content; }
    public void setContent(String value) { this.content=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
}
