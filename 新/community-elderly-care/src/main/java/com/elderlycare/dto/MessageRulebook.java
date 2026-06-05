package com.elderlycare.dto;

public final class MessageRulebook {

    public static final int STATUS_UNREAD = 0;
    public static final int STATUS_READ = 1;

    public static final int TYPE_MESSAGE = 1;
    public static final int TYPE_FEEDBACK = 2;
    public static final int TYPE_CONSULT = 3;
    public static final int TYPE_SYSTEM = 4;

    private MessageRulebook() {
    }

    public static String statusText(Integer status) {
        if (status == null) {
            return "未知状态";
        }
        return switch (status) {
            case STATUS_UNREAD -> "未读";
            case STATUS_READ -> "已读";
            default -> "未知状态";
        };
    }

    public static String messageTypeText(Integer messageType) {
        if (messageType == null) {
            return "未知类型";
        }
        return switch (messageType) {
            case TYPE_MESSAGE -> "留言";
            case TYPE_FEEDBACK -> "反馈";
            case TYPE_CONSULT -> "咨询";
            case TYPE_SYSTEM -> "系统提醒";
            default -> "未知类型";
        };
    }
}
