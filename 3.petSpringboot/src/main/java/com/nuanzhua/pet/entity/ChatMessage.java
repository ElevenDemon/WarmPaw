package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.nuanzhua.pet.config.JsonTextTypeHandler;
import java.time.*;

/** 聊天消息实体，对应表 chat_message；金额单位元，时间使用带时区时间。 */
@TableName(value="chat_message",autoResultMap=true)
public class ChatMessage {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 客服会话ID。 */
    @TableField("conversation_id")
    private Long conversationId;
    /** 服务订单ID。 */
    @TableField("order_id")
    private Long orderId;
    /** 发送人账号。 */
    @TableField("sender_id")
    private Long senderId;
    /** 文本内容。 */
    @TableField("content")
    private String content;
    /** 媒体对象键JSON数组。 */
    @TableField(value="media_keys",typeHandler=JsonTextTypeHandler.class)
    private String mediaKeys;
    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long value) { this.conversationId=value; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { this.orderId=value; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long value) { this.senderId=value; }
    public String getContent() { return content; }
    public void setContent(String value) { this.content=value; }
    public String getMediaKeys() { return mediaKeys; }
    public void setMediaKeys(String value) { this.mediaKeys=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
}
