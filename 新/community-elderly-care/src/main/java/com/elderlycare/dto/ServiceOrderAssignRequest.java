package com.elderlycare.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ServiceOrderAssignRequest {

    @NotNull(message = "服务人员ID不能为空")
    private Long serviceUserId;
}
