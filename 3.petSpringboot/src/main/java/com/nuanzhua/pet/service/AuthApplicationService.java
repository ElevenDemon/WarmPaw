package com.nuanzhua.pet.service;
import com.nuanzhua.pet.dto.AuthRequests.*;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

/** 业务服务契约；调用方通过接口依赖具体实现。 */
public interface AuthApplicationService {
    Api<?> login(Login r);
    Api<?> register(Register r);
    Api<?> me(Actor actor);
    Api<?> switchRole(Actor actor,String role,String token);
    Api<?> logout(String header);
}
