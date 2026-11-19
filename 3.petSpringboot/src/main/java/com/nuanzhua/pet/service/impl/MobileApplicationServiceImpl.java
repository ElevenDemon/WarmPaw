package com.nuanzhua.pet.service.impl;

import com.nuanzhua.pet.common.Db;
import com.nuanzhua.pet.service.MobileApplicationService;
import com.nuanzhua.pet.vo.Actor;
import org.springframework.stereotype.Service;
import java.util.*;

/** 宠主和喂养师只可查询自己参与订单的应急记录和个人收益。 */
@Service
public class MobileApplicationServiceImpl implements MobileApplicationService {
    private final Db db;
    public MobileApplicationServiceImpl(Db db) { this.db=db; }
    public Map<String,Object> emergencies(Actor actor,int page,int size) {
        String where;
        if("OWNER".equals(actor.role())) where="order_id in (select id from pet_order where owner_id=?)";
        else { actor.role("SITTER"); where="order_id in (select o.id from pet_order o join sitter_profile s on s.id=o.sitter_id where s.user_id=?)"; }
        return db.page("emergency_ticket",where,List.of(actor.id()),page,size);
    }
    public Map<String,Object> settlements(Actor actor,int page,int size) {
        actor.role("SITTER");
        return db.page("settlement","sitter_id in (select id from sitter_profile where user_id=?)",List.of(actor.id()),page,size);
    }
}
