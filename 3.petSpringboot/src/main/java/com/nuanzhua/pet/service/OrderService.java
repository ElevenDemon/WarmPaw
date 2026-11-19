package com.nuanzhua.pet.service;
import com.nuanzhua.pet.dto.OrderRequests;
import com.nuanzhua.pet.entity.Review;
import com.nuanzhua.pet.vo.Actor;

import java.math.*;
import java.time.*;
import java.util.*;
import static com.nuanzhua.pet.common.Db.number;
import static com.nuanzhua.pet.common.BusinessSupport.state;

/** 业务服务契约；调用方通过接口依赖具体实现。 */
public interface OrderService {
    Map<String,Object> create(Actor actor,OrderRequests.Create r);
    void cancel(long id,Actor actor,String reason);
    void payDemo(long id,Actor actor);
    void respond(long id,Actor actor,boolean accept);
    void checkin(long id,Actor actor);
    void checklist(long id,Actor actor,OrderRequests.Checklist r);
    long media(long id,Actor actor,OrderRequests.Media r);
    void complete(long id,Actor actor);
    long review(long id,Actor actor,OrderRequests.Review r);
    void refundDemo(long id,Actor actor,String reason);
}
