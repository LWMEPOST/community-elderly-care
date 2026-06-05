package com.elderlycare.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EmergencyHelpCreateRequest {

    private Long elderlyId;

    @DecimalMin(value = "-180.0000000", message = "经度不能小于-180")
    @DecimalMax(value = "180.0000000", message = "经度不能大于180")
    private BigDecimal longitude;

    @DecimalMin(value = "-90.0000000", message = "纬度不能小于-90")
    @DecimalMax(value = "90.0000000", message = "纬度不能大于90")
    private BigDecimal latitude;

    @Size(max = 255, message = "位置地址长度不能超过255位")
    private String locationAddress;

    @NotNull(message = "求助类型不能为空")
    @Min(value = 1, message = "求助类型不正确")
    @Max(value = 3, message = "求助类型不正确")
    private Integer helpType;

    @Size(max = 1000, message = "求助描述长度不能超过1000位")
    private String description;
}
