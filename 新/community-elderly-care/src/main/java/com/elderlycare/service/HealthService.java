package com.elderlycare.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.elderlycare.dto.HealthRecordCreateRequest;
import com.elderlycare.dto.HealthRecordView;
import com.elderlycare.dto.HealthWarningSummaryView;
import com.elderlycare.entity.HealthRecord;
import java.math.BigDecimal;
import java.util.List;

public interface HealthService extends IService<HealthRecord> {
    HealthRecordView addHealthRecord(Long operatorUserId, Integer operatorUserType, HealthRecordCreateRequest request);
    List<HealthRecordView> getHealthRecords(Long operatorUserId, Integer operatorUserType,
                                            Long elderlyId, Integer recordType, Integer warningLevel, Integer limit);
    HealthRecordView getLatestRecord(Long operatorUserId, Integer operatorUserType, Long elderlyId, Integer recordType);
    HealthWarningSummaryView getWarningSummary(Long operatorUserId, Integer operatorUserType, Long elderlyId);
    List<HealthRecordView> getWarningRecords(Long operatorUserId, Integer operatorUserType,
                                             Long elderlyId, Integer warningLevel, Integer limit);
    String generateAdvice(Integer recordType, Integer systolic, Integer diastolic, BigDecimal bloodSugar, Integer heartRate);
}
