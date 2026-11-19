package com.nuanzhua.pet.service.impl;
import com.nuanzhua.pet.service.OwnerApplicationService;
import org.springframework.stereotype.Service;
import com.nuanzhua.pet.dto.OwnerRequests.*;
import com.nuanzhua.pet.common.Db;

import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;

import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** 业务服务实现；写操作在服务事务内完成，数据库约束提供最后一致性保护。 */
@Service
@org.springframework.transaction.annotation.Transactional
public class OwnerApplicationServiceImpl implements OwnerApplicationService {
    private final Db db;
    public OwnerApplicationServiceImpl(Db db) { this.db=db; }

    /** 查询当前宠主未删除的宠物。 */
    public Api<?> pets(Actor actor) { actor.role("OWNER"); return Api.ok(db.queryForList("select * from pet where owner_id=? and not deleted order by id",actor.id())); }
    /** 新增当前宠主的宠物档案。 */
    public Api<?> addPet(Actor actor,Pet r) {
        actor.role("OWNER"); return Api.ok(db.id("insert into pet(owner_id,name,category,breed,birthday,weight,temperament,dietary_notes,medical_history,photo_url) values (?,?,?,?,?,?,?,?,?,?) returning id",actor.id(),r.name(),r.category(),r.breed(),r.birthday(),r.weight(),r.temperament(),r.dietaryNotes(),r.medicalHistory(),r.photoUrl()));
    }
    /** 更新当前宠主自己的宠物档案。 */
    public Api<?> updatePet(Actor actor,long id,Pet r) {
        actor.role("OWNER"); db.one("select id from pet where id=? and owner_id=? and not deleted",id,actor.id());
        db.update("update pet set name=?,category=?,breed=?,birthday=?,weight=?,temperament=?,dietary_notes=?,medical_history=?,photo_url=?,updated_at=now() where id=? and owner_id=?",r.name(),r.category(),r.breed(),r.birthday(),r.weight(),r.temperament(),r.dietaryNotes(),r.medicalHistory(),r.photoUrl(),id,actor.id()); return Api.ok(true);
    }
    /** 软删除当前宠主的宠物档案。 */
    public Api<?> deletePet(Actor actor,long id) {
        actor.role("OWNER"); db.update("update pet set deleted=true where id=? and owner_id=?",id,actor.id()); return Api.ok(true);
    }
    /** 查询当前宠主的地址簿。 */
    public Api<?> addresses(Actor actor) { actor.role("OWNER"); return Api.ok(db.queryForList("select * from owner_address where owner_id=? order by id",actor.id())); }
    /** 新增当前宠主的服务地址。 */
    public Api<?> addAddress(Actor actor,Address r) {
        actor.role("OWNER"); return Api.ok(db.id("insert into owner_address(owner_id,contact_name,phone,district,address,longitude,latitude) values (?,?,?,?,?,?,?) returning id",actor.id(),r.contactName(),r.phone(),r.district(),r.address(),r.longitude(),r.latitude()));
    }
    /** 更新当前宠主自己的地址。 */
    public Api<?> updateAddress(Actor actor,long id,Address r) {
        actor.role("OWNER"); db.one("select id from owner_address where id=? and owner_id=?",id,actor.id());
        db.update("update owner_address set contact_name=?,phone=?,district=?,address=?,longitude=?,latitude=? where id=? and owner_id=?",r.contactName(),r.phone(),r.district(),r.address(),r.longitude(),r.latitude(),id,actor.id()); return Api.ok(true);
    }
}
