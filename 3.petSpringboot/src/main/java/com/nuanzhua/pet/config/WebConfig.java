package com.nuanzhua.pet.config;

import com.nuanzhua.pet.filter.LoginInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

/** MVC配置：统一注册跨域白名单和登录拦截规则。 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final LoginInterceptor login;
    private final String[] origins;

    public WebConfig(LoginInterceptor login, @Value("${app.cors-origins}") String[] origins) {
        this.login = login;
        this.origins = origins;
    }

    /** 允许已配置的Vue开发站点携带Authorization访问业务接口。 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(origins)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("Authorization", "Content-Type").maxAge(3600);
    }

    /** 公开登录、注册和公共目录；Swagger路径位于业务API之外。 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(login).addPathPatterns("/api/**")
            .excludePathPatterns("/api/auth/login", "/api/auth/register", "/api/public/**");
    }
}
