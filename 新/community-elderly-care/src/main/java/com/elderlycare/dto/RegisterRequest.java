package com.elderlycare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 50, message = "用户名长度需在4到50位之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在6到32位之间")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名长度不能超过50位")
    private String realName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @NotNull(message = "用户类型不能为空")
    private Integer userType;

    @Size(max = 255, message = "地址长度不能超过255位")
    private String address;

    @Size(max = 50, message = "紧急联系人长度不能超过50位")
    private String emergencyContact;

    @Pattern(regexp = "^$|^1\\d{10}$", message = "紧急联系人手机号格式不正确")
    private String emergencyPhone;
}
