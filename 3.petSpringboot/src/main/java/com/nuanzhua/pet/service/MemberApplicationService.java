package com.nuanzhua.pet.service;
import com.nuanzhua.pet.dto.MemberRequests.*;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

/** 业务服务契约；调用方通过接口依赖具体实现。 */
public interface MemberApplicationService {
    Api<?> catalog();
    Api<?> health();
    Api<?> mine(Actor actor);
    Api<?> purchase(Actor actor,Purchase r);
    Api<?> pay(Actor actor,long id);
}
