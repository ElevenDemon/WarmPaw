package com.nuanzhua.pet.service;
import com.nuanzhua.pet.dto.OrderRequests;
import com.nuanzhua.pet.vo.Actor;

import java.time.*;
import java.util.*;
import static com.nuanzhua.pet.common.Db.number;

/** 业务服务契约；调用方通过接口依赖具体实现。 */
public interface DispatchService {
    List<String> violations(Map<String,Object> o,long sitterId);
    /** 使用Haversine公式计算球面距离，返回公里。 */
    public static double distance(double lon1,double lat1,double lon2,double lat2) {
        double a=Math.pow(Math.sin(Math.toRadians(lat2-lat1)/2),2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.pow(Math.sin(Math.toRadians(lon2-lon1)/2),2);
        return 6371*2*Math.atan2(Math.sqrt(a),Math.sqrt(Math.max(0,1-a)));
    }
    void assign(long orderId,Actor actor,OrderRequests.Assign r);
    List<Map<String,Object>> candidates(long orderId,Actor actor);
    Map<String,Object> autoAssign(long orderId,Actor actor);
    Map<String,Object> llmAssign(long orderId,Actor actor);
    void dispatchPendingOrders();
    void expire();
}
