package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.*;

/** 服务媒体元数据实体，对应表 service_media；金额单位元，时间使用带时区时间。 */
@TableName("service_media")
public class ServiceMedia {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 服务订单ID。 */
    @TableField("order_id")
    private Long orderId;
    /** 上传者账号ID。 */
    @TableField("uploader_id")
    private Long uploaderId;
    /** 媒体类型PHOTO/VIDEO/LIVE。 */
    @TableField("kind")
    private String kind;
    /** 相对存储对象键，不是公网播放地址。 */
    @TableField("object_key")
    private String objectKey;
    /** 到期时间。 */
    @TableField("expires_at")
    private OffsetDateTime expiresAt;
    /** 软删除标志。 */
    @TableField("deleted")
    private Boolean deleted;
    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { this.orderId=value; }
    public Long getUploaderId() { return uploaderId; }
    public void setUploaderId(Long value) { this.uploaderId=value; }
    public String getKind() { return kind; }
    public void setKind(String value) { this.kind=value; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String value) { this.objectKey=value; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime value) { this.expiresAt=value; }
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean value) { this.deleted=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
}
