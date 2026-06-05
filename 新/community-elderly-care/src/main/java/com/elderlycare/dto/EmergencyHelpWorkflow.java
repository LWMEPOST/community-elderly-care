package com.elderlycare.dto;

public final class EmergencyHelpWorkflow {

    public static final int STATUS_PENDING = 1;
    public static final int STATUS_RESPONDED = 2;
    public static final int STATUS_RESOLVED = 3;

    private EmergencyHelpWorkflow() {
    }

    public static String statusText(Integer status) {
        if (status == null) {
            return "未知状态";
        }
        return switch (status) {
            case STATUS_PENDING -> "待响应";
            case STATUS_RESPONDED -> "已响应";
            case STATUS_RESOLVED -> "已解决";
            default -> "未知状态";
        };
    }

    public static String helpTypeText(Integer helpType) {
        if (helpType == null) {
            return "未知类型";
        }
        return switch (helpType) {
            case 1 -> "医疗求助";
            case 2 -> "摔倒求助";
            case 3 -> "其他求助";
            default -> "未知类型";
        };
    }
}
