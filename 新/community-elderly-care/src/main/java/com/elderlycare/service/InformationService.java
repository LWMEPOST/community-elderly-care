package com.elderlycare.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.elderlycare.dto.InformationSaveRequest;
import com.elderlycare.dto.InformationView;
import com.elderlycare.entity.Information;

import java.util.List;

public interface InformationService extends IService<Information> {
    InformationView createDraft(Long operatorUserId, InformationSaveRequest request);
    InformationView createAndPublish(Long operatorUserId, InformationSaveRequest request);
    InformationView updateInformation(Long informationId, Long operatorUserId, InformationSaveRequest request);
    InformationView publishInformation(Long informationId, Long operatorUserId);
    InformationView withdrawInformation(Long informationId, Long operatorUserId);
    List<InformationView> getPublishedInformationList(Integer infoType, String keyword, Integer limit);
    List<InformationView> getManageInformationList(Integer infoType, Integer status, String keyword,
                                                   Long publisherId, Integer limit);
    InformationView getInformationDetail(Long id, Long operatorUserId, Integer operatorUserType);
    boolean incrementViewCount(Long id);
}
