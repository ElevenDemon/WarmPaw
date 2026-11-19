package com.nuanzhua.pet.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

/** 请求参数模型；字段约束由Bean Validation在接口入口校验。 */
public final class ConfigRequests {
    private ConfigRequests() {}
    /** Price请求参数；各字段的含义和约束见Swagger。 */
    public record Price(@NotBlank @Size(max=60) @Schema(description="名称") String name,@Size(max=500) @Schema(description="说明") String description,
        @NotNull @DecimalMin("0") @DecimalMax("9999") @Digits(integer=4,fraction=2) @Schema(description="单价，人民币元") BigDecimal price,@Schema(description="是否启用") boolean active) {}
    /** Plan请求参数；各字段的含义和约束见Swagger。 */
    public record Plan(@NotBlank @Size(max=60) @Schema(description="名称") String name,@NotBlank @Size(max=60) @Schema(description="展示名称") String displayName,
        @Min(1) @Max(120) @Schema(description="套餐月数") int months,@NotNull @DecimalMin("0.01") @DecimalMax("9999") @Digits(integer=4,fraction=2) @Schema(description="单价，人民币元") BigDecimal price,@Schema(description="是否启用") boolean active) {}
    /** Benefit请求参数；各字段的含义和约束见Swagger。 */
    public record Benefit(@Schema(description="开关") boolean enabled,@NotNull @DecimalMin("0.001") @DecimalMax("1") @Digits(integer=1,fraction=3) @Schema(description="优惠乘数，例如0.900为九折") BigDecimal discountRate) {}
    /** Grade请求参数；各字段的含义和约束见Swagger。 */
    public record Grade(@NotBlank @Pattern(regexp="L[1-9][0-9]?") @Schema(description="稳定业务代码") String code,@NotBlank @Size(max=32) @Schema(description="名称") String name,
        @Min(0) @Schema(description="等级最低完成单量") int minOrders,@NotNull @DecimalMin("0") @DecimalMax("5") @Schema(description="等级最低平均评分") BigDecimal minRating,@Min(0) @Schema(description="最大重大投诉数") int maxComplaints,
        @NotNull @DecimalMin("0") @DecimalMax("50") @Digits(integer=2,fraction=2) @Schema(description="平台抽成百分数，18表示18%") BigDecimal commissionRate,
        @NotNull @DecimalMin("0") @DecimalMax("100") @Schema(description="预留派单权重，不执行AI计算") BigDecimal dispatchWeight,@Schema(description="能否承接遛狗/用药/驱虫高风险服务") boolean highRiskEnabled,@Schema(description="是否启用") boolean active,
        @Schema(description="规则生效时间") OffsetDateTime effectiveAt) {}
    /** Coupon请求参数；各字段的含义和约束见Swagger。 */
    public record Coupon(@NotBlank @Size(max=60) @Schema(description="名称") String name,@NotNull @Pattern(regexp="push|claim") @Schema(description="push直发或claim主动领取") String issueType,
        @NotNull @DecimalMin("0.01") @DecimalMax("9999") @Digits(integer=4,fraction=2) @Schema(description="金额，人民币元") BigDecimal amount,
        @NotNull @DecimalMin("0") @DecimalMax("9999") @Digits(integer=4,fraction=2) @Schema(description="最低消费门槛，元") BigDecimal threshold,
        @NotNull @Size(max=30) @Schema(description="可用服务代码JSON数组，空表示不限") List<@Pattern(regexp="[A-Z_]{1,32}") String> serviceCodes,
        @Min(1) @Max(3650) @Schema(description="有效天数") int validDays,@Schema(description="是否启用") boolean active,@NotNull @Pattern(regexp="ALL|NEW|NORMAL|MEMBER|SMALL") @Schema(description="适用人群ALL/NEW/NORMAL/MEMBER/SMALL") String audience,
        @Min(1) @Max(100) @Schema(description="每人主动领取上限") int perUserLimit,@Min(0) @Schema(description="主动领取剩余库存") int stock,@NotNull @Schema(description="领取开始时间") OffsetDateTime claimStartsAt,@NotNull @Schema(description="领取截止时间") OffsetDateTime claimEndsAt,
        @Size(max=32) @Schema(description="兑换码，唯一") String redeemCode,@Schema(description="同设备限制开关") boolean limitDevice,@Schema(description="同手机号限制开关") boolean limitPhone,@Schema(description="同IP限制开关") boolean limitIp) {}
    /** Notice请求参数；各字段的含义和约束见Swagger。 */
    public record Notice(@Schema(description="开关") boolean enabled,@NotEmpty @Size(max=4) @Schema(description="配置投递渠道JSON，目前仅站内落地") List<@Pattern(regexp="IN_APP|WECHAT|SMS|GROUP") String> channels) {}
    /** Status请求参数；各字段的含义和约束见Swagger。 */
    public record Status(@NotNull @Pattern(regexp="ACTIVE|BLOCKED") @Schema(description="业务状态，枚举见本表CHECK约束") String status) {}
    /** MemberStatus请求参数；各字段的含义和约束见Swagger。 */
    public record MemberStatus(@NotNull @Pattern(regexp="ACTIVE|DISABLED") @Schema(description="业务状态，枚举见本表CHECK约束") String status) {}
    /** Permissions请求参数；各字段的含义和约束见Swagger。 */
    public record Permissions(@NotEmpty @Size(max=100) @Schema(description="受控业务权限集合") Set<@Pattern(regexp="[a-z]+:(read|update|settle|refund|risk|rule)") String> permissions) {}
    /** Staff请求参数；各字段的含义和约束见Swagger。 */
    public record Staff(@NotBlank @Pattern(regexp="[a-zA-Z0-9_]{4,32}") @Schema(description="登录账号") String username,
        @NotBlank @Size(min=8,max=64) @Schema(description="password") String password,@NotBlank @Size(max=32) @Schema(description="名称") String name,
        @NotNull @Pattern(regexp="SUPPORT|FINANCE") @Schema(description="账号角色") String role) {}
    /** Batch请求参数；各字段的含义和约束见Swagger。 */
    public record Batch(@NotEmpty @Size(max=100) @Schema(description="业务记录ID列表") List<@NotNull @Positive Long> ids) {}
}
