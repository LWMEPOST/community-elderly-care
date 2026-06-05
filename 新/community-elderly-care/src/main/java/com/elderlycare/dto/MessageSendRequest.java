package com.elderlycare.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MessageSendRequest {

    @NotNull(message = "接收人ID不能为空")
    @Min(value = 1, message = "接收人ID不正确")
    private Long receiverId;

    @NotNull(message = "消息类型不能为空")
    @Min(value = 1, message = "消息类型不正确")
    @Max(value = 3, message = "消息类型不正确")
    private Integer messageType;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 1000, message = "消息内容长度不能超过1000位")
    private String content;

    @Min(value = 1, message = "父消息ID不正确")
    private Long parentId;
}
