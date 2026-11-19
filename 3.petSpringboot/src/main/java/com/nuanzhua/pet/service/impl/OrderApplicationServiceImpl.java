package com.nuanzhua.pet.service.impl;
import com.nuanzhua.pet.service.OrderApplicationService;
import org.springframework.stereotype.Service;
import com.nuanzhua.pet.common.BusinessSupport;
import com.nuanzhua.pet.common.Db;
import com.nuanzhua.pet.dto.OrderRequests;
import com.nuanzhua.pet.entity.Review;
import com.nuanzhua.pet.exception.BusinessException;
import com.nuanzhua.pet.service.DispatchService;
import com.nuanzhua.pet.service.OrderService;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** 业务服务实现；写操作在服务事务内完成，数据库约束提供最后一致性保护。 */
@Service
@org.springframework.transaction.annotation.Transactional
public class OrderApplicationServiceImpl implements OrderApplicationService {
    private final Db db; private final BusinessSupport support; private final OrderService orders; private final DispatchService dispatch; private final boolean demo;
    public OrderApplicationServiceImpl(Db db,BusinessSupport support,OrderService orders,DispatchService dispatch,@Value("${app.demo-enabled}") boolean demo) {
        this.db=db; this.support=support; this.orders=orders; this.dispatch=dispatch; this.demo=demo;
    }
    /** 创建订单；按服务端价格计算，冻结协议和价格快照，requestKey保证幂等。 */
    public Api<?> create(Actor actor,OrderRequests.Create r) { return Api.ok(orders.create(actor,r)); }
    /** 分页查询业务列表，表名与过滤列由服务端白名单提供。 */
    public Api<?> list(Actor actor,int page,int size,String status) {
        String where="true"; var args=new ArrayList<Object>();
        if(actor.staff()) actor.require("order:read");
        else if("OWNER".equals(actor.role())) { where+=" and owner_id=?"; args.add(actor.id()); }
        else { actor.role("SITTER"); where+=" and sitter_id=(select id from sitter_profile where user_id=?)"; args.add(actor.id()); }
        if(status!=null && !status.isBlank()) { where+=" and status=?"; args.add(status); }
        return Api.ok(db.page("pet_order",where,args,page,size));
    }
    /** 查询业务详情，并校验访问权限。 */
    public Api<?> detail(Actor actor,long id) {
        var order=support.order(id,actor,false);
        order.put("items",db.queryForList("select * from order_item where order_id=?",id));
        order.put("timeline",db.queryForList("select * from order_event where order_id=? order by id",id));
        order.put("checklist",db.queryForList("select * from service_checklist where order_id=? order by id",id));
        if(order.get("sitter_id")!=null) {
            order.put("sitter",db.one("select u.name,s.rating,s.completed_orders,g.code as grade_code from sitter_profile s join app_user u on u.id=s.user_id join sitter_grade g on g.id=s.grade_id where s.id=?",order.get("sitter_id")));
        }
        var records=db.queryForList("select reason,mode,status,created_at from dispatch_record where order_id=? order by id desc limit 1",id);
        if(!records.isEmpty()) order.put("dispatch",records.getFirst());
        var reviews=db.queryForList("select score,tags,content,created_at from review where order_id=?",id);
        if(!reviews.isEmpty()) order.put("review",reviews.getFirst());
        if("SITTER".equals(actor.role()) || actor.staff()) {
            var settlements=db.queryForList("select sitter_amount,commission_rate,status from settlement where order_id=?",id);
            if(!settlements.isEmpty()) order.put("settlement",settlements.getFirst());
        }
        return Api.ok(order);
    }
    /** 取消待付款订单并释放锁定优惠券。 */
    public Api<?> cancel(Actor actor,long id,OrderRequests.Reason r) { orders.cancel(id,actor,r.reason()); return Api.ok(true); }
    /** 本地演示支付入口；必须开启DEMO_ENABLED。 */
    public Api<?> pay(Actor actor,long id) {
        if(!demo) throw new BusinessException(HttpStatus.NOT_IMPLEMENTED,"演示支付未启用，微信支付待配置");
        orders.payDemo(id,actor); return Api.ok(true);
    }
    /** 人工派单；校验档期、半径、容量与资质，按订单再喂养师顺序加锁。 */
    public Api<?> assign(Actor actor,long id,OrderRequests.Assign r) { dispatch.assign(id,actor,r); return Api.ok(true); }
    /** 返回人工派单准入结果及所有淘汰原因，不执行AI排序。 */
    public Api<?> eligibility(Actor actor,long id,long sitterId) {
        actor.require("dispatch:update"); var o=support.order(id,actor,false); var reasons=dispatch.violations(o,sitterId);
        return Api.ok(Map.of("eligible",reasons.isEmpty(),"reasons",reasons,"mode","MANUAL_VALIDATION"));
    }
    /** 喂养师确认接单；必须在接单期限内操作。 */
    public Api<?> accept(Actor actor,long id) { orders.respond(id,actor,true); return Api.ok(true); }
    /** 喂养师拒单，退回人工待派单。 */
    public Api<?> reject(Actor actor,long id) { orders.respond(id,actor,false); return Api.ok(true); }
    /** 喂养师进门打卡；创建标准清单并通知宠主。 */
    public Api<?> checkin(Actor actor,long id) { orders.checkin(id,actor); return Api.ok(true); }
    /** 保存标准服务清单；陪玩至少10分钟且单位为MINUTE。 */
    public Api<?> checklist(Actor actor,long id,OrderRequests.Checklist r) { orders.checklist(id,actor,r); return Api.ok(true); }
    /** 登记服务媒体元数据，不处理文件上传或生成播放地址。 */
    public Api<?> media(Actor actor,long id,OrderRequests.Media r) { return Api.ok(orders.media(id,actor,r)); }
    /** 查询未删除且未过期的订单媒体元数据。 */
    public Api<?> mediaList(Actor actor,long id) {
        support.order(id,actor,false); return Api.ok(db.queryForList("select id,kind,object_key,expires_at from service_media where order_id=? and not deleted and expires_at>now()",id));
    }
    /** 软删除指定订单媒体；仅限有权限的订单参与人。 */
    public Api<?> deleteMedia(Actor actor,long id,long mediaId) {
        var o=support.order(id,actor,false);
        if(!"ADMIN".equals(actor.role()) && !("OWNER".equals(actor.role()) && Db.number(o,"owner_id")==actor.id())) throw BusinessException.forbidden();
        db.update("update service_media set deleted=true where id=? and order_id=?",mediaId,id); return Api.ok(true);
    }
    /** 完成服务；校验清单和录像，原子创建待处理报告、结算及离门通知。 */
    public Api<?> complete(Actor actor,long id) { orders.complete(id,actor); return Api.ok(true); }
    /** 读取订单报告；当前仅返回原始快照及PENDING_AI状态。 */
    public Api<?> report(Actor actor,long id) {
        support.order(id,actor,false); return Api.ok(db.one("select * from feeding_report where order_id=?",id));
    }
    /** 订单完成后评价，单订单只允许一次并更新综合评分。 */
    public Api<?> review(Actor actor,long id,OrderRequests.Review r) { return Api.ok(orders.review(id,actor,r)); }
    /** 本地演示退款入口；需要财务退款权限。 */
    public Api<?> refund(Actor actor,long id,OrderRequests.Reason r) {
        if(!demo) throw new BusinessException(HttpStatus.NOT_IMPLEMENTED,"演示退款未启用"); orders.refundDemo(id,actor,r.reason()); return Api.ok(true);
    }
    /** AI功能预留；当前返回HTTP 501，后续由Python实现。 */
    public Api<?> ai() {
        throw new BusinessException(HttpStatus.NOT_IMPLEMENTED,"AI功能暂未实现，后续由Python服务提供");
    }
}
