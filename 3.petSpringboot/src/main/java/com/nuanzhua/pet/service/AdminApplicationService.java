package com.nuanzhua.pet.service;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** 业务服务契约；调用方通过接口依赖具体实现。 */
public interface AdminApplicationService {
    Api<?> list(Actor actor,String module,
        int page,int size,
        String keyword,String status);
    Api<?> dashboard(Actor actor);
    Api<?> dictionary(Actor actor);
    Api<?> owner(Actor actor,long id);
}
