package com.elderlycare.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InformationSaveRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题长度不能超过100位")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Size(max = 10000, message = "内容长度不能超过10000位")
    private String content;

    @NotNull(message = "资讯类型不能为空")
    @Min(value = 1, message = "资讯类型不正确")
    @Max(value = 4, message = "资讯类型不正确")
    private Integer infoType;

    @Size(max = 255, message = "封面图地址长度不能超过255位")
    private String coverImage;
}
