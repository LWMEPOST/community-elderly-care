package com.elderlycare.dto;

import com.elderlycare.entity.Message;
import com.elderlycare.entity.User;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Objects;

@Data
public class MessageView {

    private Long id;
    private Long rootMessageId;
    private Long parentId;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private String content;
    private Integer messageType;
    private String messageTypeText;
    private Integer status;
    private String statusText;
    private String replyContent;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;
    private Boolean fromCurrentUser;

    public static MessageView from(Message message, User sender, User receiver, Long currentUserId) {
        MessageView view = new MessageView();
        view.setId(message.getId());
        view.setRootMessageId(message.getParentId() == null ? message.getId() : message.getParentId());
        view.setParentId(message.getParentId());
        view.setSenderId(message.getSenderId());
        view.setSenderName(sender == null ? null : sender.getRealName());
        view.setReceiverId(message.getReceiverId());
        view.setReceiverName(receiver == null ? null : receiver.getRealName());
        view.setContent(message.getContent());
        view.setMessageType(message.getMessageType());
        view.setMessageTypeText(MessageRulebook.messageTypeText(message.getMessageType()));
        view.setStatus(message.getStatus());
        view.setStatusText(MessageRulebook.statusText(message.getStatus()));
        view.setReplyContent(message.getReplyContent());
        view.setReplyTime(message.getReplyTime());
        view.setCreateTime(message.getCreateTime());
        view.setFromCurrentUser(Objects.equals(message.getSenderId(), currentUserId));
        return view;
    }
}
