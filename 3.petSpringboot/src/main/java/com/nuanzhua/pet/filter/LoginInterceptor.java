package com.nuanzhua.pet.filter;

import com.nuanzhua.pet.exception.BusinessException;
import com.nuanzhua.pet.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 校验随机Bearer会话，为控制器提供可信的当前登录人。 */
@Component
public class LoginInterceptor implements HandlerInterceptor {
    private final AuthService auth;

    public LoginInterceptor(AuthService auth) { this.auth = auth; }

    /** 跨域预检直接放行；普通请求验证令牌后写入actor请求属性。 */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equals(request.getMethod())) return true;
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        request.setAttribute("actor", auth.authenticate(header.substring(7)));
        return true;
    }
}
