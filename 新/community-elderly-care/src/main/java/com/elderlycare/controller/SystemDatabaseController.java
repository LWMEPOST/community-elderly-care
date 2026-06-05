package com.elderlycare.controller;

import com.elderlycare.auth.RequireUserTypes;
import com.elderlycare.common.Result;
import com.elderlycare.service.DatabaseOpsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequireUserTypes({3})
@RequestMapping("/api/system/db")
public class SystemDatabaseController {

    private final DatabaseOpsService databaseOpsService;

    public SystemDatabaseController(DatabaseOpsService databaseOpsService) {
        this.databaseOpsService = databaseOpsService;
    }

    @GetMapping("/connectivity")
    public Result<Map<String, Object>> connectivity() {
        Map<String, Object> data = databaseOpsService.checkConnectivity();
        if (Boolean.TRUE.equals(data.get("connected"))) {
            return Result.success("数据库连通性校验通过", data);
        }
        return Result.error(500, String.valueOf(data.get("message")));
    }

    @PostMapping("/initialize")
    public Result<Map<String, Object>> initialize() {
        Map<String, Object> data = databaseOpsService.initializeSchema();
        if (Boolean.TRUE.equals(data.get("success"))) {
            return Result.success("数据库初始化成功", data);
        }
        return Result.error(500, String.valueOf(data.get("message")));
    }
}
