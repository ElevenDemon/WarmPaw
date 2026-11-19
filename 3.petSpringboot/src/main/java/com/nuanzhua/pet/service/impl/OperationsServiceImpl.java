package com.nuanzhua.pet.service.impl;
import com.nuanzhua.pet.common.BusinessSupport;
import com.nuanzhua.pet.common.Db;
import com.nuanzhua.pet.dto.OperationsRequests;
import com.nuanzhua.pet.exception.BusinessException;
import com.nuanzhua.pet.service.OperationsService;
import com.nuanzhua.pet.vo.Actor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** 业务服务实现；写操作在服务事务内完成，数据库约束提供最后一致性保护。 */
@Service
public class OperationsServiceImpl implements OperationsService {
    private final Db db; private final BusinessSupport support;
    public OperationsServiceImpl(Db db,BusinessSupport support) { this.db=db; this.support=support; }
    /** 创建应急工单并向宠主和运营人员发送站内提醒。 */
    @Transactional public long emergency(Actor actor,long orderId,String description) {
        var o=support.order(orderId,actor,false);
        long id=db.id("insert into emergency_ticket(order_id,reporter_id,description) values (?,?,?) returning id",orderId,actor.id(),description);
        support.notify(Db.number(o,"owner_id"),orderId,"EMERGENCY","服务异常 / 应急请求",description);
        for(long staff:db.queryForList("select id from app_user where role in ('ADMIN','SUPPORT') and status='ACTIVE'",Long.class)) support.notify(staff,orderId,"EMERGENCY","应急工单待处理",description);
        support.event(orderId,actor,"EMERGENCY",Map.of("ticketId",id,"description",description)); return id;
    }
    /** 推进应急工单；送医前必须有宠主同意和目标医院。 */
    @Transactional public void updateEmergency(Actor actor,long id,OperationsRequests.EmergencyUpdate r) {
        actor.require("emergency:update"); var before=db.one("select * from emergency_ticket where id=? for update",id);
        if("RESOLVED".equals(before.get("status"))) throw BusinessException.conflict("已关闭的应急工单不能重复处理");
        var allowed=switch(before.get("status").toString()) {
            case "OPEN" -> Set.of("ACCEPTED","RESOLVED");
            case "ACCEPTED" -> Set.of("ACCEPTED","TRANSPORTING","RESOLVED");
            case "TRANSPORTING" -> Set.of("TRANSPORTING","RESOLVED"); default -> Set.<String>of();
        };
        if(!allowed.contains(r.status())) throw BusinessException.conflict("应急工单状态流转无效");
        if("TRANSPORTING".equals(r.status()) && (!Boolean.TRUE.equals(before.get("owner_consent")) || r.hospitalId()==null)) throw BusinessException.bad("送医须先取得宠主确认并选择医院");
        if("RESOLVED".equals(r.status()) && (r.resolution()==null || r.resolution().isBlank())) throw BusinessException.bad("关闭工单须填写复盘结果");
        if(r.hospitalId()!=null) db.one("select id from hospital where id=? and active",r.hospitalId());
        if(r.sitterId()!=null) db.one("select id from sitter_profile where id=? and status='ACTIVE'",r.sitterId());
        db.update("update emergency_ticket set status=?,sitter_id=?,hospital_id=?,resolution=?,updated_at=now() where id=?",r.status(),r.sitterId(),r.hospitalId(),r.resolution(),id);
        support.log(actor,"处理应急工单","emergency",id,before,r);
    }
    /** 创建关联订单的投诉工单。 */
    @Transactional public long complaint(Actor actor,long orderId,String content) {
        actor.role("OWNER"); support.order(orderId,actor,false);
        return db.id("insert into complaint(order_id,owner_id,content) values (?,?,?) returning id",orderId,actor.id(),content);
    }
    /** 查询或创建客服会话并校验归属。 */
    public Map<String,Object> conversation(Actor actor,long id) {
        var c=db.one("select * from support_conversation where id=?",id);
        if(actor.staff()) actor.require("service:read");
        else if(Db.number(c,"owner_id")!=actor.id()) throw BusinessException.forbidden();
        return c;
    }
    /** 发送客服或订单消息；人工回复前必须接管会话。 */
    @Transactional public long send(Actor actor,OperationsRequests.Message r) {
        if((r.orderId()==null)==(r.conversationId()==null)) throw BusinessException.bad("orderId 与 conversationId 必须且只能提供一个");
        if(r.orderId()!=null) support.order(r.orderId(),actor,false);
        else {
            var c=conversation(actor,r.conversationId());
            if("CLOSED".equals(c.get("status"))) throw BusinessException.conflict("会话已关闭");
            if(actor.staff()) {
                actor.require("service:update");
                if(c.get("agent_id")==null || Db.number(c,"agent_id")!=actor.id()) throw BusinessException.bad("请先接管会话后回复");
            }
        }
        return db.id("insert into chat_message(conversation_id,order_id,sender_id,sender_type,content,media_keys) values (?,?,?,'USER',?,?::jsonb) returning id",r.conversationId(),r.orderId(),actor.id(),r.content(),db.json(r.mediaKeys()==null?List.of():r.mediaKeys()));
    }
}
