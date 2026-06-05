package com.elderlycare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("health_record")
public class HealthRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long elderlyId;
    private Integer recordType;
    private Integer systolicPressure;
    private Integer diastolicPressure;
    private BigDecimal bloodSugar;
    private Integer heartRate;
    private LocalDateTime recordTime;
    private Integer warningLevel;
    private String advice;
    private LocalDateTime createTime;
}