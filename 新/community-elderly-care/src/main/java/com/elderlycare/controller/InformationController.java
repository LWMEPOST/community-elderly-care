package com.elderlycare.controller;

import com.elderlycare.auth.AuthenticatedUser;
import com.elderlycare.auth.RequireUserTypes;
import com.elderlycare.auth.UserContextHolder;
import com.elderlycare.common.Result;
import com.elderlycare.dto.InformationSaveRequest;
import com.elderlycare.dto.InformationView;
import com.elderlycare.service.InformationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/information")
public class InformationController {

    @Autowired
    private InformationService informationService;

    @RequireUserTypes({3})
    @PostMapping("/draft")
    public Result<InformationView> createDraft(@Valid @RequestBody InformationSaveRequest request) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        InformationView draft = informationService.createDraft(currentUser.getUserId(), request);
        return Result.success("草稿保存成功", draft);
    }

    @RequireUserTypes({3})
    @PostMapping("/publish")
    public Result<InformationView> publish(@Valid @RequestBody InformationSaveRequest request) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        InformationView published = informationService.createAndPublish(currentUser.getUserId(), request);
        return Result.success("信息发布成功", published);
    }

    @RequireUserTypes({3})
    @PutMapping("/{id}")
    public Result<InformationView> updateInformation(@PathVariable Long id,
                                                     @Valid @RequestBody InformationSaveRequest request) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        InformationView information = informationService.updateInformation(id, currentUser.getUserId(), request);
        return Result.success("资讯更新成功", information);
    }

    @RequireUserTypes({3})
    @PutMapping("/{id}/publish")
    public Result<InformationView> publishInformation(@PathVariable Long id) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        InformationView information = informationService.publishInformation(id, currentUser.getUserId());
        return Result.success("资讯已发布", information);
    }

    @RequireUserTypes({3})
    @PutMapping("/{id}/withdraw")
    public Result<InformationView> withdrawInformation(@PathVariable Long id) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        InformationView information = informationService.withdrawInformation(id, currentUser.getUserId());
        return Result.success("资讯已撤回", information);
    }

    @RequireUserTypes({3})
    @GetMapping("/manage")
    public Result<List<InformationView>> getManageList(@RequestParam(required = false) Integer infoType,
                                                       @RequestParam(required = false) Integer status,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Long publisherId,
                                                       @RequestParam(required = false) Integer limit) {
        List<InformationView> list = informationService.getManageInformationList(
                infoType, status, keyword, publisherId, limit);
        return Result.success(list);
    }

    @RequireUserTypes({1, 2, 3, 4})
    @GetMapping("/list")
    public Result<List<InformationView>> getInformationList(
            @RequestParam(required = false) Integer infoType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer limit) {
        List<InformationView> list = informationService.getPublishedInformationList(infoType, keyword, limit);
        return Result.success(list);
    }

    @RequireUserTypes({1, 2, 3, 4})
    @GetMapping("/{id}")
    public Result<InformationView> getInformationDetail(@PathVariable Long id) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        InformationView info = informationService.getInformationDetail(
                id, currentUser.getUserId(), currentUser.getUserType());
        return Result.success(info);
    }
}
