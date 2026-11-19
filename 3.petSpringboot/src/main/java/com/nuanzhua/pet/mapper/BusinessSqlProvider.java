package com.nuanzhua.pet.mapper;

import java.util.Map;

/** SQL comes exclusively from server code; values are MyBatis bound parameters. */
public class BusinessSqlProvider {
    /** 把服务端SQL中的占位符转换为MyBatis绑定参数，保留PostgreSQL问号运算符。 */
    public String sql(Map<String,Object> parameters) {
        String sql=(String)parameters.get("sql");
        Object[] args=(Object[])parameters.get("args");
        StringBuilder result=new StringBuilder();
        boolean quoted=false; int index=0;
        for(int i=0;i<sql.length();i++) {
            char c=sql.charAt(i);
            if(c=='\'') { quoted=!quoted; result.append(c); }
            else if(c=='?' && !quoted) {
                if(i+1<sql.length() && sql.charAt(i+1)=='?') { result.append("??"); i++; }
                else result.append("#{args[").append(index++).append("]}");
            } else result.append(c);
        }
        if(index!=args.length) throw new IllegalArgumentException("SQL parameter count mismatch");
        return result.toString();
    }
}
