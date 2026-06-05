package com.elderlycare.dto;

public final class HealthRulebook {

    private HealthRulebook() {
    }

    public static String recordTypeText(Integer recordType) {
        if (recordType == null) {
            return "未知类型";
        }
        return switch (recordType) {
            case 1 -> "血压";
            case 2 -> "血糖";
            case 3 -> "心率";
            case 4 -> "其他";
            default -> "未知类型";
        };
    }

    public static String warningLevelText(Integer warningLevel) {
        if (warningLevel == null) {
            return "未知";
        }
        return switch (warningLevel) {
            case 0 -> "正常";
            case 1 -> "低风险预警";
            case 2 -> "高风险预警";
            default -> "未知";
        };
    }
}
