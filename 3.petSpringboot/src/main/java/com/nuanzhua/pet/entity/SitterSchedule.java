package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.*;

/** 每周七天档期实体，对应表 sitter_schedule；金额单位元，时间使用带时区时间。 */
@TableName("sitter_schedule")
public class SitterSchedule {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 喂养师档案ID。 */
    @TableField("sitter_id")
    private Long sitterId;
    /** 星期1至7，周一为1。 */
    @TableField("weekday")
    private Integer weekday;
    /** 是否休息。 */
    @TableField("rest")
    private Boolean rest;
    /** 当日接单开始时间。 */
    @TableField("start_time")
    private LocalTime startTime;
    /** 当日接单结束时间。 */
    @TableField("end_time")
    private LocalTime endTime;
    /** 当日容量0至20。 */
    @TableField("capacity")
    private Integer capacity;
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
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getSitterId() { return sitterId; }
    public void setSitterId(Long value) { this.sitterId=value; }
    public Integer getWeekday() { return weekday; }
    public void setWeekday(Integer value) { this.weekday=value; }
    public Boolean getRest() { return rest; }
    public void setRest(Boolean value) { this.rest=value; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime value) { this.startTime=value; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime value) { this.endTime=value; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer value) { this.capacity=value; }
    public String getDistrict() { return district; }
    public void setDistrict(String value) { this.district=value; }
    public String getAddress() { return address; }
    public void setAddress(String value) { this.address=value; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal value) { this.longitude=value; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal value) { this.latitude=value; }
}
