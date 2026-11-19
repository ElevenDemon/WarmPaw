package com.nuanzhua.pet.service;
import com.nuanzhua.pet.dto.OperationsRequests;
import com.nuanzhua.pet.vo.Actor;

import java.util.*;

/** 业务服务契约；调用方通过接口依赖具体实现。 */
public interface OperationsService {
    long emergency(Actor actor,long orderId,String description);
    void updateEmergency(Actor actor,long id,OperationsRequests.EmergencyUpdate r);
    long complaint(Actor actor,long orderId,String content);
    Map<String,Object> conversation(Actor actor,long id);
    long send(Actor actor,OperationsRequests.Message r);
}
