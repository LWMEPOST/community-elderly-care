package com.elderlycare.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageConversationView {

    private Long counterpartUserId;
    private String counterpartUserName;
    private Integer counterpartUserType;
    private Long lastMessageId;
    private String lastMessageContent;
    private Integer lastMessageType;
    private String lastMessageTypeText;
    private Integer lastMessageStatus;
    private String lastMessageStatusText;
    private LocalDateTime lastMessageTime;
    private Boolean lastMessageFromCurrentUser;
    private int unreadCount;
}
