package com.nuanzhua.pet.service;
import com.nuanzhua.pet.dto.OwnerRequests.*;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** 业务服务契约；调用方通过接口依赖具体实现。 */
public interface OwnerApplicationService {
    Api<?> pets(Actor actor);
    Api<?> addPet(Actor actor,Pet r);
    Api<?> updatePet(Actor actor,long id,Pet r);
    Api<?> deletePet(Actor actor,long id);
    Api<?> addresses(Actor actor);
    Api<?> addAddress(Actor actor,Address r);
    Api<?> updateAddress(Actor actor,long id,Address r);
}
