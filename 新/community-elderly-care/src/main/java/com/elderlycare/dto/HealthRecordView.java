package com.elderlycare.dto;

import com.elderlycare.entity.HealthRecord;
import com.elderlycare.entity.User;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class HealthRecordView {

    private Long id;
    private Long elderlyId;
    private String elderlyName;
    private Integer recordType;
    private String recordTypeText;
    private Integer systolicPressure;
    private Integer diastolicPressure;
    private BigDecimal bloodSugar;
    private Integer heartRate;
    private LocalDateTime recordTime;
    private Integer warningLevel;
    private String warningLevelText;
    private String advice;
    private LocalDateTime createTime;

    public static HealthRecordView from(HealthRecord record, User elderlyUser) {
        HealthRecordView view = new HealthRecordView();
        view.setId(record.getId());
        view.setElderlyId(record.getElderlyId());
        view.setElderlyName(elderlyUser == null ? null : elderlyUser.getRealName());
        view.setRecordType(record.getRecordType());
        view.setRecordTypeText(HealthRulebook.recordTypeText(record.getRecordType()));
        view.setSystolicPressure(record.getSystolicPressure());
        view.setDiastolicPressure(record.getDiastolicPressure());
        view.setBloodSugar(record.getBloodSugar());
        view.setHeartRate(record.getHeartRate());
        view.setRecordTime(record.getRecordTime());
        view.setWarningLevel(record.getWarningLevel());
        view.setWarningLevelText(HealthRulebook.warningLevelText(record.getWarningLevel()));
        view.setAdvice(record.getAdvice());
        view.setCreateTime(record.getCreateTime());
        return view;
    }
}
