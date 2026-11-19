package com.nuanzhua.pet.service;
import com.nuanzhua.pet.vo.Actor;

import java.time.*;

/** 业务服务契约；调用方通过接口依赖具体实现。 */
public interface MemberService {
    long purchase(Actor actor,long planId);
    void payDemo(Actor actor,long purchaseId);
}
