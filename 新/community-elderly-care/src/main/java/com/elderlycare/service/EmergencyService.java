package com.elderlycare.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.elderlycare.dto.EmergencyHelpCreateRequest;
import com.elderlycare.dto.EmergencyHelpView;
import com.elderlycare.entity.EmergencyHelp;

import java.util.List;

public interface EmergencyService extends IService<EmergencyHelp> {
    EmergencyHelpView createEmergency(Long operatorUserId, Integer operatorUserType, EmergencyHelpCreateRequest request);
    List<EmergencyHelpView> getEmergencyList(Long operatorUserId, Integer operatorUserType,
                                             Long elderlyId, Integer status, Integer limit);
    EmergencyHelpView getEmergencyDetail(Long emergencyId, Long operatorUserId, Integer operatorUserType);
    EmergencyHelpView respondEmergency(Long emergencyId, Long operatorUserId, Integer operatorUserType, Long responseUserId);
    EmergencyHelpView resolveEmergency(Long emergencyId, Long operatorUserId, Integer operatorUserType);
}
