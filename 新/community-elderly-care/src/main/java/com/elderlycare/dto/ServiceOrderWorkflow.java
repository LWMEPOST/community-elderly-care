package com.elderlycare.dto;

public final class ServiceOrderWorkflow {

    public static final int PENDING = 1;
    public static final int ACCEPTED = 2;
    public static final int IN_SERVICE = 3;
    public static final int COMPLETED = 4;
    public static final int CANCELLED = 5;

    private ServiceOrderWorkflow() {
    }

    public static String statusText(Integer status) {
        if (status == null) {
            return "未知状态";
        }
        return switch (status) {
            case PENDING -> "待接单";
            case ACCEPTED -> "已接单";
            case IN_SERVICE -> "服务中";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
            default -> "未知状态";
        };
    }
}
