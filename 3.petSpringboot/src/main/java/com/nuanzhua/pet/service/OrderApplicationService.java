package com.nuanzhua.pet.service;
import com.nuanzhua.pet.dto.OrderRequests;
import com.nuanzhua.pet.entity.Review;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** 业务服务契约；调用方通过接口依赖具体实现。 */
public interface OrderApplicationService {
    Api<?> create(Actor actor,OrderRequests.Create r);
    Api<?> list(Actor actor,int page,int size,String status);
    Api<?> detail(Actor actor,long id);
    Api<?> cancel(Actor actor,long id,OrderRequests.Reason r);
    Api<?> pay(Actor actor,long id);
    Api<?> assign(Actor actor,long id,OrderRequests.Assign r);
    Api<?> eligibility(Actor actor,long id,long sitterId);
    Api<?> accept(Actor actor,long id);
    Api<?> reject(Actor actor,long id);
    Api<?> checkin(Actor actor,long id);
    Api<?> checklist(Actor actor,long id,OrderRequests.Checklist r);
    Api<?> media(Actor actor,long id,OrderRequests.Media r);
    Api<?> mediaList(Actor actor,long id);
    Api<?> deleteMedia(Actor actor,long id,long mediaId);
    Api<?> complete(Actor actor,long id);
    Api<?> report(Actor actor,long id);
    Api<?> review(Actor actor,long id,OrderRequests.Review r);
    Api<?> refund(Actor actor,long id,OrderRequests.Reason r);
    Api<?> ai();
}
