package com.elderlycare.config;

import com.elderlycare.service.DatabaseOpsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseStartupInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStartupInitializer.class);

    private final DatabaseInitProperties properties;
    private final DatabaseOpsService databaseOpsService;

    public DatabaseStartupInitializer(DatabaseInitProperties properties, DatabaseOpsService databaseOpsService) {
        this.properties = properties;
        this.databaseOpsService = databaseOpsService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.getInit().isEnabled() || !properties.getInit().isRunOnStartup()) {
            log.info("跳过数据库启动初始化，enabled={}, runOnStartup={}",
                    properties.getInit().isEnabled(), properties.getInit().isRunOnStartup());
            return;
        }
        log.info("开始执行数据库启动初始化");
        databaseOpsService.initializeSchema();
        log.info("数据库启动初始化结束");
    }
}
