package com.nuanzhua.pet.service.impl;
import com.nuanzhua.pet.service.OperationsApplicationService;
import org.springframework.stereotype.Service;
import com.nuanzhua.pet.dto.OperationsRequests.*;
import com.nuanzhua.pet.common.BusinessSupport;
import com.nuanzhua.pet.common.Db;

import com.nuanzhua.pet.exception.BusinessException;
import com.nuanzhua.pet.service.DispatchService;
import com.nuanzhua.pet.service.OperationsService;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;

import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** 业务服务实现；写操作在服务事务内完成，数据库约束提供最后一致性保护。 */
@Service
@org.springframework.transaction.annotation.Transactional
public class OperationsApplicationServiceImpl implements OperationsApplicationService {
    private final Db db; private final OperationsService operations; private final BusinessSupport support;
    public OperationsApplicationServiceImpl(Db db,OperationsService operations,BusinessSupport support) { this.db=db; this.operations=operations; this.support=support; }

    /** 创建应急工单并向宠主和运营人员发送站内提醒。 */
    public Api<?> emergency(Actor actor,Ticket r) { return Api.ok(operations.emergency(actor,r.orderId(),r.content())); }
    /** 记录宠主明确同意紧急送医。 */
    @Transactional public Api<?> consent(Actor actor,long id) {
        actor.role("OWNER"); var e=db.one("select * from emergency_ticket where id=? for update",id); support.order(Db.number(e,"order_id"),actor,false);
        if("RESOLVED".equals(e.get("status"))) throw BusinessException.conflict("工单已关闭");
        db.update("update emergency_ticket set owner_consent=true,updated_at=now() where id=?",id); support.log(actor,"同意紧急送医","emergency",id,e,Map.of("ownerConsent",true)); return Api.ok(true);
    }
    /** 推进应急工单；送医前必须有宠主同意和目标医院。 */
    public Api<?> updateEmergency(Actor actor,long id,EmergencyUpdate r) { operations.updateEmergency(actor,id,r); return Api.ok(true); }
    /** 按直线距离查询最近的合作医院。 */
    public Api<?> hospitals(Actor actor,double longitude,double latitude) {
        if(!Double.isFinite(longitude) || !Double.isFinite(latitude) || Math.abs(longitude)>180 || Math.abs(latitude)>90) throw BusinessException.bad("经纬度不合法");
        var hospitals=db.queryForList("select * from hospital where active");
        for(var h:hospitals) h.put("distanceKm",DispatchService.distance(longitude,latitude,((Number)h.get("longitude")).doubleValue(),((Number)h.get("latitude")).doubleValue()));
        hospitals.sort(Comparator.comparingDouble(h->((Number)h.get("distanceKm")).doubleValue())); return Api.ok(hospitals.stream().limit(20).toList());
    }
    /** 新增合作医院。 */
    @Transactional public Api<?> hospital(Actor actor,Hospital r) {
        actor.require("emergency:update"); long id=db.id("insert into hospital(name,phone,address,longitude,latitude,emergency_24h,active) values (?,?,?,?,?,?,?) returning id",r.name(),r.phone(),r.address(),r.longitude(),r.latitude(),r.emergency24h(),r.active());
        support.log(actor,"新增合作医院","hospital",id,null,r); return Api.ok(id);
    }
    /** 创建关联订单的投诉工单。 */
    public Api<?> complaint(Actor actor,Ticket r) { return Api.ok(operations.complaint(actor,r.orderId(),r.content())); }
    /** 处理或关闭投诉并保存解决说明。 */
    @Transactional public Api<?> resolve(Actor actor,long id,ComplaintUpdate r) {
        actor.require("review:update"); var before=db.one("select * from complaint where id=? for update",id);
        if("RESOLVED".equals(before.get("status"))) throw BusinessException.conflict("投诉已关闭");
        db.update("update complaint set status=?,resolution=? where id=?",r.status(),r.resolution(),id); support.log(actor,"处理投诉","complaint",id,before,r); return Api.ok(true);
    }
    /** 查询或创建客服会话并校验归属。 */
    public Api<?> conversation(Actor actor,Conversation r) {
        if(!Set.of("OWNER","SITTER").contains(actor.role())) throw BusinessException.forbidden();
        if(r.orderId()!=null) support.order(r.orderId(),actor,false);
        return Api.ok(db.id("insert into support_conversation(owner_id,order_id) values (?,?) returning id",actor.id(),r.orderId()));
    }
    /** 分页查询可访问的客服会话。 */
    public Api<?> conversations(Actor actor,int page,int size) {
        if(actor.staff()) { actor.require("service:read"); return Api.ok(db.page("support_conversation","true",List.of(),page,size)); }
        return Api.ok(db.page("support_conversation","owner_id=?",List.of(actor.id()),page,size));
    }
    /** 人工接管客服会话，防止多个客服同时接管。 */
    @Transactional public Api<?> takeover(Actor actor,long id) {
        actor.require("service:update"); var before=db.one("select * from support_conversation where id=? for update",id);
        if("CLOSED".equals(before.get("status"))) throw BusinessException.conflict("会话已关闭");
        if(before.get("agent_id")!=null && Db.number(before,"agent_id")!=actor.id()) throw BusinessException.conflict("会话已由其他客服接管");
        db.update("update support_conversation set status='HUMAN',agent_id=? where id=?",actor.id(),id); support.log(actor,"人工接管客服","conversation",id,before,Map.of("agentId",actor.id())); return Api.ok(true);
    }
    /** 宠主请求人工客服；后续消息进入客服待处理队列，不再触发AI回复。 */
    @Transactional public Api<?> requestHuman(Actor actor,long id) {
        if(!Set.of("OWNER","SITTER").contains(actor.role())) throw BusinessException.forbidden();
        var before=db.one("select * from support_conversation where id=? for update",id);
        if(Db.number(before,"owner_id")!=actor.id()) throw BusinessException.forbidden();
        if("CLOSED".equals(before.get("status"))) throw BusinessException.conflict("会话已关闭");
        if("WAITING_HUMAN".equals(before.get("status"))) db.update("update support_conversation set status='HUMAN_REQUESTED' where id=?",id);
        return Api.ok(true);
    }
    /** 结束人工会话并创建独立的新AI会话，保留旧会话供历史查看。 */
    @Transactional public Api<?> resumeAi(Actor actor,long id) {
        if(!Set.of("OWNER","SITTER").contains(actor.role())) throw BusinessException.forbidden();
        var before=db.one("select * from support_conversation where id=? for update",id);
        if(Db.number(before,"owner_id")!=actor.id()) throw BusinessException.forbidden();
        if("CLOSED".equals(before.get("status"))) throw BusinessException.conflict("会话已结束");
        if(!Set.of("HUMAN_REQUESTED","HUMAN").contains(before.get("status"))) throw BusinessException.conflict("当前会话仍由智能客服处理");
        db.update("update support_conversation set status='CLOSED' where id=?",id);
        long newId=db.id("insert into support_conversation(owner_id) values (?) returning id",actor.id());
        support.log(actor,"结束人工会话并恢复AI客服","conversation",id,before,Map.of("newConversationId",newId));
        return Api.ok(newId);
    }
    /** 关闭人工客服会话。 */
    @Transactional public Api<?> close(Actor actor,long id) {
        actor.require("service:update"); var before=db.one("select * from support_conversation where id=? for update",id);
        db.update("update support_conversation set status='CLOSED' where id=?",id); support.log(actor,"关闭客服会话","conversation",id,before,Map.of("status","CLOSED")); return Api.ok(true);
    }
    /** 发送客服或订单消息；人工回复前必须接管会话。 */
    public Api<?> send(Actor actor,Message r) { return Api.ok(operations.send(actor,r)); }
    /** 分页查询客服或订单消息，必须且只能指定一种会话。 */
    public Api<?> messages(Actor actor,Long conversationId,Long orderId,int page,int size) {
        if((conversationId==null)==(orderId==null)) throw BusinessException.bad("仅提供conversationId或orderId其中一个");
        if(orderId!=null) { support.order(orderId,actor,false); return Api.ok(db.page("chat_message","order_id=?",List.of(orderId),page,size)); }
        operations.conversation(actor,conversationId); return Api.ok(db.page("chat_message","conversation_id=?",List.of(conversationId),page,size));
    }
    /** 分页查询当前账号的站内通知。 */
    public Api<?> notifications(Actor actor,int page,int size) { return Api.ok(db.page("notification","user_id=?",List.of(actor.id()),page,size)); }
    /** 标记当前账号全部通知已读。 */
    public Api<?> readAll(Actor actor) { db.update("update notification set read_at=now() where user_id=? and read_at is null",actor.id()); return Api.ok(true); }
    /** 标记自己的通知已读。 */
    public Api<?> read(Actor actor,long id) { db.update("update notification set read_at=now() where user_id=? and id=?",actor.id(),id); return Api.ok(true); }
}
