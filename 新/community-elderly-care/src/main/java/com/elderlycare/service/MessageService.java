package com.elderlycare.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.elderlycare.dto.MessageConversationView;
import com.elderlycare.dto.MessageReplyRequest;
import com.elderlycare.dto.MessageSendRequest;
import com.elderlycare.dto.MessageUnreadSummaryView;
import com.elderlycare.dto.MessageView;
import com.elderlycare.entity.Message;

import java.util.Collection;
import java.util.List;

public interface MessageService extends IService<Message> {
    MessageView sendMessage(Long operatorUserId, Integer operatorUserType, MessageSendRequest request);
    List<MessageConversationView> getConversations(Long operatorUserId, Integer operatorUserType, Boolean unreadOnly);
    List<MessageView> getConversationMessages(Long operatorUserId, Integer operatorUserType, Long targetUserId, Integer limit);
    MessageView replyMessage(Long messageId, Long operatorUserId, Integer operatorUserType, MessageReplyRequest request);
    MessageView markAsRead(Long messageId, Long operatorUserId, Integer operatorUserType);
    MessageUnreadSummaryView getUnreadSummary(Long operatorUserId);
    void sendSystemNotifications(Long senderUserId, Collection<Long> receiverIds, String content);
}
