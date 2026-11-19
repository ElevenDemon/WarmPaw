package com.nuanzhua.pet.config;
import com.nuanzhua.pet.common.Db;
import com.nuanzhua.pet.service.AuthService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 首次启动初始化默认超级管理员；已有同名账号不覆盖密码或权限。 */
@Component
public class Bootstrap implements ApplicationRunner {
    private final Db db; private final AuthService auth; private final String password; private final boolean demo;
    public Bootstrap(Db db,AuthService auth,@Value("${app.bootstrap-password}") String password,@Value("${app.demo-enabled}") boolean demo) {
        this.db=db; this.auth=auth; this.password=password; this.demo=demo;
    }
    /** 创建拥有ADMIN全部权限的默认账号，演示账号需显式启用。 */
    @Override @Transactional public void run(ApplicationArguments args) {
        if(!password.isBlank()) {
            if(password.length()<8 || password.length()>64) throw new IllegalArgumentException("BOOTSTRAP_PASSWORD 长度须为8–64");
            db.update("insert into app_user(username,password_hash,name,role) values (?,?,?,'ADMIN') on conflict(username) do nothing","admin",auth.encodePassword(password),"超级管理员");
        }
        if(demo) {
            if(password.isBlank()) throw new IllegalArgumentException("演示模式需设置 BOOTSTRAP_PASSWORD，演示账号共用该初始密码");
            db.update("insert into app_user(username,password_hash,name,phone,role) values (?,?,?,?,'OWNER') on conflict(username) do nothing","owner",auth.encodePassword(password),"演示宠主","13800000001");
            db.update("insert into app_user(username,password_hash,name,phone,role) values (?,?,?,?,'SITTER') on conflict(username) do nothing","sitter",auth.encodePassword(password),"演示喂养师","13800000002");
            db.update("insert into sitter_profile(user_id,code,grade_id,status,district,address,longitude,latitude,categories) select u.id,'S1001',g.id,'ACTIVE','南山区','科技园',113.950000,22.550000,'[\"CAT\",\"DOG\",\"SMALL\",\"OTHER\"]'::jsonb from app_user u,sitter_grade g where u.username='sitter' and g.code='L2' on conflict(user_id) do nothing");
            db.update("insert into sitter_schedule(sitter_id,weekday,start_time,end_time,capacity) select s.id,d,'08:00','22:00',10 from sitter_profile s,generate_series(1,7) d where s.code='S1001' on conflict(sitter_id,weekday) do nothing");
            db.update("insert into sitter_qualification(sitter_id,category,real_verified,training_passed,practice_passed,status,expires_at) select s.id,c,true,true,true,'APPROVED',now()+interval '1 year' from sitter_profile s,unnest(ARRAY['CAT','DOG','SMALL','OTHER','WALK','MEDICINE','DEWORM']) c where s.code='S1001' on conflict(sitter_id,category) do nothing");
        }
    }
}
