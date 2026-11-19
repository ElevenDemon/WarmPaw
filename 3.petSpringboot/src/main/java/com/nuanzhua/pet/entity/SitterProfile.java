package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.nuanzhua.pet.config.JsonTextTypeHandler;
import java.math.BigDecimal;
import java.time.*;

/** 喂养师档案实体，对应表 sitter_profile；金额单位元，时间使用带时区时间。 */
@TableName(value="sitter_profile",autoResultMap=true)
public class SitterProfile {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 关联统一账号。 */
    @TableField("user_id")
    private Long userId;
    /** 稳定业务代码。 */
    @TableField("code")
    private String code;
    /** 等级ID。 */
    @TableField("grade_id")
    private Long gradeId;
    /** 业务状态，枚举见本表CHECK约束。 */
    @TableField("status")
    private String status;
    /** 最近一次入驻审核意见，驳回后展示给申请人。 */
    @TableField("admission_review_note")
    private String admissionReviewNote;
    /** 紧急联系人。 */
    @TableField("emergency_contact")
    private String emergencyContact;
    /** 头像地址。 */
    @TableField("avatar_url")
    private String avatarUrl;
    /** 备注。 */
    @TableField("notes")
    private String notes;
    /** 行政区。 */
    @TableField("district")
    private String district;
    /** 详细地址。 */
    @TableField("address")
    private String address;
    /** 经度。 */
    @TableField("longitude")
    private BigDecimal longitude;
    /** 纬度。 */
    @TableField("latitude")
    private BigDecimal latitude;
    /** 服务半径0.5至50km。 */
    @TableField("radius_km")
    private BigDecimal radiusKm;
    /** 交通方式。 */
    @TableField("transport")
    private String transport;
    /** 结算方式。 */
    @TableField("settlement_method")
    private String settlementMethod;
    /** 擅长宠物类别JSON数组。 */
    @TableField(value="categories",typeHandler=JsonTextTypeHandler.class)
    private String categories;
    /** 综合评分。 */
    @TableField("rating")
    private BigDecimal rating;
    /** 已完成订单数。 */
    @TableField("completed_orders")
    private Integer completedOrders;
    /** 重大投诉数。 */
    @TableField("major_complaints")
    private Integer majorComplaints;
    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    /** 最后更新时间（由业务显式更新）。 */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { this.userId=value; }
    public String getCode() { return code; }
    public void setCode(String value) { this.code=value; }
    public Long getGradeId() { return gradeId; }
    public void setGradeId(Long value) { this.gradeId=value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status=value; }
    public String getAdmissionReviewNote() { return admissionReviewNote; }
    public void setAdmissionReviewNote(String value) { this.admissionReviewNote=value; }
    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String value) { this.emergencyContact=value; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String value) { this.avatarUrl=value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { this.notes=value; }
    public String getDistrict() { return district; }
    public void setDistrict(String value) { this.district=value; }
    public String getAddress() { return address; }
    public void setAddress(String value) { this.address=value; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal value) { this.longitude=value; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal value) { this.latitude=value; }
    public BigDecimal getRadiusKm() { return radiusKm; }
    public void setRadiusKm(BigDecimal value) { this.radiusKm=value; }
    public String getTransport() { return transport; }
    public void setTransport(String value) { this.transport=value; }
    public String getSettlementMethod() { return settlementMethod; }
    public void setSettlementMethod(String value) { this.settlementMethod=value; }
    public String getCategories() { return categories; }
    public void setCategories(String value) { this.categories=value; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal value) { this.rating=value; }
    public Integer getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(Integer value) { this.completedOrders=value; }
    public Integer getMajorComplaints() { return majorComplaints; }
    public void setMajorComplaints(Integer value) { this.majorComplaints=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime value) { this.updatedAt=value; }
}
