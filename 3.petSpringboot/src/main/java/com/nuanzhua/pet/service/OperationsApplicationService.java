package com.nuanzhua.pet.service;
import com.nuanzhua.pet.dto.OperationsRequests.*;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** 业务服务契约；调用方通过接口依赖具体实现。 */
public interface OperationsApplicationService {
    Api<?> emergency(Actor actor,Ticket r);
    Api<?> consent(Actor actor,long id);
    Api<?> updateEmergency(Actor actor,long id,EmergencyUpdate r);
    Api<?> hospitals(Actor actor,double longitude,double latitude);
    Api<?> hospital(Actor actor,Hospital r);
    Api<?> complaint(Actor actor,Ticket r);
    Api<?> resolve(Actor actor,long id,ComplaintUpdate r);
    Api<?> conversation(Actor actor,Conversation r);
    Api<?> conversations(Actor actor,int page,int size);
    Api<?> takeover(Actor actor,long id);
    Api<?> requestHuman(Actor actor,long id);
    Api<?> resumeAi(Actor actor,long id);
    Api<?> close(Actor actor,long id);
    Api<?> send(Actor actor,Message r);
    Api<?> messages(Actor actor,Long conversationId,Long orderId,int page,int size);
    Api<?> notifications(Actor actor,int page,int size);
    Api<?> readAll(Actor actor);
    Api<?> read(Actor actor,long id);
}
