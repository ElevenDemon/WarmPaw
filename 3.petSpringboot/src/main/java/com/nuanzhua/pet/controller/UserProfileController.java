package com.nuanzhua.pet.controller;

import com.nuanzhua.pet.service.impl.UserProfileService;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/user/profile")
public class UserProfileController {
    private final UserProfileService service;
    public UserProfileController(UserProfileService service) { this.service=service; }
    public record Profile(@NotBlank @Size(max=32) String name, @NotNull @Pattern(regexp="1[3-9]\\d{9}") String phone) {}
    @GetMapping public Api<?> get(@RequestAttribute Actor actor) { return Api.ok(service.profile(actor)); }
    @PutMapping public Api<?> update(@RequestAttribute Actor actor,@Valid @RequestBody Profile profile) { return Api.ok(service.update(actor,profile.name(),profile.phone())); }
    @PostMapping(value="/avatar",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Api<?> upload(@RequestAttribute Actor actor,@RequestPart MultipartFile file) throws IOException { return Api.ok(service.upload(actor,file)); }
    @GetMapping("/avatar") public ResponseEntity<Resource> avatar(@RequestAttribute Actor actor) throws IOException {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_OCTET_STREAM).body(service.avatar(actor));
    }
}
