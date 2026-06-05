package com.elderlycare.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ElderlyProfileRequest {

    private Long userId;

    @Min(value = 1, message = "年龄不能小于1岁")
    @Max(value = 150, message = "年龄不能大于150岁")
    private Integer age;

    @Min(value = 0, message = "性别参数不正确")
    @Max(value = 1, message = "性别参数不正确")
    private Integer gender;

    @Size(max = 50, message = "健康状况长度不能超过50位")
    private String healthStatus;

    @Size(max = 2000, message = "病史长度不能超过2000位")
    private String medicalHistory;

    @DecimalMin(value = "-180.0000000", message = "经度不能小于-180")
    @DecimalMax(value = "180.0000000", message = "经度不能大于180")
    private BigDecimal longitude;

    @DecimalMin(value = "-90.0000000", message = "纬度不能小于-90")
    @DecimalMax(value = "90.0000000", message = "纬度不能大于90")
    private BigDecimal latitude;
}
