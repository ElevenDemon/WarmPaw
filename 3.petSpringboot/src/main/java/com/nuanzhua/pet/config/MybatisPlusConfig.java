package com.nuanzhua.pet.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.*;

@Configuration
public class MybatisPlusConfig {
    /** 注册PostgreSQL分页插件，单页最多100条。 */
    @Bean public MybatisPlusInterceptor mybatisPlusInterceptor() {
        var interceptor=new MybatisPlusInterceptor();
        var pagination=new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        pagination.setMaxLimit(100L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
