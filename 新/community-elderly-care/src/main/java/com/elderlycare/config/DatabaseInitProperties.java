package com.elderlycare.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.database")
public class DatabaseInitProperties {

    private String healthQuery = "SELECT 1";

    private Init init = new Init();

    @Data
    public static class Init {
        private boolean enabled = true;
        private boolean runOnStartup = true;
        private boolean failOnError = true;
        private String scriptLocation = "classpath:db/init/schema-mysql.sql";
    }
}
