package com.elderlycare.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageUnreadSummaryView {

    private long totalUnreadCount;
    private long unreadConversationCount;
    private LocalDateTime latestUnreadTime;
}
