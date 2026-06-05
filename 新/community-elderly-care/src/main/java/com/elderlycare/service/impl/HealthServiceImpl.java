package com.elderlycare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elderlycare.dto.HealthRecordCreateRequest;
import com.elderlycare.dto.HealthRecordView;
import com.elderlycare.dto.HealthWarningSummaryView;
import com.elderlycare.entity.HealthRecord;
import com.elderlycare.entity.User;
import com.elderlycare.exception.BusinessException;
import com.elderlycare.exception.ForbiddenException;
import com.elderlycare.mapper.HealthRecordMapper;
import com.elderlycare.service.HealthService;
import com.elderlycare.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HealthServiceImpl extends ServiceImpl<HealthRecordMapper, HealthRecord> implements HealthService {

    @Autowired
    private HealthRecordMapper healthRecordMapper;

    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public HealthRecordView addHealthRecord(Long operatorUserId, Integer operatorUserType, HealthRecordCreateRequest request) {
        Long elderlyId = resolveTargetElderlyId(operatorUserId, operatorUserType, request.getElderlyId());
        ensureHealthAccess(elderlyId, operatorUserId, operatorUserType, true);
        validateRecordPayload(request);

        HealthRecord record = new HealthRecord();
        record.setElderlyId(elderlyId);
        record.setRecordType(request.getRecordType());
        record.setSystolicPressure(request.getSystolicPressure());
        record.setDiastolicPressure(request.getDiastolicPressure());
        record.setBloodSugar(request.getBloodSugar());
        record.setHeartRate(request.getHeartRate());
        record.setRecordTime(request.getRecordTime());
        record.setCreateTime(LocalDateTime.now());
        record.setWarningLevel(calculateWarningLevel(record));
        record.setAdvice(generateAdvice(record.getRecordType(), record.getSystolicPressure(),
                record.getDiastolicPressure(), record.getBloodSugar(), record.getHeartRate()));
        healthRecordMapper.insert(record);
        return buildView(record);
    }

    @Override
    public List<HealthRecordView> getHealthRecords(Long operatorUserId, Integer operatorUserType,
                                                   Long elderlyId, Integer recordType, Integer warningLevel, Integer limit) {
        Long targetElderlyId = resolveTargetElderlyId(operatorUserId, operatorUserType, elderlyId);
        ensureHealthAccess(targetElderlyId, operatorUserId, operatorUserType, false);
        LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecord::getElderlyId, targetElderlyId)
               .eq(recordType != null, HealthRecord::getRecordType, recordType)
               .eq(warningLevel != null, HealthRecord::getWarningLevel, warningLevel)
               .orderByDesc(HealthRecord::getRecordTime);
        if (limit != null) {
            wrapper.last("LIMIT " + normalizeLimit(limit));
        }
        return buildViews(healthRecordMapper.selectList(wrapper));
    }

    @Override
    public HealthRecordView getLatestRecord(Long operatorUserId, Integer operatorUserType, Long elderlyId, Integer recordType) {
        Long targetElderlyId = resolveTargetElderlyId(operatorUserId, operatorUserType, elderlyId);
        ensureHealthAccess(targetElderlyId, operatorUserId, operatorUserType, false);
        LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecord::getElderlyId, targetElderlyId)
               .eq(HealthRecord::getRecordType, recordType)
               .orderByDesc(HealthRecord::getRecordTime)
               .last("LIMIT 1");
        HealthRecord record = healthRecordMapper.selectOne(wrapper);
        return record == null ? null : buildView(record);
    }

    @Override
    public HealthWarningSummaryView getWarningSummary(Long operatorUserId, Integer operatorUserType, Long elderlyId) {
        Long targetElderlyId = resolveTargetElderlyId(operatorUserId, operatorUserType, elderlyId);
        ensureHealthAccess(targetElderlyId, operatorUserId, operatorUserType, false);
        List<HealthRecord> records = healthRecordMapper.selectList(new LambdaQueryWrapper<HealthRecord>()
                .eq(HealthRecord::getElderlyId, targetElderlyId)
                .orderByDesc(HealthRecord::getRecordTime));
        User elderlyUser = userService.getUserInfo(targetElderlyId);
        long normalCount = records.stream().filter(record -> Integer.valueOf(0).equals(record.getWarningLevel())).count();
        long lowWarningCount = records.stream().filter(record -> Integer.valueOf(1).equals(record.getWarningLevel())).count();
        long highWarningCount = records.stream().filter(record -> Integer.valueOf(2).equals(record.getWarningLevel())).count();
        HealthRecord latestWarning = records.stream()
                .filter(record -> record.getWarningLevel() != null && record.getWarningLevel() > 0)
                .findFirst()
                .orElse(null);
        return HealthWarningSummaryView.builder()
                .elderlyId(targetElderlyId)
                .elderlyName(elderlyUser == null ? null : elderlyUser.getRealName())
                .totalRecords(records.size())
                .normalCount(normalCount)
                .lowWarningCount(lowWarningCount)
                .highWarningCount(highWarningCount)
                .abnormalCount(lowWarningCount + highWarningCount)
                .latestRecordTime(records.isEmpty() ? null : records.get(0).getRecordTime())
                .latestWarningRecord(latestWarning == null ? null : HealthRecordView.from(latestWarning, elderlyUser))
                .build();
    }

    @Override
    public List<HealthRecordView> getWarningRecords(Long operatorUserId, Integer operatorUserType,
                                                    Long elderlyId, Integer warningLevel, Integer limit) {
        Long targetElderlyId = resolveTargetElderlyId(operatorUserId, operatorUserType, elderlyId);
        ensureHealthAccess(targetElderlyId, operatorUserId, operatorUserType, false);
        LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecord::getElderlyId, targetElderlyId)
                .gt(HealthRecord::getWarningLevel, 0)
                .eq(warningLevel != null, HealthRecord::getWarningLevel, warningLevel)
                .orderByDesc(HealthRecord::getRecordTime);
        if (limit != null) {
            wrapper.last("LIMIT " + normalizeLimit(limit));
        }
        return buildViews(healthRecordMapper.selectList(wrapper));
    }

    private Integer calculateWarningLevel(HealthRecord record) {
        Integer recordType = record.getRecordType();
        if (recordType == 1) {
            int systolic = record.getSystolicPressure() != null ? record.getSystolicPressure() : 0;
            int diastolic = record.getDiastolicPressure() != null ? record.getDiastolicPressure() : 0;
            if (systolic > 180 || diastolic > 110) return 2;
            if (systolic < 90 || diastolic < 60) return 1;
        } else if (recordType == 2) {
            BigDecimal bloodSugar = record.getBloodSugar();
            if (bloodSugar != null) {
                if (bloodSugar.compareTo(new BigDecimal("13.9")) > 0
                        || bloodSugar.compareTo(new BigDecimal("3.9")) < 0) {
                    return 2;
                }
                if (bloodSugar.compareTo(new BigDecimal("11.1")) > 0
                        || bloodSugar.compareTo(new BigDecimal("4.4")) < 0) {
                    return 1;
                }
            }
        }
        return 0;
    }

    @Override
    public String generateAdvice(Integer recordType, Integer systolic, Integer diastolic, BigDecimal bloodSugar, Integer heartRate) {
        if (recordType == 1) {
            if (systolic != null && diastolic != null) {
                if (systolic > 180 || diastolic > 110) {
                    return "血压严重偏高，建议立即就医！";
                } else if (systolic > 140 || diastolic > 90) {
                    return "血压偏高，建议减少盐分摄入，适量运动，定期监测。";
                } else if (systolic < 90 || diastolic < 60) {
                    return "血压偏低，建议适当补充营养，注意休息。";
                }
            }
        } else if (recordType == 2) {
            if (bloodSugar != null) {
                if (bloodSugar.compareTo(new BigDecimal("13.9")) > 0) {
                    return "血糖严重偏高，建议立即就医！";
                } else if (bloodSugar.compareTo(new BigDecimal("11.1")) > 0) {
                    return "血糖偏高，建议控制饮食，适当运动。";
                } else if (bloodSugar.compareTo(new BigDecimal("3.9")) < 0) {
                    return "血糖偏低，建议及时补充糖分。";
                }
            }
        } else if (recordType == 3) {
            if (heartRate != null) {
                if (heartRate > 100) {
                    return "心率偏快，建议休息，避免剧烈运动。";
                } else if (heartRate < 60) {
                    return "心率偏慢，建议咨询医生。";
                }
            }
        }
        return "数据正常，请继续保持健康的生活方式。";
    }

    private Long resolveTargetElderlyId(Long operatorUserId, Integer operatorUserType, Long elderlyId) {
        if (elderlyId != null) {
            return elderlyId;
        }
        if (Integer.valueOf(1).equals(operatorUserType)) {
            return operatorUserId;
        }
        throw new BusinessException(400, "请指定老人用户ID");
    }

    private void ensureHealthAccess(Long targetElderlyId, Long operatorUserId, Integer operatorUserType, boolean forWrite) {
        User elderlyUser = userService.getUserInfo(targetElderlyId);
        if (elderlyUser == null || !Integer.valueOf(1).equals(elderlyUser.getUserType())) {
            throw new BusinessException(404, "老人用户不存在");
        }
        if (!Integer.valueOf(1).equals(elderlyUser.getStatus())) {
            throw new BusinessException(400, "老人账号未启用");
        }

        boolean allowed = switch (operatorUserType) {
            case 3 -> true;
            case 1 -> operatorUserId.equals(targetElderlyId);
            case 2 -> userService.hasConfirmedBinding(targetElderlyId, operatorUserId);
            default -> false;
        };
        if (!allowed) {
            throw new ForbiddenException(forWrite ? "无权维护该老人健康记录" : "无权查看该老人健康记录");
        }
    }

    private void validateRecordPayload(HealthRecordCreateRequest request) {
        Integer recordType = request.getRecordType();
        if (Integer.valueOf(1).equals(recordType)) {
            if (request.getSystolicPressure() == null || request.getDiastolicPressure() == null) {
                throw new BusinessException(400, "血压记录必须提供收缩压和舒张压");
            }
        } else if (Integer.valueOf(2).equals(recordType)) {
            if (request.getBloodSugar() == null) {
                throw new BusinessException(400, "血糖记录必须提供血糖值");
            }
        } else if (Integer.valueOf(3).equals(recordType)) {
            if (request.getHeartRate() == null) {
                throw new BusinessException(400, "心率记录必须提供心率值");
            }
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 20;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }

    private List<HealthRecordView> buildViews(List<HealthRecord> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        Map<Long, User> userMap = new HashMap<>(userService.listByIds(records.stream()
                        .map(HealthRecord::getElderlyId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user)));
        return records.stream()
                .map(record -> HealthRecordView.from(record, userMap.get(record.getElderlyId())))
                .collect(Collectors.toList());
    }

    private HealthRecordView buildView(HealthRecord record) {
        return HealthRecordView.from(record, userService.getUserInfo(record.getElderlyId()));
    }
}
