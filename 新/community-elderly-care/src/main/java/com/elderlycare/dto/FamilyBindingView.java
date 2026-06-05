package com.elderlycare.dto;

import com.elderlycare.entity.FamilyBinding;
import com.elderlycare.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FamilyBindingView {

    private Long id;
    private Long elderlyId;
    private String elderlyName;
    private Long familyId;
    private String familyName;
    private String relation;
    private Integer status;
    private LocalDateTime createTime;

    public static FamilyBindingView from(FamilyBinding binding, User elderlyUser, User familyUser) {
        FamilyBindingView view = new FamilyBindingView();
        view.setId(binding.getId());
        view.setElderlyId(binding.getElderlyId());
        view.setElderlyName(elderlyUser == null ? null : elderlyUser.getRealName());
        view.setFamilyId(binding.getFamilyId());
        view.setFamilyName(familyUser == null ? null : familyUser.getRealName());
        view.setRelation(binding.getRelation());
        view.setStatus(binding.getStatus());
        view.setCreateTime(binding.getCreateTime());
        return view;
    }
}
