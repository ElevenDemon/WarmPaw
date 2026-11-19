package com.nuanzhua.pet.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.*;
import java.util.*;

/** 请求参数模型；字段约束由Bean Validation在接口入口校验。 */
public final class CouponRequests {
    private CouponRequests() {}
    /** Claim请求参数；各字段的含义和约束见Swagger。 */
    public record Claim(@Size(max=200) @Schema(description="设备标识，服务端仅保存摘要") String deviceId) {}
    /** Redeem请求参数；各字段的含义和约束见Swagger。 */
    public record Redeem(@NotBlank @Size(max=32) @Schema(description="稳定业务代码") String code,@Size(max=200) @Schema(description="设备标识，服务端仅保存摘要") String deviceId) {}
    /** Push请求参数；各字段的含义和约束见Swagger。 */
    public record Push(@NotEmpty @Size(max=100) @Schema(description="目标宠主账号ID列表") List<@NotNull @Positive Long> ownerIds,@Min(1) @Max(20) @Schema(description="数量") int quantity) {}
}
