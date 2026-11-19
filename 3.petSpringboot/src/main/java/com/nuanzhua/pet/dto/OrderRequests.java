package com.nuanzhua.pet.dto;
import com.nuanzhua.pet.entity.Review;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.*;

/** 请求参数模型；字段约束由Bean Validation在接口入口校验。 */
public final class OrderRequests {
    private OrderRequests() {}
    /** Item请求参数；各字段的含义和约束见Swagger。 */
    public record Item(@NotNull @Positive @Schema(description="价格配置ID") Long serviceId,@Min(1) @Max(20) @Schema(description="数量") int quantity) {}
    /** Create请求参数；各字段的含义和约束见Swagger。 */
    public record Create(@NotBlank @Size(max=80) @Schema(description="同宠主下单幂等键") String requestKey,@NotNull @Positive @Schema(description="当前宠主的地址ID") Long addressId,
        @NotEmpty @Size(max=20) @Schema(description="本次服务的宠物ID列表") List<@NotNull @Positive Long> petIds,
        @NotNull @Future @Schema(description="预约服务开始") OffsetDateTime serviceStart,@NotNull @Future @Schema(description="预约服务结束") OffsetDateTime serviceEnd,
        @NotEmpty @Size(max=20) @Schema(description="服务明细，必须包含一次BASE基础服务") List<@Valid Item> items,@Schema(description="本次锁定或核销的用户券ID") Long couponId,
        @NotBlank @Size(max=40) @Schema(description="已阅读的服务协议版本") String agreementVersion,@AssertTrue @Schema(description="必须明确同意服务协议") boolean agreementAccepted,
        @NotBlank @Size(max=500) @Schema(description="本次服务授权范围") String authorizationScope,@Size(max=1000) @Schema(description="备注") String notes) {}
    /** Reason请求参数；各字段的含义和约束见Swagger。 */
    public record Reason(@NotBlank @Size(max=1000) @Schema(description="业务说明或失败理由") String reason) {}
    /** Assign请求参数；各字段的含义和约束见Swagger。 */
    public record Assign(@NotNull @Positive @Schema(description="喂养师档案ID") Long sitterId,@NotBlank @Size(max=1000) @Schema(description="业务说明或失败理由") String reason) {}
    /** Checklist请求参数；各字段的含义和约束见Swagger。 */
    public record Checklist(@NotBlank @Pattern(regexp="FEED|WATER|CLEAN|PLAY|DOOR") @Schema(description="稳定业务代码") String code,
        @Schema(description="是否完成") boolean completed,@DecimalMin("0") @DecimalMax("100000") @Schema(description="数量") BigDecimal quantity,@Size(max=20) @Schema(description="计量单位，陪玩必须MINUTE") String unit,@Size(max=1000) @Schema(description="备注") String notes) {}
    /** Media请求参数；各字段的含义和约束见Swagger。 */
    public record Media(@NotNull @Pattern(regexp="PHOTO|VIDEO|LIVE") @Schema(description="媒体类型PHOTO/VIDEO/LIVE") String kind,
        @NotBlank @Size(max=500) @Schema(description="相对存储对象键，不是公网播放地址") String objectKey) {}
    /** Review请求参数；各字段的含义和约束见Swagger。 */
    public record Review(@Min(1) @Max(5) @Schema(description="评分1至5") int score,@Size(max=20) @Schema(description="评价标签JSON数组") List<@Size(max=32) String> tags,
        @NotBlank @Size(max=1000) @Schema(description="文本内容") String content) {}
}
