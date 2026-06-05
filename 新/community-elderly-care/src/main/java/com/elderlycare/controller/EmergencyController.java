package com.elderlycare.controller;

import com.elderlycare.auth.AuthenticatedUser;
import com.elderlycare.auth.RequireUserTypes;
import com.elderlycare.auth.UserContextHolder;
import com.elderlycare.common.Result;
import com.elderlycare.dto.EmergencyHelpCreateRequest;
import com.elderlycare.dto.EmergencyHelpRespondRequest;
import com.elderlycare.dto.EmergencyHelpView;
import com.elderlycare.service.EmergencyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/emergency")
public class EmergencyController {

    @Autowired
    private EmergencyService emergencyService;

    @RequireUserTypes({1, 2, 3})
    @PostMapping("/help")
    public Result<EmergencyHelpView> createEmergency(@Valid @RequestBody EmergencyHelpCreateRequest request) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        EmergencyHelpView created = emergencyService.createEmergency(
                currentUser.getUserId(), currentUser.getUserType(), request);
        return Result.success("求助已发起", created);
    }

    @RequireUserTypes({1, 2, 3, 4})
    @GetMapping("/list")
    public Result<List<EmergencyHelpView>> getEmergencyList(@RequestParam(required = false) Long elderlyId,
                                                            @RequestParam(required = false) Integer status,
                                                            @RequestParam(required = false) Integer limit) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        List<EmergencyHelpView> list = emergencyService.getEmergencyList(
                currentUser.getUserId(), currentUser.getUserType(), elderlyId, status, limit);
        return Result.success(list);
    }

    @RequireUserTypes({1, 2, 3, 4})
    @GetMapping("/{id}")
    public Result<EmergencyHelpView> getEmergencyDetail(@PathVariable Long id) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        EmergencyHelpView detail = emergencyService.getEmergencyDetail(
                id, currentUser.getUserId(), currentUser.getUserType());
        return Result.success(detail);
    }

    @RequireUserTypes({3, 4})
    @PutMapping("/{id}/response")
    public Result<EmergencyHelpView> responseEmergency(@PathVariable Long id,
                                                       @RequestBody(required = false) EmergencyHelpRespondRequest request) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        EmergencyHelpView emergencyHelp = emergencyService.respondEmergency(
                id,
                currentUser.getUserId(),
                currentUser.getUserType(),
                request == null ? null : request.getResponseUserId());
        return Result.success("求助已响应", emergencyHelp);
    }

    @RequireUserTypes({3, 4})
    @PutMapping("/{id}/resolve")
    public Result<EmergencyHelpView> resolveEmergency(@PathVariable Long id) {
        AuthenticatedUser currentUser = UserContextHolder.requireCurrentUser();
        EmergencyHelpView emergencyHelp = emergencyService.resolveEmergency(
                id, currentUser.getUserId(), currentUser.getUserType());
        return Result.success("求助已解决", emergencyHelp);
    }
}
