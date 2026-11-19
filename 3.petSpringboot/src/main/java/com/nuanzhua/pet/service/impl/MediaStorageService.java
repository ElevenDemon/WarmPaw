package com.nuanzhua.pet.service.impl;

import com.nuanzhua.pet.common.*;
import com.nuanzhua.pet.dto.OrderRequests;
import com.nuanzhua.pet.exception.BusinessException;
import com.nuanzhua.pet.service.OrderService;
import com.nuanzhua.pet.vo.Actor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static com.nuanzhua.pet.common.BusinessSupport.state;

/** 本地持久化媒体适配器；文件不暴露为静态目录，读取需订单鉴权。 */
@Service
public class MediaStorageService {
    private final Path root; private final Db db; private final BusinessSupport support; private final OrderService orders;
    public MediaStorageService(Db db,BusinessSupport support,OrderService orders,@Value("${app.media-root:uploads}") String root) { this.db=db;this.support=support;this.orders=orders;this.root=Path.of(root).toAbsolutePath().normalize(); }
    /** 只接受实际签名匹配的JPEG、PNG、MP4文件；事务回滚时删除新写入文件。 */
    @Transactional public Map<String,Object> upload(Actor actor,long orderId,MultipartFile file) throws IOException {
        actor.role("SITTER"); state(support.order(orderId,actor,true),"SERVICING");
        if(file.isEmpty() || file.getSize()>100L*1024*1024) throw BusinessException.bad("文件须为1字节至100MB");
        byte[] header; try(var input=file.getInputStream()) { header=input.readNBytes(16); }
        String ext;
        if(header.length>=3 && (header[0]&255)==255 && (header[1]&255)==216 && (header[2]&255)==255) ext="jpg";
        else if(header.length>=8 && Arrays.equals(Arrays.copyOf(header,8),new byte[]{(byte)137,80,78,71,13,10,26,10})) ext="png";
        else if(header.length>=12 && new String(header,4,4,java.nio.charset.StandardCharsets.US_ASCII).equals("ftyp")) ext="mp4";
        else throw BusinessException.bad("仅支持JPEG、PNG照片或MP4录像");
        String key="orders/"+orderId+"/"+UUID.randomUUID()+"."+ext;
        Path target=root.resolve(key).normalize(); Files.createDirectories(target.getParent());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            public void afterCompletion(int status) { if(status!=STATUS_COMMITTED) try {Files.deleteIfExists(target);}catch(IOException ignored){} }
        });
        try(var input=file.getInputStream()) { Files.copy(input,target); }
        long id=orders.media(orderId,actor,new OrderRequests.Media("mp4".equals(ext)?"VIDEO":"PHOTO",key));
        return Map.of("id",id,"kind","mp4".equals(ext)?"VIDEO":"PHOTO","objectKey",key);
    }
    /** 校验参与权限、过期和软删除状态，禁止路径穿越及符号链接逃逸。 */
    public Resource file(Actor actor,long orderId,long mediaId) throws IOException {
        support.order(orderId,actor,false);
        var media=db.one("select object_key from service_media where id=? and order_id=? and not deleted and expires_at>now()",mediaId,orderId);
        Path target=root.resolve(media.get("object_key").toString()).normalize();
        if(!target.startsWith(root) || !Files.isRegularFile(target) || !target.toRealPath().startsWith(root.toRealPath())) throw BusinessException.bad("媒体文件不可用");
        return new FileSystemResource(target);
    }
}
