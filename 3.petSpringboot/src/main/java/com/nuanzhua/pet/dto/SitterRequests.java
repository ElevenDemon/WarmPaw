package com.nuanzhua.pet.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** 请求参数模型；字段约束由Bean Validation在接口入口校验。 */
public final class SitterRequests {
    private SitterRequests() {}
    /** 入驻申请审核决定；分类资质仍由独立资质审核流程处理。 */
    public enum AdmissionDecision { APPROVE, REJECT }
    public record AdmissionAudit(@NotNull @Schema(description="审核决定：通过或驳回") AdmissionDecision decision,
        @Size(max=1000) @Schema(description="审核意见；驳回时必填，说明需补充或修改的内容") String note) {}
    /** Personal请求参数；各字段的含义和约束见Swagger。 */
    public record Personal(@NotBlank @Size(max=12) @Schema(description="名称") String name,
        @NotNull @Pattern(regexp="1[3-9]\\d{9}") @Schema(description="手机号码") String phone,@Size(max=80) @Schema(description="紧急联系人") String emergencyContact,
        @Size(max=1000) @Schema(description="头像地址") String avatarUrl,@Size(max=60) @Schema(description="备注") String notes,
        @Size(max=1000) @Schema(description="服务经验与偏好，例如可照护大型犬、怕生或性格较凶的宠物；仅用于推荐，不替代资质审核") String servicePreferences,
        @NotBlank @Size(max=60) @Schema(description="行政区") String district,@NotBlank @Size(max=300) @Schema(description="详细地址") String address,
        @NotNull @DecimalMin("113.7") @DecimalMax("114.7") @Schema(description="经度") BigDecimal longitude,
        @NotNull @DecimalMin("22.4") @DecimalMax("22.9") @Schema(description="纬度") BigDecimal latitude,
        @NotNull @DecimalMin("0.5") @DecimalMax("50") @Schema(description="服务半径0.5至50km") BigDecimal radiusKm,
        @NotBlank @Size(max=30) @Schema(description="交通方式") String transport) {}
    /** Day请求参数；各字段的含义和约束见Swagger。 */
    public record Day(@Min(1) @Max(7) @Schema(description="星期1至7，周一为1") int weekday,@Schema(description="是否休息") boolean rest,@NotNull @Schema(description="当日接单开始时间") LocalTime startTime,@NotNull @Schema(description="当日接单结束时间") LocalTime endTime,
        @Min(0) @Max(20) @Schema(description="当日容量0至20") int capacity,@Size(max=60) @Schema(description="行政区") String district,@Size(max=300) @Schema(description="详细地址") String address,
        @DecimalMin("113.7") @DecimalMax("114.7") @Schema(description="经度") BigDecimal longitude,
        @DecimalMin("22.4") @DecimalMax("22.9") @Schema(description="纬度") BigDecimal latitude) {}
    /** Profile请求参数；各字段的含义和约束见Swagger。 */
    public record Profile(@NotNull @Valid Personal personal,@NotNull @Positive @Schema(description="等级ID") Long gradeId,
        @NotNull @Pattern(regexp="PENDING|ACTIVE|REST|DISABLED|REJECTED") @Schema(description="业务状态，枚举见本表CHECK约束") String status,
        @NotBlank @Size(max=30) @Schema(description="结算方式") String settlementMethod,
        @NotEmpty @Size(max=4) @Schema(description="擅长宠物类别JSON数组") Set<@Pattern(regexp="CAT|DOG|SMALL|OTHER") String> categories,
        @NotNull @Size(min=7,max=7) @Schema(description="七天档期，星期1至7必须完整且不重复") List<@NotNull @Valid Day> schedule) {}
    /** Application请求参数；各字段的含义和约束见Swagger。 */
    public record Application(@NotNull @Valid Personal personal,
        @NotEmpty @Size(max=4) @Schema(description="擅长宠物类别JSON数组") Set<@Pattern(regexp="CAT|DOG|SMALL|OTHER") String> categories) {}
    /** Qualification请求参数；各字段的含义和约束见Swagger。 */
    public record Qualification(@NotNull @Pattern(regexp="CAT|DOG|SMALL|OTHER|WALK|MEDICINE|DEWORM") @Schema(description="宠物或认证类别") String category,
        @NotEmpty @Size(max=20) @Schema(description="证明材料对象键JSON数组") List<@NotBlank @Size(max=1000) String> materials) {}
    /** Audit请求参数；各字段的含义和约束见Swagger。 */
    public record Audit(@Schema(description="approved") boolean approved,@Schema(description="实名认证是否通过") boolean realVerified,@Schema(description="培训是否通过") boolean trainingPassed,@Schema(description="实操考核是否通过") boolean practicePassed,
        @NotNull @Future @Schema(description="到期时间") OffsetDateTime expiresAt,@NotBlank @Size(max=1000) @Schema(description="note") String note) {}
}
