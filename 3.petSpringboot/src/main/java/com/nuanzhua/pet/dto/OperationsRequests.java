package com.nuanzhua.pet.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** 请求参数模型；字段约束由Bean Validation在接口入口校验。 */
public final class OperationsRequests {
    private OperationsRequests() {}
    /** Ticket请求参数；各字段的含义和约束见Swagger。 */
    public record Ticket(@NotNull @Positive @Schema(description="服务订单ID") Long orderId,@NotBlank @Size(max=2000) @Schema(description="文本内容") String content) {}
    /** EmergencyUpdate请求参数；各字段的含义和约束见Swagger。 */
    public record EmergencyUpdate(@NotNull @Pattern(regexp="ACCEPTED|TRANSPORTING|RESOLVED") @Schema(description="业务状态，枚举见本表CHECK约束") String status,@Schema(description="喂养师档案ID") Long sitterId,@Schema(description="合作医院ID") Long hospitalId,@Size(max=5000) @Schema(description="处理结果") String resolution) {}
    /** ComplaintUpdate请求参数；各字段的含义和约束见Swagger。 */
    public record ComplaintUpdate(@NotNull @Pattern(regexp="PROCESSING|RESOLVED") @Schema(description="业务状态，枚举见本表CHECK约束") String status,@NotBlank @Size(max=5000) @Schema(description="处理结果") String resolution) {}
    /** Conversation请求参数；各字段的含义和约束见Swagger。 */
    public record Conversation(@Schema(description="服务订单ID") Long orderId) {}
    /** Message请求参数；各字段的含义和约束见Swagger。 */
    public record Message(@Schema(description="客服会话ID") Long conversationId,@Schema(description="服务订单ID") Long orderId,@NotBlank @Size(max=2000) @Schema(description="文本内容") String content,@Size(max=20) @Schema(description="媒体对象键JSON数组") List<@Size(max=500) String> mediaKeys) {}
    /** Hospital请求参数；各字段的含义和约束见Swagger。 */
    public record Hospital(@NotBlank @Size(max=100) @Schema(description="名称") String name,@NotBlank @Size(max=32) @Schema(description="手机号码") String phone,
        @NotBlank @Size(max=300) @Schema(description="详细地址") String address,@NotNull @DecimalMin("-180") @DecimalMax("180") @Schema(description="经度") BigDecimal longitude,
        @NotNull @DecimalMin("-90") @DecimalMax("90") @Schema(description="纬度") BigDecimal latitude,@Schema(description="emergency24h") boolean emergency24h,@Schema(description="是否启用") boolean active) {}
}
