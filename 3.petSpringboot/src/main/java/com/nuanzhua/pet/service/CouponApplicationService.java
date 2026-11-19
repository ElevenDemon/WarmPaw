package com.nuanzhua.pet.service;
import com.nuanzhua.pet.dto.CouponRequests.*;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** 业务服务契约；调用方通过接口依赖具体实现。 */
public interface CouponApplicationService {
    Api<?> center(Actor actor);
    Api<?> mine(Actor actor,int page,int size);
    Api<?> claim(Actor actor,long id,Claim r,HttpServletRequest request);
    Api<?> redeem(Actor actor,Redeem r,HttpServletRequest request);
    Api<?> push(Actor actor,long id,Push r);
}
