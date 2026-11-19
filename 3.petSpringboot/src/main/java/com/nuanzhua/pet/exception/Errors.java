package com.nuanzhua.pet.exception;
import com.nuanzhua.pet.vo.Api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@RestControllerAdvice
public class Errors {
    private static final Logger log = LoggerFactory.getLogger(Errors.class);
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<Api<Object>> business(BusinessException e) {
        return ResponseEntity.status(e.status).body(new Api<>(e.status.value(), e.userMessage(), null));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Api<Object>> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream().map(Errors::friendlyValidation)
            .findFirst().orElse("请检查必填信息后重试");
        return ResponseEntity.badRequest().body(new Api<>(400, message, null));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    ResponseEntity<Api<Object>> invalid(Exception e) {
        return ResponseEntity.badRequest().body(new Api<>(400, "提交内容格式有误，请检查填写内容后重试", null));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Api<Object>> integrity(Exception e) {
        log.warn("Database constraint rejected a request", e);
        return ResponseEntity.status(409).body(new Api<>(409, "提交内容与现有信息冲突，请核对后重试；如仍无法处理，请联系客服", null));
    }
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Api<Object>> notFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Api<>(404, "没有找到对应的页面或服务，请检查后重试", null));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<Api<Object>> unexpected(Exception e) {
        log.error("Unhandled request error", e);
        return ResponseEntity.internalServerError().body(new Api<>(500, "服务暂时开了个小差，请稍后重试；问题持续时请联系客服", null));
    }

    static String friendlyValidation(FieldError error) {
        String field = error.getField();
        String leaf = field.substring(field.lastIndexOf('.') + 1);
        String label = Map.ofEntries(
            Map.entry("personal", "个人资料"), Map.entry("name", "姓名"), Map.entry("phone", "手机号"),
            Map.entry("emergencyContact", "紧急联系人"), Map.entry("district", "所在区"),
            Map.entry("address", "详细地址"), Map.entry("longitude", "经度"), Map.entry("latitude", "纬度"),
            Map.entry("radiusKm", "服务半径"), Map.entry("transport", "交通方式"),
            Map.entry("categories", "照护类目"), Map.entry("schedule", "服务档期"),
            Map.entry("gradeId", "服务等级"), Map.entry("settlementMethod", "结算方式"),
            Map.entry("materials", "资质材料"), Map.entry("category", "资质类别"),
            Map.entry("expiresAt", "资质有效期"), Map.entry("note", "审核说明")
        ).getOrDefault(leaf, "填写内容");
        String code = error.getCode();
        if ("personal".equals(leaf)) return "请补全个人资料后再提交";
        if ("categories".equals(leaf) && "NotEmpty".equals(code)) return "请至少选择一个照护类目";
        if ("phone".equals(leaf) && "Pattern".equals(code)) return "请输入正确的11位手机号";
        if (("longitude".equals(leaf) || "latitude".equals(leaf)) && ("DecimalMin".equals(code) || "DecimalMax".equals(code)))
            return "服务地址坐标超出当前服务范围，请重新选择深圳范围内的地点";
        if ("radiusKm".equals(leaf)) return "服务半径请填写0.5至50公里";
        if ("NotBlank".equals(code) || "NotNull".equals(code)) return "请填写" + label;
        if ("NotEmpty".equals(code)) return "请补充" + label;
        if ("Pattern".equals(code)) return label + "格式不正确，请检查后重试";
        if ("Size".equals(code)) return label + "长度超出限制，请精简后重试";
        if ("DecimalMin".equals(code) || "DecimalMax".equals(code)) return label + "超出允许范围，请检查后重试";
        if ("Positive".equals(code) || "Min".equals(code) || "Max".equals(code)) return label + "数值不符合要求，请检查后重试";
        return "请检查" + label + "后重试";
    }

}
