package com.nuanzhua.pet.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.*;
import java.util.*;

/** 请求参数模型；字段约束由Bean Validation在接口入口校验。 */
public final class MemberRequests {
    private MemberRequests() {}
    /** Purchase请求参数；各字段的含义和约束见Swagger。 */
    public record Purchase(@NotNull @Positive @Schema(description="会员套餐ID") Long planId) {}
}
