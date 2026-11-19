package com.nuanzhua.pet.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** 请求参数模型；字段约束由Bean Validation在接口入口校验。 */
public final class OwnerRequests {
    private OwnerRequests() {}
    /** Pet请求参数；各字段的含义和约束见Swagger。 */
    public record Pet(@NotBlank @Size(max=32) @Schema(description="名称") String name,@NotNull @Pattern(regexp="CAT|DOG|SMALL|OTHER") @Schema(description="宠物或认证类别") String category,
        @Size(max=60) @Schema(description="品种") String breed,@PastOrPresent @Schema(description="出生日期") LocalDate birthday,@DecimalMin("0.01") @DecimalMax("9999.99") @Schema(description="体重kg") BigDecimal weight,
        @Size(max=500) @Schema(description="性格与注意事项") String temperament,@Size(max=5000) @Schema(description="饮食说明") String dietaryNotes,@Size(max=5000) @Schema(description="病史说明") String medicalHistory,@Size(max=1000) @Schema(description="照片地址") String photoUrl) {}
    /** Address请求参数；各字段的含义和约束见Swagger。 */
    public record Address(@NotBlank @Size(max=32) @Schema(description="联系人") String contactName,@NotNull @Pattern(regexp="1[3-9]\\d{9}") @Schema(description="手机号码") String phone,
        @NotBlank @Size(max=60) @Schema(description="行政区") String district,@NotBlank @Size(max=300) @Schema(description="详细地址") String address,
        @NotNull @DecimalMin("113.7") @DecimalMax("114.7") @Schema(description="经度") BigDecimal longitude,
        @NotNull @DecimalMin("22.4") @DecimalMax("22.9") @Schema(description="纬度") BigDecimal latitude) {}
}
