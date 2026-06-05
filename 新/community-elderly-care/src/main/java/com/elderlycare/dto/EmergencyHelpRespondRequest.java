package com.elderlycare.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class EmergencyHelpRespondRequest {

    @Min(value = 1, message = "响应人员ID不正确")
    private Long responseUserId;
}
