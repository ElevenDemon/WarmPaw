package com.nuanzhua.pet.controller;

import com.nuanzhua.pet.service.impl.MediaStorageService;
import com.nuanzhua.pet.vo.*;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;

/** 服务媒体上传与受保护下载，适配uni.uploadFile/uni.downloadFile。 */
@RestController @RequestMapping("/api/orders/{id}") @Tag(name="服务媒体文件")
public class MediaController {
    private final MediaStorageService storage;
    public MediaController(MediaStorageService storage) { this.storage=storage; }
    @PostMapping(value="/upload",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @Operation(summary="上传当前服务的照片或录像，最大100MB")
    public Api<?> upload(@RequestAttribute Actor actor,@PathVariable long id,@RequestPart MultipartFile file) throws IOException { return Api.ok(storage.upload(actor,id,file)); }
    @GetMapping("/media/{mediaId}/file") @Operation(summary="鉴权读取订单媒体文件，支持视频范围请求")
    public ResponseEntity<Resource> file(@RequestAttribute Actor actor,@PathVariable long id,@PathVariable long mediaId) throws IOException {
        Resource file=storage.file(actor,id,mediaId);String name=file.getFilename();
        MediaType type=name.endsWith(".mp4")?MediaType.valueOf("video/mp4"):name.endsWith(".png")?MediaType.IMAGE_PNG:MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).header("X-Content-Type-Options","nosniff").header("Cache-Control","private, no-store").body(file);
    }
}
