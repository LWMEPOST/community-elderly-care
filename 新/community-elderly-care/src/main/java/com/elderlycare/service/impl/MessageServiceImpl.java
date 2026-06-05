package com.elderlycare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elderlycare.dto.MessageConversationView;
import com.elderlycare.dto.MessageReplyRequest;
import com.elderlycare.dto.MessageRulebook;
import com.elderlycare.dto.MessageSendRequest;
import com.elderlycare.dto.MessageUnreadSummaryView;
import com.elderlycare.dto.MessageView;
import com.elderlycare.entity.Message;
import com.elderlycare.entity.User;
import com.elderlycare.exception.BusinessException;
import com.elderlycare.exception.ForbiddenException;
import com.elderlycare.mapper.MessageMapper;
import com.elderlycare.service.MessageService;
import com.elderlycare.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public MessageView sendMessage(Long operatorUserId, Integer operatorUserType, MessageSendRequest request) {
        User sender = getRequiredUser(operatorUserId, "发送人不存在");
        validateEnabledUser(sender, "发送人账号未启用");
        User receiver = getRequiredUser(request.getReceiverId(), "接收人不存在");
        validateEnabledUser(receiver, "接收人账号未启用");
        if (Objects.equals(operatorUserId, receiver.getId())) {
            throw new BusinessException(400, "不能给自己发送消息");
        }
        ensureCommunicationAllowed(operatorUserId, operatorUserType, receiver);

        Long rootMessageId = null;
        if (request.getParentId() != null) {
            Message parentMessage = getRequiredMessage(request.getParentId());
            ensureConversationParticipants(parentMessage, operatorUserId, receiver.getId());
            rootMessageId = parentMessage.getParentId() == null ? parentMessage.getId() : parentMessage.getParentId();
        }

        Message message = new Message();
        message.setSenderId(operatorUserId);
        message.setReceiverId(receiver.getId());
        message.setContent(normalizeContent(request.getContent()));
        message.setMessageType(request.getMessageType());
        message.setParentId(rootMessageId);
        message.setStatus(MessageRulebook.STATUS_UNREAD);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);
        return MessageView.from(message, sender, receiver, operatorUserId);
    }

    @Override
    public List<MessageConversationView> getConversations(Long operatorUserId, Integer operatorUserType, Boolean unreadOnly) {
        List<Message> messages = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .and(wrapper -> wrapper.eq(Message::getSenderId, operatorUserId).or().eq(Message::getReceiverId, operatorUserId))
                .orderByDesc(Message::getCreateTime));
        if (messages.isEmpty()) {
            return List.of();
        }

        Set<Long> counterpartIds = messages.stream()
                .map(message -> Objects.equals(message.getSenderId(), operatorUserId)
                        ? message.getReceiverId()
                        : message.getSenderId())
                .collect(Collectors.toSet());
        Map<Long, User> counterpartMap = userService.listByIds(counterpartIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        Map<Long, MessageConversationView> conversationMap = new LinkedHashMap<>();
        for (Message message : messages) {
            Long counterpartId = Objects.equals(message.getSenderId(), operatorUserId)
                    ? message.getReceiverId()
                    : message.getSenderId();
            MessageConversationView conversation = conversationMap.computeIfAbsent(counterpartId, key -> {
                MessageConversationView view = new MessageConversationView();
                view.setCounterpartUserId(key);
                User counterpart = counterpartMap.get(key);
                view.setCounterpartUserName(displayName(counterpart));
                view.setCounterpartUserType(counterpart == null ? null : counterpart.getUserType());
                view.setUnreadCount(0);
                return view;
            });

            if (Objects.equals(message.getReceiverId(), operatorUserId)
                    && Objects.equals(message.getStatus(), MessageRulebook.STATUS_UNREAD)) {
                conversation.setUnreadCount(conversation.getUnreadCount() + 1);
            }

            LocalDateTime activityTime = resolveActivityTime(message);
            if (conversation.getLastMessageTime() == null || activityTime.isAfter(conversation.getLastMessageTime())) {
                conversation.setLastMessageId(message.getId());
                conversation.setLastMessageContent(resolveActivityContent(message));
                conversation.setLastMessageType(message.getMessageType());
                conversation.setLastMessageTypeText(MessageRulebook.messageTypeText(message.getMessageType()));
                conversation.setLastMessageStatus(resolveActivityStatus(message));
                conversation.setLastMessageStatusText(MessageRulebook.statusText(resolveActivityStatus(message)));
                conversation.setLastMessageTime(activityTime);
                conversation.setLastMessageFromCurrentUser(Objects.equals(resolveActivitySenderId(message), operatorUserId));
            }
        }

        return conversationMap.values().stream()
                .filter(view -> !Boolean.TRUE.equals(unreadOnly) || view.getUnreadCount() > 0)
                .sorted(Comparator.comparing(
                        MessageConversationView::getLastMessageTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public List<MessageView> getConversationMessages(Long operatorUserId, Integer operatorUserType,
                                                     Long targetUserId, Integer limit) {
        User targetUser = getRequiredUser(targetUserId, "会话对象不存在");
        ensureConversationAccessible(operatorUserId, operatorUserType, targetUser);

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.nested(condition -> condition
                        .eq(Message::getSenderId, operatorUserId)
                        .eq(Message::getReceiverId, targetUserId)
                        .or()
                        .eq(Message::getSenderId, targetUserId)
                        .eq(Message::getReceiverId, operatorUserId))
                .orderByDesc(Message::getCreateTime);
        if (limit != null) {
            wrapper.last("LIMIT " + normalizeLimit(limit));
        }
        List<Message> messages = messageMapper.selectList(wrapper);
        if (messages.isEmpty()) {
            return List.of();
        }
        List<Message> orderedMessages = new ArrayList<>(messages);
        orderedMessages.sort(Comparator.comparing(Message::getCreateTime));
        return buildViews(orderedMessages, operatorUserId);
    }

    @Override
    @Transactional
    public MessageView replyMessage(Long messageId, Long operatorUserId,
                                    Integer operatorUserType, MessageReplyRequest request) {
        Message originalMessage = getRequiredMessage(messageId);
        ensureMessageParticipant(originalMessage, operatorUserId);

        Long targetUserId = Objects.equals(originalMessage.getSenderId(), operatorUserId)
                ? originalMessage.getReceiverId()
                : originalMessage.getSenderId();
        User currentUser = getRequiredUser(operatorUserId, "当前用户不存在");
        User targetUser = getRequiredUser(targetUserId, "会话对象不存在");
        validateEnabledUser(currentUser, "当前用户账号未启用");
        validateEnabledUser(targetUser, "会话对象账号未启用");
        ensureCommunicationAllowed(operatorUserId, operatorUserType, targetUser);

        if (Objects.equals(originalMessage.getReceiverId(), operatorUserId)
                && Objects.equals(originalMessage.getStatus(), MessageRulebook.STATUS_UNREAD)) {
            originalMessage.setStatus(MessageRulebook.STATUS_READ);
            messageMapper.updateById(originalMessage);
        }

        Message replyMessage = new Message();
        replyMessage.setSenderId(operatorUserId);
        replyMessage.setReceiverId(targetUserId);
        replyMessage.setContent(normalizeContent(request.getContent()));
        replyMessage.setMessageType(originalMessage.getMessageType());
        replyMessage.setParentId(originalMessage.getParentId() == null
                ? originalMessage.getId()
                : originalMessage.getParentId());
        replyMessage.setStatus(MessageRulebook.STATUS_UNREAD);
        replyMessage.setCreateTime(LocalDateTime.now());
        messageMapper.insert(replyMessage);
        return MessageView.from(replyMessage, currentUser, targetUser, operatorUserId);
    }

    @Override
    @Transactional
    public MessageView markAsRead(Long messageId, Long operatorUserId, Integer operatorUserType) {
        Message message = getRequiredMessage(messageId);
        if (!Objects.equals(message.getReceiverId(), operatorUserId)) {
            throw new ForbiddenException("仅接收人可标记消息已读");
        }
        if (!Objects.equals(message.getStatus(), MessageRulebook.STATUS_READ)) {
            message.setStatus(MessageRulebook.STATUS_READ);
            messageMapper.updateById(message);
        }
        return buildView(message, operatorUserId);
    }

    @Override
    public MessageUnreadSummaryView getUnreadSummary(Long operatorUserId) {
        List<Message> unreadMessages = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, operatorUserId)
                .eq(Message::getStatus, MessageRulebook.STATUS_UNREAD)
                .orderByDesc(Message::getCreateTime));
        return MessageUnreadSummaryView.builder()
                .totalUnreadCount(unreadMessages.size())
                .unreadConversationCount(unreadMessages.stream().map(Message::getSenderId).distinct().count())
                .latestUnreadTime(unreadMessages.isEmpty() ? null : unreadMessages.get(0).getCreateTime())
                .build();
    }

    @Override
    @Transactional
    public void sendSystemNotifications(Long senderUserId, Collection<Long> receiverIds, String content) {
        if (receiverIds == null || receiverIds.isEmpty()) {
            return;
        }
        User sender = getRequiredUser(senderUserId, "消息发送人不存在");
        Set<Long> normalizedReceiverIds = receiverIds.stream()
                .filter(Objects::nonNull)
                .filter(receiverId -> !Objects.equals(receiverId, senderUserId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedReceiverIds.isEmpty()) {
            return;
        }

        Set<Long> enabledReceiverIds = userService.listByIds(normalizedReceiverIds).stream()
                .filter(user -> Objects.equals(user.getStatus(), 1))
                .map(User::getId)
                .collect(Collectors.toSet());
        if (enabledReceiverIds.isEmpty()) {
            return;
        }

        String normalizedContent = normalizeContent(content);
        LocalDateTime now = LocalDateTime.now();
        for (Long receiverId : normalizedReceiverIds) {
            if (!enabledReceiverIds.contains(receiverId)) {
                continue;
            }
            Message message = new Message();
            message.setSenderId(sender.getId());
            message.setReceiverId(receiverId);
            message.setContent(normalizedContent);
            message.setMessageType(MessageRulebook.TYPE_SYSTEM);
            message.setStatus(MessageRulebook.STATUS_UNREAD);
            message.setCreateTime(now);
            messageMapper.insert(message);
        }
    }

    private void ensureCommunicationAllowed(Long operatorUserId, Integer operatorUserType, User targetUser) {
        if (!canCommunicate(operatorUserId, operatorUserType, targetUser)) {
            throw new ForbiddenException("当前用户无权向该对象发送消息");
        }
    }

    private boolean canCommunicate(Long operatorUserId, Integer operatorUserType, User targetUser) {
        if (targetUser == null || !Objects.equals(targetUser.getStatus(), 1)) {
            return false;
        }
        if (Objects.equals(operatorUserId, targetUser.getId())) {
            return false;
        }
        if (Objects.equals(operatorUserType, 3)) {
            return true;
        }
        if (Objects.equals(targetUser.getUserType(), 3)) {
            return true;
        }
        if (Objects.equals(operatorUserType, 1) && Objects.equals(targetUser.getUserType(), 2)) {
            return userService.hasConfirmedBinding(operatorUserId, targetUser.getId());
        }
        if (Objects.equals(operatorUserType, 2) && Objects.equals(targetUser.getUserType(), 1)) {
            return userService.hasConfirmedBinding(targetUser.getId(), operatorUserId);
        }
        return Objects.equals(operatorUserType, 4) && Objects.equals(targetUser.getUserType(), 3);
    }

    private void ensureConversationAccessible(Long operatorUserId, Integer operatorUserType, User targetUser) {
        if (Objects.equals(operatorUserId, targetUser.getId())) {
            throw new BusinessException(400, "不能查看与自己的会话");
        }
        if (canCommunicate(operatorUserId, operatorUserType, targetUser) || hasConversationHistory(operatorUserId, targetUser.getId())) {
            return;
        }
        throw new ForbiddenException("无权查看该会话");
    }

    private boolean hasConversationHistory(Long userId, Long targetUserId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.nested(condition -> condition
                        .eq(Message::getSenderId, userId)
                        .eq(Message::getReceiverId, targetUserId)
                        .or()
                        .eq(Message::getSenderId, targetUserId)
                        .eq(Message::getReceiverId, userId))
                .last("LIMIT 1");
        return messageMapper.selectOne(wrapper) != null;
    }

    private void ensureConversationParticipants(Message parentMessage, Long senderId, Long receiverId) {
        boolean validParticipants = (Objects.equals(parentMessage.getSenderId(), senderId)
                && Objects.equals(parentMessage.getReceiverId(), receiverId))
                || (Objects.equals(parentMessage.getSenderId(), receiverId)
                && Objects.equals(parentMessage.getReceiverId(), senderId));
        if (!validParticipants) {
            throw new ForbiddenException("父消息不属于当前会话");
        }
    }

    private void ensureMessageParticipant(Message message, Long operatorUserId) {
        if (!Objects.equals(message.getSenderId(), operatorUserId)
                && !Objects.equals(message.getReceiverId(), operatorUserId)) {
            throw new ForbiddenException("无权操作该消息");
        }
    }

    private User getRequiredUser(Long userId, String message) {
        User user = userService.getUserInfo(userId);
        if (user == null) {
            throw new BusinessException(404, message);
        }
        return user;
    }

    private void validateEnabledUser(User user, String message) {
        if (!Objects.equals(user.getStatus(), 1)) {
            throw new BusinessException(400, message);
        }
    }

    private Message getRequiredMessage(Long messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(404, "消息不存在");
        }
        return message;
    }

    private List<MessageView> buildViews(List<Message> messages, Long currentUserId) {
        if (messages.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = messages.stream()
                .flatMap(message -> java.util.stream.Stream.of(message.getSenderId(), message.getReceiverId()))
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        return messages.stream()
                .map(message -> MessageView.from(
                        message,
                        userMap.get(message.getSenderId()),
                        userMap.get(message.getReceiverId()),
                        currentUserId))
                .toList();
    }

    private MessageView buildView(Message message, Long currentUserId) {
        Map<Long, User> userMap = userService.listByIds(List.of(message.getSenderId(), message.getReceiverId())).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        return MessageView.from(message, userMap.get(message.getSenderId()), userMap.get(message.getReceiverId()), currentUserId);
    }

    private LocalDateTime resolveActivityTime(Message message) {
        if (hasInlineReply(message)) {
            return message.getReplyTime();
        }
        return message.getCreateTime();
    }

    private String resolveActivityContent(Message message) {
        if (hasInlineReply(message)) {
            return message.getReplyContent();
        }
        return message.getContent();
    }

    private Long resolveActivitySenderId(Message message) {
        if (hasInlineReply(message)) {
            return message.getReceiverId();
        }
        return message.getSenderId();
    }

    private Integer resolveActivityStatus(Message message) {
        if (hasInlineReply(message)) {
            return MessageRulebook.STATUS_READ;
        }
        return message.getStatus();
    }

    private boolean hasInlineReply(Message message) {
        return StringUtils.hasText(message.getReplyContent()) && message.getReplyTime() != null
                && (message.getCreateTime() == null || !message.getReplyTime().isBefore(message.getCreateTime()));
    }

    private String normalizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(400, "消息内容不能为空");
        }
        return content.trim();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 50;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 200);
    }

    private String displayName(User user) {
        if (user == null) {
            return null;
        }
        if (StringUtils.hasText(user.getRealName())) {
            return user.getRealName().trim();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return "用户#" + user.getId();
    }
}
