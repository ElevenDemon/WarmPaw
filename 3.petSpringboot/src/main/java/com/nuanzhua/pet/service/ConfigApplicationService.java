package com.nuanzhua.pet.service;
import com.nuanzhua.pet.dto.ConfigRequests;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;
import org.springframework.web.bind.annotation.*;
import java.math.*;
import java.util.*;
import static com.nuanzhua.pet.service.ConfigService.values;

/** 业务服务契约；调用方通过接口依赖具体实现。 */
public interface ConfigApplicationService {
    Api<?> price(Actor actor,long id,ConfigRequests.Price r);
    Api<?> addPlan(Actor actor,ConfigRequests.Plan r);
    Api<?> plan(Actor actor,long id,ConfigRequests.Plan r);
    Api<?> benefit(Actor actor,long id,ConfigRequests.Benefit r);
    Api<?> addGrade(Actor actor,ConfigRequests.Grade r);
    Api<?> grade(Actor actor,long id,ConfigRequests.Grade r);
    Api<?> simulate(Actor actor,int orders,BigDecimal rating,int complaints);
    Api<?> estimate(Actor actor,BigDecimal amount);
    Api<?> addCoupon(Actor actor,ConfigRequests.Coupon r);
    Api<?> coupon(Actor actor,long id,ConfigRequests.Coupon r);
    Api<?> notice(Actor actor,long id,ConfigRequests.Notice r);
    Api<?> ownerStatus(Actor actor,long id,ConfigRequests.Status r);
    Api<?> memberStatus(Actor actor,long id,ConfigRequests.MemberStatus r);
    Api<?> settle(Actor actor,ConfigRequests.Batch r);
    Api<?> permissions(Actor actor,String role,ConfigRequests.Permissions r);
    Api<?> staff(Actor actor,ConfigRequests.Staff r);
}
