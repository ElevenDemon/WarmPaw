package com.nuanzhua.pet.controller;

import com.nuanzhua.pet.common.Db;
import com.nuanzhua.pet.exception.BusinessException;
import com.nuanzhua.pet.service.OperationsService;
import com.nuanzhua.pet.vo.Actor;
import com.nuanzhua.pet.vo.Api;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.*;

/** 宠主及喂养师的AI客服代理；身份、会话归属和聊天记录均由业务后端管理。 */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {
    private final Db db;
    private final OperationsService operations;
    private final RestClient aiClient;
    private final String apiKey;

    public AiChatController(Db db, OperationsService operations,
            @Value("${ai.service.base-url:http://localhost:8090}") String baseUrl,
            @Value("${ai.service.api-key:}") String apiKey) {
        this.db = db;
        this.operations = operations;
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(50000);
        this.aiClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.apiKey = apiKey;
    }

    @PostMapping("/chat")
    public Api<?> chat(@RequestAttribute Actor actor, @Valid @RequestBody ChatRequest request) {
        if (!Set.of("OWNER", "SITTER").contains(actor.role())) throw BusinessException.forbidden();
        var conversation = operations.conversation(actor, request.conversationId());
        String status = String.valueOf(conversation.get("status"));
        if ("CLOSED".equals(status)) throw BusinessException.conflict("会话已关闭");
        if (!"WAITING_HUMAN".equals(status)) throw BusinessException.conflict("该会话已转交人工客服");

        var history = db.queryForList("""
                select sender_type, content from (
                    select sender_type, content, id from chat_message
                    where conversation_id=? order by id desc limit 12
                ) recent order by id
                """, request.conversationId()).stream().map(row -> Map.of(
                    "role", "AI".equals(row.get("sender_type")) ? "assistant" : "user",
                    "content", String.valueOf(row.get("content")))).toList();
        var payload = Map.of("message", request.message(), "context", Map.of(
                "role", actor.role(), "history", history));
        Map<?, ?> result;
        try {
            var call = aiClient.post().uri("/api/ai/chat").body(payload);
            if (!apiKey.isBlank()) call.header("X-AI-API-Key", apiKey);
            result = call.retrieve().body(Map.class);
        } catch (RestClientException ex) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "智能客服暂时不可用，请稍后重试或联系人工客服");
        }
        if (result == null || !(result.get("reply") instanceof String reply) || reply.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "智能客服暂未返回有效答复");
        }

        long userMessageId = db.id("insert into chat_message(conversation_id,sender_id,sender_type,content,media_keys) values (?,?, 'USER', ?, '[]'::jsonb) returning id",
                request.conversationId(), actor.id(), request.message());
        long assistantMessageId = db.id("insert into chat_message(conversation_id,sender_id,sender_type,content,media_keys) values (?,?, 'AI', ?, '[]'::jsonb) returning id",
                request.conversationId(), actor.id(), reply);
        return Api.ok(Map.of(
                "userMessage", Map.of("id", userMessageId, "sender_id", actor.id(), "sender_type", "USER", "content", request.message()),
                "assistantMessage", Map.of("id", assistantMessageId, "sender_id", actor.id(), "sender_type", "AI", "content", reply),
                "handoff", false));
    }

    public record ChatRequest(@NotNull Long conversationId, @NotBlank @Size(max = 2000) String message) {}
}
