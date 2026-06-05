package com.elderlycare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FamilyBindingRequest {

    @NotNull(message = "老人ID不能为空")
    private Long elderlyId;

    private Long familyId;

    @NotBlank(message = "关系不能为空")
    @Size(max = 20, message = "关系长度不能超过20位")
    private String relation;
}
