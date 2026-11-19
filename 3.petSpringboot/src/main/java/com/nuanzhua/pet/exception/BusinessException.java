package com.nuanzhua.pet.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    public final HttpStatus status;
    private final String userMessage;
    public BusinessException(HttpStatus status, String userMessage) { super(userMessage); this.status = status; this.userMessage = userMessage; }
    /** 仅用于返回给用户的业务提示；异常堆栈和第三方错误详情不会由此暴露。 */
    public String userMessage() { return userMessage; }
    /** 创建参数校验失败异常。 */
    public static BusinessException bad(String message) { return new BusinessException(HttpStatus.BAD_REQUEST, message); }
    /** 创建重复操作或状态冲突异常。 */
    public static BusinessException conflict(String message) { return new BusinessException(HttpStatus.CONFLICT, message); }
    /** 创建无权访问异常。 */
    public static BusinessException forbidden() { return new BusinessException(HttpStatus.FORBIDDEN, "无权访问该资源"); }
    /** 创建适合当前业务场景展示的无权访问提示。 */
    public static BusinessException forbidden(String message) { return new BusinessException(HttpStatus.FORBIDDEN, message); }
}
