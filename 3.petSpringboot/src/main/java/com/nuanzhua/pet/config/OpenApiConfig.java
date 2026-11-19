package com.nuanzhua.pet.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;

/** Swagger使用登录接口返回的随机Bearer令牌；未使用JWT。 */
@Configuration
public class OpenApiConfig {
    /** 配置接口总览和右上角Authorize授权入口。 */
    @Bean public OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title("暖爪宠物上门喂养平台 API").version("1.0.0")
                .description("Spring Boot + MyBatis-Plus + PostgreSQL。先调用登录接口，再在Authorize中填入token。AI功能预留，返回501。演示支付需显式启用。"))
            .components(new Components().addSecuritySchemes("bearerAuth",new SecurityScheme()
                .type(SecurityScheme.Type.HTTP).scheme("bearer").description("登录响应data.token，不需要手工添加Bearer前缀")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
