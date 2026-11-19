package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.nuanzhua.pet.config.JsonTextTypeHandler;
import java.time.*;

/** 喂养师分类资质实体，对应表 sitter_qualification；金额单位元，时间使用带时区时间。 */
@TableName(value="sitter_qualification",autoResultMap=true)
public class SitterQualification {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 喂养师档案ID。 */
    @TableField("sitter_id")
    private Long sitterId;
    /** 宠物或认证类别。 */
    @TableField("category")
    private String category;
    /** 实名认证是否通过。 */
    @TableField("real_verified")
    private Boolean realVerified;
    /** 培训是否通过。 */
    @TableField("training_passed")
    private Boolean trainingPassed;
    /** 实操考核是否通过。 */
    @TableField("practice_passed")
    private Boolean practicePassed;
    /** 业务状态，枚举见本表CHECK约束。 */
    @TableField("status")
    private String status;
    /** 到期时间。 */
    @TableField("expires_at")
    private OffsetDateTime expiresAt;
    /** 证明材料对象键JSON数组。 */
    @TableField(value="materials",typeHandler=JsonTextTypeHandler.class)
    private String materials;
    /** 审核人账号ID。 */
    @TableField("reviewer_id")
    private Long reviewerId;
    /** 审核意见。 */
    @TableField("review_note")
    private String reviewNote;
    /** 最后更新时间（由业务显式更新）。 */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getSitterId() { return sitterId; }
    public void setSitterId(Long value) { this.sitterId=value; }
    public String getCategory() { return category; }
    public void setCategory(String value) { this.category=value; }
    public Boolean getRealVerified() { return realVerified; }
    public void setRealVerified(Boolean value) { this.realVerified=value; }
    public Boolean getTrainingPassed() { return trainingPassed; }
    public void setTrainingPassed(Boolean value) { this.trainingPassed=value; }
    public Boolean getPracticePassed() { return practicePassed; }
    public void setPracticePassed(Boolean value) { this.practicePassed=value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status=value; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime value) { this.expiresAt=value; }
    public String getMaterials() { return materials; }
    public void setMaterials(String value) { this.materials=value; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long value) { this.reviewerId=value; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String value) { this.reviewNote=value; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime value) { this.updatedAt=value; }
}
