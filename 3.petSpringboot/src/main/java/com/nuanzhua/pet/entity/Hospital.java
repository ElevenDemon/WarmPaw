package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.*;

/** 合作医院实体，对应表 hospital；金额单位元，时间使用带时区时间。 */
@TableName("hospital")
public class Hospital {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 名称。 */
    @TableField("name")
    private String name;
    /** 手机号码。 */
    @TableField("phone")
    private String phone;
    /** 详细地址。 */
    @TableField("address")
    private String address;
    /** 经度。 */
    @TableField("longitude")
    private BigDecimal longitude;
    /** 纬度。 */
    @TableField("latitude")
    private BigDecimal latitude;
    /** 是否24小时急诊。 */
    @TableField("emergency_24h")
    private Boolean emergency_24h;
    /** 是否启用。 */
    @TableField("active")
    private Boolean active;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public String getName() { return name; }
    public void setName(String value) { this.name=value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { this.phone=value; }
    public String getAddress() { return address; }
    public void setAddress(String value) { this.address=value; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal value) { this.longitude=value; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal value) { this.latitude=value; }
    public Boolean getEmergency_24h() { return emergency_24h; }
    public void setEmergency_24h(Boolean value) { this.emergency_24h=value; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean value) { this.active=value; }
}
