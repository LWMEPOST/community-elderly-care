package com.elderlycare.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LoginResponse {

    private String token;
    private String tokenType;
    private LocalDateTime expireAt;
    private UserView user;
}
