package com.nuanzhua.pet.controller;
import com.nuanzhua.pet.service.OwnerApplicationService;
import com.nuanzhua.pet.dto.OwnerRequests.*;

import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.*;

/** 宠主档案REST接口；负责参数校验和服务调用，不直接访问数据库。 */
@Tag(name="宠主档案",description="宠主档案相关接口")
@RestController @RequestMapping("/api/owner")
public class OwnerController {
    private final OwnerApplicationService application;
    public OwnerController(OwnerApplicationService application) { this.application=application; }
    /** 查询当前宠主未删除的宠物。 */
    @Operation(summary="查询当前宠主未删除的宠物")
    @GetMapping("/pets") public Api<?> pets(@Parameter(hidden=true) @RequestAttribute Actor actor) { return application.pets(actor); }
    /** 新增当前宠主的宠物档案。 */
    @Operation(summary="新增当前宠主的宠物档案")
    @PostMapping("/pets") public Api<?> addPet(@Parameter(hidden=true) @RequestAttribute Actor actor,@Valid @RequestBody Pet r) { return application.addPet(actor,r); }
    /** 更新当前宠主自己的宠物档案。 */
    @Operation(summary="更新当前宠主自己的宠物档案")
    @PutMapping("/pets/{id}") public Api<?> updatePet(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id,@Valid @RequestBody Pet r) { return application.updatePet(actor,id,r); }
    /** 软删除当前宠主的宠物档案。 */
    @Operation(summary="软删除当前宠主的宠物档案")
    @DeleteMapping("/pets/{id}") public Api<?> deletePet(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id) { return application.deletePet(actor,id); }
    /** 查询当前宠主的地址簿。 */
    @Operation(summary="查询当前宠主的地址簿")
    @GetMapping("/addresses") public Api<?> addresses(@Parameter(hidden=true) @RequestAttribute Actor actor) { return application.addresses(actor); }
    /** 新增当前宠主的服务地址。 */
    @Operation(summary="新增当前宠主的服务地址")
    @PostMapping("/addresses") public Api<?> addAddress(@Parameter(hidden=true) @RequestAttribute Actor actor,@Valid @RequestBody Address r) { return application.addAddress(actor,r); }
    /** 更新当前宠主自己的地址。 */
    @Operation(summary="更新当前宠主自己的地址")
    @PutMapping("/addresses/{id}") public Api<?> updateAddress(@Parameter(hidden=true) @RequestAttribute Actor actor,@PathVariable long id,@Valid @RequestBody Address r) { return application.updateAddress(actor,id,r); }
}
