package com.nuanzhua.pet.common;
import com.nuanzhua.pet.exception.BusinessException;
import com.nuanzhua.pet.vo.Actor;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class BusinessSupport {
    private final Db db;
    public BusinessSupport(Db db) { this.db=db; }
    /** 记录业务审计快照，过滤账号密码摘要。 */
    public void log(Actor actor,String action,String resource,Long id,Object before,Object after) {
        if (before instanceof Map<?,?> m && m.containsKey("password_hash")) {
            var safe=new LinkedHashMap<>(m); safe.remove("password_hash"); before=safe;
        }
        if (after instanceof Map<?,?> m && m.containsKey("password_hash")) {
            var safe=new LinkedHashMap<>(m); safe.remove("password_hash"); after=safe;
        }
        db.update("insert into operation_log(actor_id,action,resource,resource_id,before_data,after_data) values (?,?,?,?,?::jsonb,?::jsonb)",
            actor==null?null:actor.id(),action,resource,id,db.json(before),db.json(after));
    }
    /** 记录订单状态及操作事件，随业务事务提交。 */
    public void event(long orderId,Actor actor,String event,Object data) {
        db.update("insert into order_event(order_id,actor_id,event,detail) values (?,?,?,?::jsonb)",orderId,actor==null?null:actor.id(),event,db.json(data));
    }
    /** 写入站内通知；与上层业务事务保持一致，不发送外部短信或微信。 */
    public void notify(long userId,Long orderId,String code,String title,String content) {
        // 安全类提醒不允许通过配置关停；外部投递尚未接入，站内通知与业务事务同时提交。
        if(!Set.of("CHECKIN","CHECKOUT","EMERGENCY").contains(code)
            && db.id("select count(*) from notification_template where code=? and enabled=false",code)>0) return;
        db.update("insert into notification(user_id,order_id,code,title,content) values (?,?,?,?,?)",userId,orderId,code,title,content);
    }
    /** 查询订单并校验宠主、喂养师或后台权限，可选择加行锁。 */
    public Map<String,Object> order(long id,Actor actor,boolean lock) {
        var order=db.one("select * from pet_order where id=?"+(lock?" for update":""),id);
        if(actor.staff()) actor.require("order:read");
        else if("OWNER".equals(actor.role())) {
            if(Db.number(order,"owner_id")!=actor.id()) throw BusinessException.forbidden();
        } else if("SITTER".equals(actor.role())) {
            if(order.get("sitter_id")==null || db.id("select count(*) from sitter_profile where id=? and user_id=?",order.get("sitter_id"),actor.id())==0) throw BusinessException.forbidden();
        } else throw BusinessException.forbidden();
        return order;
    }
    /** 断言订单处于允许的业务状态。 */
    public static void state(Map<String,Object> order,String... allowed) {
        if(!Arrays.asList(allowed).contains(order.get("status"))) throw BusinessException.conflict("当前状态不允许该操作："+order.get("status"));
    }
}
