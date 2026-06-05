package com.elderlycare.dto;

import com.elderlycare.entity.EmergencyHelp;
import com.elderlycare.entity.User;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EmergencyHelpView {

    private Long id;
    private Long elderlyId;
    private String elderlyName;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String locationAddress;
    private Integer helpType;
    private String helpTypeText;
    private String description;
    private Integer status;
    private String statusText;
    private Long responseUserId;
    private String responseUserName;
    private LocalDateTime createTime;
    private LocalDateTime resolveTime;
    private Boolean canRespond;
    private Boolean canResolve;

    public static EmergencyHelpView from(EmergencyHelp emergencyHelp, User elderlyUser, User responseUser) {
        EmergencyHelpView view = new EmergencyHelpView();
        view.setId(emergencyHelp.getId());
        view.setElderlyId(emergencyHelp.getElderlyId());
        view.setElderlyName(elderlyUser == null ? null : elderlyUser.getRealName());
        view.setLongitude(emergencyHelp.getLongitude());
        view.setLatitude(emergencyHelp.getLatitude());
        view.setLocationAddress(emergencyHelp.getLocationAddress());
        view.setHelpType(emergencyHelp.getHelpType());
        view.setHelpTypeText(EmergencyHelpWorkflow.helpTypeText(emergencyHelp.getHelpType()));
        view.setDescription(emergencyHelp.getDescription());
        view.setStatus(emergencyHelp.getStatus());
        view.setStatusText(EmergencyHelpWorkflow.statusText(emergencyHelp.getStatus()));
        view.setResponseUserId(emergencyHelp.getResponseUserId());
        view.setResponseUserName(responseUser == null ? null : responseUser.getRealName());
        view.setCreateTime(emergencyHelp.getCreateTime());
        view.setResolveTime(emergencyHelp.getResolveTime());
        return view;
    }
}
