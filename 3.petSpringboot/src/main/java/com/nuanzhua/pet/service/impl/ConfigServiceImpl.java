package com.nuanzhua.pet.service.impl;
import com.nuanzhua.pet.common.BusinessSupport;
import com.nuanzhua.pet.common.Db;
import com.nuanzhua.pet.exception.BusinessException;
import com.nuanzhua.pet.service.ConfigService;
import com.nuanzhua.pet.vo.Actor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** 业务服务实现；写操作在服务事务内完成，数据库约束提供最后一致性保护。 */
@Service
public class ConfigServiceImpl implements ConfigService {
    private final Db db; private final BusinessSupport support;
    public ConfigServiceImpl(Db db,BusinessSupport support) { this.db=db; this.support=support; }
    /** 仅由类型明确的控制器提供静态表/列名，绝不接受客户端SQL标识符。 */
    /** 事务保存业务配置或档案，并记录变更审计。 */
    @Transactional public long save(Actor actor,String permission,String table,Long id,LinkedHashMap<String,Object> values,Set<String> jsonColumns) {
        actor.require(permission);
        Object before=id==null?null:db.one("select * from "+table+" where id=? for update",id);
        var columns=new ArrayList<>(values.keySet()); var args=new ArrayList<>(values.values());
        String sql;
        if(id==null) {
            sql="insert into "+table+" ("+String.join(",",columns)+") values ("+String.join(",",columns.stream().map(x->jsonColumns.contains(x)?"?::jsonb":"?").toList())+") returning id";
            id=db.id(sql,args.toArray());
        } else {
            args.add(id);
            sql="update "+table+" set "+String.join(",",columns.stream().map(x->x+"="+(jsonColumns.contains(x)?"?::jsonb":"?")).toList())+" where id=?";
            db.update(sql,args.toArray());
        }
        support.log(actor,"保存配置",table,id,before,db.one("select * from "+table+" where id=?",id)); return id;
    }
    /** 按声明顺序构造受控列和值映射。 */
    public static LinkedHashMap<String,Object> values(Object... pairs) {
        var m=new LinkedHashMap<String,Object>(); for(int i=0;i<pairs.length;i+=2) m.put((String)pairs[i],pairs[i+1]); return m;
    }
    /** 批量结算，锁定订单后锁定结算，防止与退款并发冲突。 */
    @Transactional public void settle(Actor actor,List<Long> ids) {
        actor.require("finance:settle");
        // 退款与结算都先锁订单，再锁结算，避免竞态及死锁。
        for(long id:new TreeSet<>(ids)) {
            long orderId=db.id("select order_id from settlement where id=?",id);
            var order=db.one("select status from pet_order where id=? for update",orderId);
            var before=db.one("select * from settlement where id=? for update",id);
            if("SETTLED".equals(before.get("status"))) continue;
            if(!"COMPLETED".equals(order.get("status")) || !"PENDING".equals(before.get("status"))) throw BusinessException.conflict("仅已完成且未结算的订单可以结算");
            db.update("update settlement set status='SETTLED',settled_at=now() where id=?",id);
            support.log(actor,"标记结算","settlement",id,before,Map.of("status","SETTLED"));
        }
    }
    /** 更新受控角色权限集合。 */
    @Transactional public void permissions(Actor actor,String role,Set<String> permissions) {
        actor.require("system:update");
        if(!Set.of("SUPPORT","FINANCE").contains(role)) throw BusinessException.bad("只能配置客服与财务角色，管理员权限不可降级");
        var before=db.queryForList("select permission from role_permission where role=?",String.class,role);
        db.update("delete from role_permission where role=?",role);
        for(String p:permissions) db.update("insert into role_permission(role,permission) values (?,?)",role,p);
        support.log(actor,"修改角色权限","role",null,before,Map.of("role",role,"permissions",permissions));
    }
}
