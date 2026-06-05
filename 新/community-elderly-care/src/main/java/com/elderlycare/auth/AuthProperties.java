package com.elderlycare.auth;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    @NotBlank
    private String issuer = "community-elderly-care";

    @NotBlank
    private String secret = "community-elderly-care-dev-secret-key-please-change-2026";

    @Min(1)
    private long expireMinutes = 720;

    private BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();

    @Data
    public static class BootstrapAdmin {
        private boolean enabled = true;
        private boolean syncPasswordOnStartup = true;
        private String username = "admin";
        private String password = "admin123";
        private String realName = "系统管理员";
        private String phone = "13800000000";
    }
}
