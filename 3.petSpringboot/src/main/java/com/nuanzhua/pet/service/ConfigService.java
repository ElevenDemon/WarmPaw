package com.nuanzhua.pet.service;
import com.nuanzhua.pet.vo.Actor;

import java.util.*;

/** 业务服务契约；调用方通过接口依赖具体实现。 */
public interface ConfigService {
    long save(Actor actor,String permission,String table,Long id,LinkedHashMap<String,Object> values,Set<String> jsonColumns);
    /** 按声明顺序构造受控列和值映射。 */
    public static LinkedHashMap<String,Object> values(Object... pairs) {
        var m=new LinkedHashMap<String,Object>(); for(int i=0;i<pairs.length;i+=2) m.put((String)pairs[i],pairs[i+1]); return m;
    }
    void settle(Actor actor,List<Long> ids);
    void permissions(Actor actor,String role,Set<String> permissions);
}
