package com.nuanzhua.pet.service;
import com.nuanzhua.pet.vo.Actor;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** 业务服务契约；调用方通过接口依赖具体实现。 */
public interface AuthService {
    String encodePassword(String password);
    /** 计算SHA-256摘要，用于令牌和风控标识，避免保存明文。 */
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    Actor authenticate(String token);
    boolean hasSitterProfile(long userId);
    void switchRole(String token,String role);
    Map<String,Object> login(String username,String password);
    long register(String username,String password,String name,String phone);
    void logout(String token);
}
