package com.nuanzhua.pet.config;

import org.apache.ibatis.type.*;
import org.postgresql.util.PGobject;
import java.sql.*;

/** JSON text <-> PostgreSQL jsonb for BaseMapper entity CRUD. */
public class JsonTextTypeHandler extends BaseTypeHandler<String> {
    /** 将实体JSON字符串绑定为PostgreSQL jsonb对象。 */
    @Override public void setNonNullParameter(PreparedStatement ps,int i,String value,JdbcType type) throws SQLException {
        var object=new PGobject(); object.setType("jsonb"); object.setValue(value); ps.setObject(i,object);
    }
    /** 读取可为空的PostgreSQL JSON文本。 */
    @Override public String getNullableResult(ResultSet rs,String name) throws SQLException { return rs.getString(name); }
    /** 读取可为空的PostgreSQL JSON文本。 */
    @Override public String getNullableResult(ResultSet rs,int index) throws SQLException { return rs.getString(index); }
    /** 读取可为空的PostgreSQL JSON文本。 */
    @Override public String getNullableResult(CallableStatement cs,int index) throws SQLException { return cs.getString(index); }
}
