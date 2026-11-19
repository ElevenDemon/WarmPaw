package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.*;

/** 当前等级规则实体，对应表 sitter_grade；金额单位元，时间使用带时区时间。 */
@TableName("sitter_grade")
public class SitterGrade {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 稳定业务代码。 */
    @TableField("code")
    private String code;
    /** 名称。 */
    @TableField("name")
    private String name;
    /** 等级最低完成单量。 */
    @TableField("min_orders")
    private Integer minOrders;
    /** 等级最低平均评分。 */
    @TableField("min_rating")
    private BigDecimal minRating;
    /** 最大重大投诉数。 */
    @TableField("max_complaints")
    private Integer maxComplaints;
    /** 平台抽成百分数，18表示18%。 */
    @TableField("commission_rate")
    private BigDecimal commissionRate;
    /** 预留派单权重，不执行AI计算。 */
    @TableField("dispatch_weight")
    private BigDecimal dispatchWeight;
    /** 能否承接遛狗/用药/驱虫高风险服务。 */
    @TableField("high_risk_enabled")
    private Boolean highRiskEnabled;
    /** 是否启用。 */
    @TableField("active")
    private Boolean active;
    /** 规则生效时间。 */
    @TableField("effective_at")
    private OffsetDateTime effectiveAt;
    /** 最后更新时间（由业务显式更新）。 */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public String getCode() { return code; }
    public void setCode(String value) { this.code=value; }
    public String getName() { return name; }
    public void setName(String value) { this.name=value; }
    public Integer getMinOrders() { return minOrders; }
    public void setMinOrders(Integer value) { this.minOrders=value; }
    public BigDecimal getMinRating() { return minRating; }
    public void setMinRating(BigDecimal value) { this.minRating=value; }
    public Integer getMaxComplaints() { return maxComplaints; }
    public void setMaxComplaints(Integer value) { this.maxComplaints=value; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal value) { this.commissionRate=value; }
    public BigDecimal getDispatchWeight() { return dispatchWeight; }
    public void setDispatchWeight(BigDecimal value) { this.dispatchWeight=value; }
    public Boolean getHighRiskEnabled() { return highRiskEnabled; }
    public void setHighRiskEnabled(Boolean value) { this.highRiskEnabled=value; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean value) { this.active=value; }
    public OffsetDateTime getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(OffsetDateTime value) { this.effectiveAt=value; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime value) { this.updatedAt=value; }
}
