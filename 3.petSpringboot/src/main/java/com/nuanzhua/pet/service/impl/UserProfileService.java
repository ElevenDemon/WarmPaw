package com.nuanzhua.pet.service.impl;

import com.nuanzhua.pet.common.Db;
import com.nuanzhua.pet.exception.BusinessException;
import com.nuanzhua.pet.vo.Actor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class UserProfileService {
    private final Db db;
    private final Path root;
    public UserProfileService(Db db, @Value("${app.media-root:uploads}") String root) {
        this.db=db; this.root=Path.of(root).toAbsolutePath().normalize();
    }
    public Map<String,Object> profile(Actor actor) {
        return db.one("select id,username,name,phone,role,avatar_key is not null as has_avatar from app_user where id=?",actor.id());
    }
    @Transactional public Map<String,Object> update(Actor actor,String name,String phone) {
        if(db.id("select count(*) from app_user where phone=? and id<>?",phone,actor.id())>0) throw BusinessException.conflict("手机号已被使用");
        db.update("update app_user set name=?,phone=? where id=?",name.trim(),phone,actor.id());
        return profile(actor);
    }
    @Transactional public Map<String,Object> upload(Actor actor,MultipartFile file) throws IOException {
        if(file.isEmpty() || file.getSize()>5L*1024*1024) throw BusinessException.bad("头像大小须小于5MB");
        byte[] h; try(var in=file.getInputStream()){h=in.readNBytes(8);}
        String ext;
        if(h.length>=3 && (h[0]&255)==255 && (h[1]&255)==216 && (h[2]&255)==255) ext="jpg";
        else if(h.length>=8 && Arrays.equals(h,new byte[]{(byte)137,80,78,71,13,10,26,10})) ext="png";
        else throw BusinessException.bad("头像仅支持JPEG或PNG");
        String key="avatars/"+actor.id()+"/"+UUID.randomUUID()+"."+ext;
        Path target=root.resolve(key); Files.createDirectories(target.getParent());
        try(var in=file.getInputStream()){Files.copy(in,target);}
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            public void afterCompletion(int status) { if(status!=STATUS_COMMITTED) try { Files.deleteIfExists(target); } catch(IOException ignored) {} }
        });
        db.update("update app_user set avatar_key=? where id=?",key,actor.id());
        return profile(actor);
    }
    public Resource avatar(Actor actor) throws IOException {
        Object key=db.one("select avatar_key from app_user where id=?",actor.id()).get("avatar_key");
        if(key==null) throw BusinessException.bad("尚未设置头像");
        Path path=root.resolve(key.toString()).normalize();
        if(!path.startsWith(root) || !Files.isRegularFile(path) || !path.toRealPath().startsWith(root.toRealPath())) throw BusinessException.bad("头像不可用");
        return new FileSystemResource(path);
    }
}
