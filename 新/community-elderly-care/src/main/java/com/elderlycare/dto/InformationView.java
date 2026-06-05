package com.elderlycare.dto;

import com.elderlycare.entity.Information;
import com.elderlycare.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InformationView {

    private Long id;
    private String title;
    private String content;
    private String contentSummary;
    private Integer infoType;
    private String infoTypeText;
    private Long publisherId;
    private String publisherName;
    private String coverImage;
    private Integer status;
    private String statusText;
    private Integer viewCount;
    private LocalDateTime createTime;
    private LocalDateTime publishTime;
    private Boolean canEdit;
    private Boolean canPublish;
    private Boolean canWithdraw;

    public static InformationView from(Information information, User publisher) {
        InformationView view = new InformationView();
        view.setId(information.getId());
        view.setTitle(information.getTitle());
        view.setContent(information.getContent());
        view.setContentSummary(InformationRulebook.summarize(information.getContent()));
        view.setInfoType(information.getInfoType());
        view.setInfoTypeText(InformationRulebook.infoTypeText(information.getInfoType()));
        view.setPublisherId(information.getPublisherId());
        view.setPublisherName(publisher == null ? null : publisher.getRealName());
        view.setCoverImage(information.getCoverImage());
        view.setStatus(information.getStatus());
        view.setStatusText(InformationRulebook.statusText(information.getStatus()));
        view.setViewCount(information.getViewCount());
        view.setCreateTime(information.getCreateTime());
        view.setPublishTime(information.getPublishTime());
        return view;
    }
}
