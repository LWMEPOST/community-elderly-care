package com.elderlycare.controller;

import com.elderlycare.auth.AuthenticatedUser;
import com.elderlycare.auth.RequireUserTypes;
import com.elderlycare.auth.UserContextHolder;
import com.elderlycare.common.Result;
import com.elderlycare.dto.MessageConversationView;
import com.elderlycare.dto.MessageReplyRequest;
import com.elderlycare.dto.MessageSendRequest;
import com.elderlycare.dto.MessageUnreadSummaryView;
import com.elderlycare.dto.MessageView;
import com.elderlycare.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @RequireUserTypes({1, 2, 3, 4})
    @PostMapping("/send")
    public Result<MessageView> sendMessage(@Valid @RequestBody MessageSendRequest request) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        MessageView sent = messageService.sendMessage(
                currentUser.getUserId(), currentUser.getUserType(), request);
        return Result.success("消息发送成功", sent);
    }

    @RequireUserTypes({1, 2, 3, 4})
    @GetMapping("/conversations")
    public Result<List<MessageConversationView>> getConversations(@RequestParam(required = false) Boolean unreadOnly) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        List<MessageConversationView> conversations = messageService.getConversations(
                currentUser.getUserId(), currentUser.getUserType(), unreadOnly);
        return Result.success(conversations);
    }

    @RequireUserTypes({1, 2, 3, 4})
    @GetMapping("/conversation/{targetUserId}")
    public Result<List<MessageView>> getConversationMessages(@PathVariable Long targetUserId,
                                                             @RequestParam(required = false) Integer limit) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        List<MessageView> messages = messageService.getConversationMessages(
                currentUser.getUserId(), currentUser.getUserType(), targetUserId, limit);
        return Result.success(messages);
    }

    @RequireUserTypes({1, 2, 3, 4})
    @PostMapping("/{id}/reply")
    public Result<MessageView> replyMessage(@PathVariable Long id, @Valid @RequestBody MessageReplyRequest request) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        MessageView message = messageService.replyMessage(
                id, currentUser.getUserId(), currentUser.getUserType(), request);
        return Result.success("回复发送成功", message);
    }

    @RequireUserTypes({1, 2, 3, 4})
    @PutMapping("/{id}/read")
    public Result<MessageView> markAsRead(@PathVariable Long id) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        MessageView message = messageService.markAsRead(
                id, currentUser.getUserId(), currentUser.getUserType());
        return Result.success("消息已读", message);
    }

    @RequireUserTypes({1, 2, 3, 4})
    @GetMapping("/unread-summary")
    public Result<MessageUnreadSummaryView> getUnreadSummary() {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        MessageUnreadSummaryView summary = messageService.getUnreadSummary(currentUser.getUserId());
        return Result.success(summary);
    }
}
