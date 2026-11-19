package com.nuanzhua.pet.service;

import com.nuanzhua.pet.vo.Actor;
import java.util.Map;

/** 移动端补充查询，数据归属始终由登录身份限定。 */
public interface MobileApplicationService {
    Map<String,Object> emergencies(Actor actor,int page,int size);
    Map<String,Object> settlements(Actor actor,int page,int size);
}
