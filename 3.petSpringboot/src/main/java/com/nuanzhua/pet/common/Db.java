package com.nuanzhua.pet.common;
import com.nuanzhua.pet.exception.BusinessException;
import com.nuanzhua.pet.mapper.BusinessMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.util.*;

/** MyBatis access for PostgreSQL reports, row locks, JSON and RETURNING statements. */
@Component
public class Db {
    private final BusinessMapper mapper;
    private final ObjectMapper jsonMapper;
    public Db(BusinessMapper mapper,ObjectMapper jsonMapper) { this.mapper=mapper; this.jsonMapper=jsonMapper; }
    /** 使用MyBatis绑定参数执行服务端SQL，避免拼接用户值。 */
    public List<Map<String,Object>> queryForList(String sql,Object... args) { return mapper.rows(sql,args); }
    /** 使用MyBatis绑定参数执行服务端SQL，避免拼接用户值。 */
    public <T> List<T> queryForList(String sql,Class<T> type,Object... args) {
        return queryForList(sql,args).stream().map(row->type.cast(row.values().iterator().next())).toList();
    }
    /** 执行服务端参数化写入SQL。 */
    public int update(String sql,Object... args) { return mapper.execute(sql,args); }
    /** 查询单条记录，不存在时返回404。 */
    public Map<String,Object> one(String sql,Object... args) {
        var rows=queryForList(sql,args);
        if(rows.isEmpty()) throw new BusinessException(HttpStatus.NOT_FOUND,"记录不存在");
        return rows.getFirst();
    }
    /** 执行返回单个数值的查询或INSERT RETURNING；无记录返回404。 */
    public long id(String sql,Object... args) { return ((Number)one(sql,args).values().iterator().next()).longValue(); }
    /** 序列化快照JSON；转换错误作为业务参数异常处理。 */
    public String json(Object object) {
        try { return jsonMapper.writeValueAsString(object); }
        catch(JsonProcessingException e) { throw BusinessException.bad("JSON 格式错误"); }
    }
    /** 读取数据库数值列并转换为long。 */
    public static long number(Map<String,Object> row,String key) { return ((Number)row.get(key)).longValue(); }
    /** 通过MyBatis-Plus分页插件查询，限制page和size范围。 */
    public Map<String,Object> page(String table,String where,List<Object> args,int page,int size) {
        if(page<1 || size<1 || size>100) throw BusinessException.bad("page≥1，size 为 1–100");
        var result=mapper.page(new Page<Map<String,Object>>(page,size),"select * from "+table+" where "+where+" order by id desc",args.toArray());
        return Map.of("list",result.getRecords(),"total",result.getTotal(),"page",page,"size",size);
    }
}
