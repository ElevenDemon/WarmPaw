package com.nuanzhua.pet.controller;

import com.nuanzhua.pet.exception.BusinessException;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;

/** 高德地址服务代理，密钥只保留在服务端。 */
@RestController
@RequestMapping("/api/owner/addresses/amap")
public class AmapAddressController {
    private final RestClient amap;
    private final String key;

    public AmapAddressController(@Value("${app.amap.base-url:https://restapi.amap.com}") String baseUrl,
                                 @Value("${app.amap.web-key:}") String key) {
        this.amap = RestClient.builder().baseUrl(baseUrl).build();
        this.key = key;
    }

    @GetMapping("/tips")
    public Api<?> tips(@RequestAttribute Actor actor, @RequestParam String keywords) {
        authorizeAddressLookup(actor);
        return Api.ok(call("/v3/assistant/inputtips", Map.of("keywords", keywords, "city", "深圳", "citylimit", "true", "datatype", "all")));
    }

    @GetMapping("/geocode")
    public Api<?> geocode(@RequestAttribute Actor actor, @RequestParam String address) {
        authorizeAddressLookup(actor);
        return Api.ok(call("/v3/geocode/geo", Map.of("address", address, "city", "深圳")));
    }

    @GetMapping("/regeo")
    public Api<?> regeo(@RequestAttribute Actor actor, @RequestParam String location) {
        authorizeAddressLookup(actor);
        return Api.ok(call("/v3/geocode/regeo", Map.of("location", location, "extensions", "all", "radius", "1000")));
    }

    private Map<?, ?> call(String path, Map<String, String> params) {
        if (key.isBlank()) throw BusinessException.bad("地址服务暂不可用，请稍后重试或联系客服");
        try {
            var uri = org.springframework.web.util.UriComponentsBuilder.fromPath(path).queryParam("key", key);
            params.forEach(uri::queryParam);
            @SuppressWarnings("unchecked") Map<String, Object> result = amap.get().uri(uri.build().encode().toUri()).retrieve().body(Map.class);
            if (result == null || !"1".equals(String.valueOf(result.get("status"))))
                throw BusinessException.bad("地点查询暂不可用，请检查网络后重试");
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw BusinessException.bad("高德地址服务请求失败，请稍后重试");
        }
    }

    private void authorizeAddressLookup(Actor actor) {
        if (!Set.of("OWNER", "SITTER").contains(actor.role())) throw BusinessException.forbidden();
    }
}
