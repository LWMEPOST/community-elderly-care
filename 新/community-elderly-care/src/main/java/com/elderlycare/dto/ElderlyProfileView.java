package com.elderlycare.dto;

import com.elderlycare.entity.ElderlyInfo;
import com.elderlycare.entity.User;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ElderlyProfileView {

    private Long id;
    private Long userId;
    private String realName;
    private String phone;
    private Integer age;
    private Integer gender;
    private String healthStatus;
    private String medicalHistory;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static ElderlyProfileView from(User user, ElderlyInfo elderlyInfo) {
        ElderlyProfileView view = new ElderlyProfileView();
        view.setUserId(user.getId());
        view.setRealName(user.getRealName());
        view.setPhone(user.getPhone());
        if (elderlyInfo != null) {
            view.setId(elderlyInfo.getId());
            view.setAge(elderlyInfo.getAge());
            view.setGender(elderlyInfo.getGender());
            view.setHealthStatus(elderlyInfo.getHealthStatus());
            view.setMedicalHistory(elderlyInfo.getMedicalHistory());
            view.setLongitude(elderlyInfo.getLongitude());
            view.setLatitude(elderlyInfo.getLatitude());
            view.setCreateTime(elderlyInfo.getCreateTime());
            view.setUpdateTime(elderlyInfo.getUpdateTime());
        }
        return view;
    }
}
