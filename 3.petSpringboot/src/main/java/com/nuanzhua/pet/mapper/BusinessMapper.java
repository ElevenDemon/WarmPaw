package com.nuanzhua.pet.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.*;
import java.util.*;

/** 业务SQL数据访问；实体CRUD继承MyBatis-Plus BaseMapper。 */
@Mapper
public interface BusinessMapper {
    @SelectProvider(type=BusinessSqlProvider.class,method="sql",affectData=true)
    @Options(flushCache=Options.FlushCachePolicy.TRUE,useCache=false)
    List<Map<String,Object>> rows(@Param("sql") String sql,@Param("args") Object[] args);
    @SelectProvider(type=BusinessSqlProvider.class,method="sql")
    IPage<Map<String,Object>> page(IPage<Map<String,Object>> page,@Param("sql") String sql,@Param("args") Object[] args);
    @UpdateProvider(type=BusinessSqlProvider.class,method="sql")
    int execute(@Param("sql") String sql,@Param("args") Object[] args);
}
