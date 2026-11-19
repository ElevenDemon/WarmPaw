package com.nuanzhua.pet.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.*;
import java.util.*;

/** 请求参数模型；字段约束由Bean Validation在接口入口校验。 */
public final class AuthRequests {
    private AuthRequests() {}
    /** Login请求参数；各字段的含义和约束见Swagger。 */
    public record Login(@NotBlank @Size(max=64) @Schema(description="登录账号") String username,@NotBlank @Size(max=72) @Schema(description="password") String password) {}
    /** Register请求参数；各字段的含义和约束见Swagger。 */
    public record Register(@Pattern(regexp="[a-zA-Z0-9_]{4,32}") @NotNull @Schema(description="登录账号") String username,
        @NotBlank @Size(min=8,max=64) @Schema(description="password") String password,@NotBlank @Size(max=32) @Schema(description="名称") String name,
        @NotNull @Pattern(regexp="1[3-9]\\d{9}") @Schema(description="手机号码") String phone) {}
}
