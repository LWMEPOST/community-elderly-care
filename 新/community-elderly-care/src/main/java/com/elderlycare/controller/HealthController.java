package com.elderlycare.controller;

import com.elderlycare.auth.AuthenticatedUser;
import com.elderlycare.auth.RequireUserTypes;
import com.elderlycare.auth.UserContextHolder;
import com.elderlycare.common.Result;
import com.elderlycare.dto.HealthRecordCreateRequest;
import com.elderlycare.dto.HealthRecordView;
import com.elderlycare.dto.HealthWarningSummaryView;
import com.elderlycare.service.HealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private HealthService healthService;

    @RequireUserTypes({1, 2, 3})
    @PostMapping("/record")
    public Result<HealthRecordView> addHealthRecord(@Valid @RequestBody HealthRecordCreateRequest request) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        HealthRecordView savedRecord = healthService.addHealthRecord(
                currentUser.getUserId(), currentUser.getUserType(), request);
        return Result.success("健康记录添加成功", savedRecord);
    }

    @RequireUserTypes({1, 2, 3})
    @GetMapping("/records")
    public Result<List<HealthRecordView>> getHealthRecords(@RequestParam(required = false) Long elderlyId,
                                                           @RequestParam(required = false) Integer recordType,
                                                           @RequestParam(required = false) Integer warningLevel,
                                                           @RequestParam(required = false) Integer limit) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        List<HealthRecordView> records = healthService.getHealthRecords(
                currentUser.getUserId(), currentUser.getUserType(), elderlyId, recordType, warningLevel, limit);
        return Result.success(records);
    }

    @RequireUserTypes({1, 2, 3})
    @GetMapping("/latest")
    public Result<HealthRecordView> getLatestRecord(@RequestParam(required = false) Long elderlyId,
                                                     @RequestParam Integer recordType) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        HealthRecordView record = healthService.getLatestRecord(
                currentUser.getUserId(), currentUser.getUserType(), elderlyId, recordType);
        return Result.success(record);
    }

    @RequireUserTypes({1, 2, 3})
    @GetMapping("/warnings")
    public Result<List<HealthRecordView>> getWarningRecords(@RequestParam(required = false) Long elderlyId,
                                                            @RequestParam(required = false) Integer warningLevel,
                                                            @RequestParam(required = false) Integer limit) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        List<HealthRecordView> records = healthService.getWarningRecords(
                currentUser.getUserId(), currentUser.getUserType(), elderlyId, warningLevel, limit);
        return Result.success(records);
    }

    @RequireUserTypes({1, 2, 3})
    @GetMapping("/summary")
    public Result<HealthWarningSummaryView> getWarningSummary(@RequestParam(required = false) Long elderlyId) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        HealthWarningSummaryView summary = healthService.getWarningSummary(
                currentUser.getUserId(), currentUser.getUserType(), elderlyId);
        return Result.success(summary);
    }
}
