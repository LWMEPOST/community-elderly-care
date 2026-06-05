package com.elderlycare.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    private Long id;

    @Size(max = 50, message = "真实姓名长度不能超过50位")
    private String realName;

    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 255, message = "头像地址长度不能超过255位")
    private String avatar;

    @Size(max = 255, message = "地址长度不能超过255位")
    private String address;

    @Size(max = 50, message = "紧急联系人长度不能超过50位")
    private String emergencyContact;

    @Pattern(regexp = "^$|^1\\d{10}$", message = "紧急联系人手机号格式不正确")
    private String emergencyPhone;
}
