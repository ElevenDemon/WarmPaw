package com.nuanzhua.pet.service;
import com.nuanzhua.pet.dto.SitterRequests;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;
import org.springframework.web.bind.annotation.*;

/** 业务服务契约；调用方通过接口依赖具体实现。 */
public interface SitterApplicationService {
    Api<?> apply(Actor actor,SitterRequests.Application r);
    Api<?> mine(Actor actor);
    Api<?> personal(Actor actor,SitterRequests.Personal r);
    Api<?> qualification(Actor actor,SitterRequests.Qualification r);
    Api<?> income(Actor actor);
    Api<?> detail(Actor actor,long id);
    Api<?> save(Actor actor,long id,SitterRequests.Profile r);
    Api<?> audit(Actor actor,long id,SitterRequests.Audit r);
    /** 入驻审核使用喂养师档案ID。 */
    Api<?> admission(Actor actor,long id,SitterRequests.AdmissionAudit r);
}
