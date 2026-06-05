package com.elderlycare.dto;

public final class InformationRulebook {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_PUBLISHED = 1;

    private static final int SUMMARY_LIMIT = 120;

    private InformationRulebook() {
    }

    public static String infoTypeText(Integer infoType) {
        if (infoType == null) {
            return "未知类型";
        }
        return switch (infoType) {
            case 1 -> "政策";
            case 2 -> "活动";
            case 3 -> "通知";
            case 4 -> "动态";
            default -> "未知类型";
        };
    }

    public static String statusText(Integer status) {
        if (status == null) {
            return "未知状态";
        }
        return switch (status) {
            case STATUS_DRAFT -> "未发布";
            case STATUS_PUBLISHED -> "已发布";
            default -> "未知状态";
        };
    }

    public static String summarize(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String normalized = content.trim().replace("\r", "").replace("\n", " ");
        if (normalized.length() <= SUMMARY_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, SUMMARY_LIMIT) + "...";
    }
}
