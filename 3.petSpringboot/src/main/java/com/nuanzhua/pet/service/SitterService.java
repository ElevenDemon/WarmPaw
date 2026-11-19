package com.nuanzhua.pet.service;
import com.nuanzhua.pet.dto.SitterRequests;
import com.nuanzhua.pet.vo.Actor;

import java.util.*;

/** 业务服务契约；调用方通过接口依赖具体实现。 */
public interface SitterService {
    Map<String,Object> detail(long id);
    void personal(long id,SitterRequests.Personal r);
    void save(long id,Actor actor,SitterRequests.Profile r);
    long apply(Actor actor,SitterRequests.Application r);
    void audit(long qualificationId,Actor actor,SitterRequests.Audit r);
    /** 审核同一份喂养师档案的入驻状态。 */
    void admission(long id,Actor actor,SitterRequests.AdmissionAudit r);
}
