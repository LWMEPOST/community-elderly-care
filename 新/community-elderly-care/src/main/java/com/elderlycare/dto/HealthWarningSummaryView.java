package com.elderlycare.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HealthWarningSummaryView {

    private Long elderlyId;
    private String elderlyName;
    private long totalRecords;
    private long normalCount;
    private long lowWarningCount;
    private long highWarningCount;
    private long abnormalCount;
    private LocalDateTime latestRecordTime;
    private HealthRecordView latestWarningRecord;
}
