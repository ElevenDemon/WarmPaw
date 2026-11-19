package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.*;

/** 宠主地址实体，对应表 owner_address；金额单位元，时间使用带时区时间。 */
@TableName("owner_address")
public class OwnerAddress {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 宠主账号ID。 */
    @TableField("owner_id")
    private Long ownerId;
    /** 联系人。 */
    @TableField("contact_name")
    private String contactName;
    /** 手机号码。 */
    @TableField("phone")
    private String phone;
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
    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long value) { this.ownerId=value; }
    public String getContactName() { return contactName; }
    public void setContactName(String value) { this.contactName=value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { this.phone=value; }
    public String getDistrict() { return district; }
    public void setDistrict(String value) { this.district=value; }
    public String getAddress() { return address; }
    public void setAddress(String value) { this.address=value; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal value) { this.longitude=value; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal value) { this.latitude=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
}
