package com.nuanzhua.pet.vo;

/** 统一响应及登录上下文视图，不包含密码或令牌摘要。 */
public record Api<T>(int code, String message, T data) {
    /** 构造统一成功响应，业务码为0。 */
    public static <T> Api<T> ok(T data) { return new Api<>(0, "ok", data); }
}
