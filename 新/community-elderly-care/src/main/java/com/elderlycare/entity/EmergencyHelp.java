package com.elderlycare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("emergency_help")
public class EmergencyHelp {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long elderlyId;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String locationAddress;
    private Integer helpType;
    private String description;
    private Integer status;
    private Long responseUserId;
    private LocalDateTime createTime;
    private LocalDateTime resolveTime;
}