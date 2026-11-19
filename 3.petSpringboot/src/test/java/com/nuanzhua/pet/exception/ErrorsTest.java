package com.nuanzhua.pet.exception;

import com.nuanzhua.pet.vo.Api;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorsTest {
    private final Errors errors = new Errors();

    @Test
    void unexpectedFailureDoesNotExposeExceptionDetails() {
        var response = errors.unexpected(new IllegalStateException("database password leaked"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("请稍后重试").doesNotContain("database password");
    }

    @Test
    void validationFailureUsesAReadableFieldPrompt() {
        var error = new FieldError("application", "personal.phone", "123", false,
            new String[]{"Pattern"}, null, "must match pattern");

        assertThat(Errors.friendlyValidation(error)).isEqualTo("请输入正确的11位手机号");
    }

    @Test
    void businessFailureReturnsItsCuratedUserMessage() {
        var response = errors.business(BusinessException.forbidden("请使用宠主账号提交喂养师入驻申请"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Api<?> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("请使用宠主账号提交喂养师入驻申请");
    }
}
