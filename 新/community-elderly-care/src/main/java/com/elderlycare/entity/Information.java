package com.elderlycare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("information")
public class Information {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private Integer infoType;
    private Long publisherId;
    private String coverImage;
    private Integer status;
    private Integer viewCount;
    private LocalDateTime createTime;
    private LocalDateTime publishTime;
}