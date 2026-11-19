package com.nuanzhua.pet.service;
import com.nuanzhua.pet.vo.Actor;

import java.util.*;

/** 业务服务契约；调用方通过接口依赖具体实现。 */
public interface CouponService {
    Map<String,Object> claim(long templateId,Actor actor,String device,String ip);
    long credit(Map<String,Object> t,long owner,String source);
    List<Long> push(long templateId,Actor actor,List<Long> owners,int quantity);
}
