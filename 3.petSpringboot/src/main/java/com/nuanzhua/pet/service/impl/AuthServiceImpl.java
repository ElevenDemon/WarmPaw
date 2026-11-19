package com.nuanzhua.pet.service.impl;
import com.nuanzhua.pet.common.Db;
import com.nuanzhua.pet.entity.AppUser;
import com.nuanzhua.pet.exception.BusinessException;
import com.nuanzhua.pet.mapper.AppUserMapper;
import com.nuanzhua.pet.service.AuthService;
import com.nuanzhua.pet.vo.Actor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.OffsetDateTime;
import java.util.*;

/** 业务服务实现；写操作在服务事务内完成，数据库约束提供最后一致性保护。 */
@Service
public class AuthServiceImpl implements AuthService {
    private final Db db;
    private final int hours;
    private final AppUserMapper users;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    public AuthServiceImpl(Db db, AppUserMapper users, @Value("${app.token-hours}") int hours) { this.db=db; this.users=users; this.hours=hours; }
    /** 使用BCrypt生成密码摘要。 */
    public String encodePassword(String password) { return encoder.encode(password); }
    /** 计算SHA-256摘要，用于令牌和风控标识，避免保存明文。 */
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    /** 根据令牌摘要查询有效会话，并重新加载账号状态和权限。 */
    public Actor authenticate(String token) {
        var rows=db.queryForList("select u.id,u.name,coalesce(s.active_role,u.role) as role from auth_session s join app_user u on u.id=s.user_id where s.token_hash=? and s.expires_at>now() and u.status='ACTIVE'",hash(token));
        if(rows.isEmpty()) throw new BusinessException(HttpStatus.UNAUTHORIZED,"登录已失效");
        var u=rows.getFirst(); String role=(String)u.get("role");
        return new Actor(Db.number(u,"id"),(String)u.get("name"),role,new HashSet<>(db.queryForList("select permission from role_permission where role=?",String.class,role)));
    }
    public boolean hasSitterProfile(long userId) { return db.id("select count(*) from sitter_profile where user_id=?",userId)>0; }
    @Transactional public void switchRole(String token,String role) {
        if(!Set.of("OWNER","SITTER").contains(role)) throw BusinessException.forbidden();
        if(db.update("update auth_session set active_role=? where token_hash=? and expires_at>now()",role,hash(token))!=1)
            throw new BusinessException(HttpStatus.UNAUTHORIZED,"登录已失效");
    }
    @Transactional
    /** 校验账号密码并生成有时效的随机Bearer会话。 */
    public Map<String,Object> login(String username,String password) {
        var rows=db.queryForList("select * from app_user where username=?",username);
        if(rows.isEmpty() || !encoder.matches(password,(String)rows.getFirst().get("password_hash")))
            throw new BusinessException(HttpStatus.UNAUTHORIZED,"账号或密码错误");
        var u=rows.getFirst();
        if(!"ACTIVE".equals(u.get("status"))) throw BusinessException.forbidden();
        byte[] bytes=new byte[32]; new SecureRandom().nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var expiry=OffsetDateTime.now().plusHours(hours);
        db.update("insert into auth_session(user_id,token_hash,expires_at) values (?,?,?)",u.get("id"),hash(token),expiry);
        return Map.of("token",token,"expiresAt",expiry,"user",authenticate(token));
    }
    @Transactional
    /** 注册宠主账号，使用BaseMapper持久化并校验账号和手机重复。 */
    public long register(String username,String password,String name,String phone) {
        if(users.exists(new QueryWrapper<AppUser>().eq("username",username).or().eq("phone",phone)))
            throw BusinessException.conflict("账号或手机号已注册");
        var user=new AppUser(); user.username=username; user.passwordHash=encoder.encode(password);
        user.name=name; user.phone=phone; user.role="OWNER";
        users.insert(user); return user.id;
    }
    /** 删除当前登录会话，令牌立即失效。 */
    public void logout(String token) { db.update("delete from auth_session where token_hash=?",hash(token)); }
}
