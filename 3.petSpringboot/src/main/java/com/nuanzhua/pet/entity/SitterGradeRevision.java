package com.nuanzhua.pet.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.nuanzhua.pet.config.JsonTextTypeHandler;
import java.time.*;

/** 等级规则待生效版本实体，对应表 sitter_grade_revision；金额单位元，时间使用带时区时间。 */
@TableName(value="sitter_grade_revision",autoResultMap=true)
public class SitterGradeRevision {
    /** 自增主键。 */
    @TableId(type=IdType.AUTO)
    private Long id;
    /** 等级ID。 */
    @TableField("grade_id")
    private Long gradeId;
    /** 待生效规则完整JSON。 */
    @TableField(value="rule_data",typeHandler=JsonTextTypeHandler.class)
    private String ruleData;
    /** 规则生效时间。 */
    @TableField("effective_at")
    private OffsetDateTime effectiveAt;
    /** 实际生效时间，空表示待执行。 */
    @TableField("applied_at")
    private OffsetDateTime appliedAt;
    /** 操作人账号ID。 */
    @TableField("operator_id")
    private Long operatorId;
    /** 创建时间。 */
    @TableField("created_at")
    private OffsetDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long value) { this.id=value; }
    public Long getGradeId() { return gradeId; }
    public void setGradeId(Long value) { this.gradeId=value; }
    public String getRuleData() { return ruleData; }
    public void setRuleData(String value) { this.ruleData=value; }
    public OffsetDateTime getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(OffsetDateTime value) { this.effectiveAt=value; }
    public OffsetDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(OffsetDateTime value) { this.appliedAt=value; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long value) { this.operatorId=value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime value) { this.createdAt=value; }
}
