package com.elderlycare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("family_binding")
public class FamilyBinding {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long elderlyId;
    private Long familyId;
    private String relation;
    private Integer status;
    private LocalDateTime createTime;
}