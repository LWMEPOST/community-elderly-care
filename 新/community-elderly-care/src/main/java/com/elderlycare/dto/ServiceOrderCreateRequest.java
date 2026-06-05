package com.elderlycare.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ServiceOrderCreateRequest {

    private Long elderlyId;

    @NotNull(message = "服务项目不能为空")
    private Long serviceItemId;

    @NotNull(message = "预约时间不能为空")
    @Future(message = "预约时间必须晚于当前时间")
    private LocalDateTime appointmentTime;

    @NotBlank(message = "服务地址不能为空")
    @Size(max = 255, message = "服务地址长度不能超过255位")
    private String serviceAddress;

    @Size(max = 500, message = "备注长度不能超过500位")
    private String remark;
}
