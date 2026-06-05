package com.elderlycare.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class HealthRecordCreateRequest {

    private Long elderlyId;

    @NotNull(message = "记录类型不能为空")
    @Min(value = 1, message = "记录类型不正确")
    @Max(value = 4, message = "记录类型不正确")
    private Integer recordType;

    @Min(value = 1, message = "收缩压必须大于0")
    @Max(value = 300, message = "收缩压不能大于300")
    private Integer systolicPressure;

    @Min(value = 1, message = "舒张压必须大于0")
    @Max(value = 200, message = "舒张压不能大于200")
    private Integer diastolicPressure;

    @DecimalMin(value = "0.1", message = "血糖值必须大于0")
    @DecimalMax(value = "99.9", message = "血糖值不能大于99.9")
    private BigDecimal bloodSugar;

    @Min(value = 1, message = "心率必须大于0")
    @Max(value = 250, message = "心率不能大于250")
    private Integer heartRate;

    @NotNull(message = "记录时间不能为空")
    @PastOrPresent(message = "记录时间不能晚于当前时间")
    private LocalDateTime recordTime;
}
