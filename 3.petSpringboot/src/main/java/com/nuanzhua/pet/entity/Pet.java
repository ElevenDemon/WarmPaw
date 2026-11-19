package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.*;

/** 宠物档案实体，对应表 pet；金额单位元，时间使用带时区时间。 */
@TableName("pet")
public class Pet {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 宠主账号ID。 */
    @TableField("owner_id")
    private Long ownerId;
    /** 名称。 */
    @TableField("name")
    private String name;
    /** 宠物或认证类别。 */
    @TableField("category")
    private String category;
    /** 品种。 */
    @TableField("breed")
    private String breed;
    /** 出生日期。 */
    @TableField("birthday")
    private LocalDate birthday;
    /** 体重kg。 */
    @TableField("weight")
    private BigDecimal weight;
    /** 性格与注意事项。 */
    @TableField("temperament")
    private String temperament;
    /** 饮食说明。 */
    @TableField("dietary_notes")
    private String dietaryNotes;
    /** 病史说明。 */
    @TableField("medical_history")
    private String medicalHistory;
    /** 照片地址。 */
    @TableField("photo_url")
    private String photoUrl;
    /** 软删除标志。 */
    @TableField("deleted")
    private Boolean deleted;
    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    /** 最后更新时间（由业务显式更新）。 */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long value) { this.ownerId=value; }
    public String getName() { return name; }
    public void setName(String value) { this.name=value; }
    public String getCategory() { return category; }
    public void setCategory(String value) { this.category=value; }
    public String getBreed() { return breed; }
    public void setBreed(String value) { this.breed=value; }
    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate value) { this.birthday=value; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal value) { this.weight=value; }
    public String getTemperament() { return temperament; }
    public void setTemperament(String value) { this.temperament=value; }
    public String getDietaryNotes() { return dietaryNotes; }
    public void setDietaryNotes(String value) { this.dietaryNotes=value; }
    public String getMedicalHistory() { return medicalHistory; }
    public void setMedicalHistory(String value) { this.medicalHistory=value; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String value) { this.photoUrl=value; }
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean value) { this.deleted=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime value) { this.updatedAt=value; }
}
