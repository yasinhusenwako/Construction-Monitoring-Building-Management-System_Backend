package com.org.cmbms.preventive.controller;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.enums.Role;
import com.org.cmbms.common.exception.ApiException;
import com.org.cmbms.common.security.SecurityUtils;
import com.org.cmbms.preventive.dto.PreventiveScheduleRequest;
import com.org.cmbms.preventive.model.PreventiveSchedule;
import com.org.cmbms.preventive.service.PreventiveScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/preventive-schedules")
@RequiredArgsConstructor
public class PreventiveScheduleController {

    private final PreventiveScheduleService scheduleService;

    @GetMapping
    public ResponseEntity<List<PreventiveSchedule>> getAllSchedules() {
        return ResponseEntity.ok(scheduleService.getAllSchedules());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PreventiveSchedule> getScheduleById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.getScheduleById(id));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<PreventiveSchedule>> getOverdueSchedules() {
        return ResponseEntity.ok(scheduleService.getOverdueSchedules());
    }

    @GetMapping("/due-soon")
    public ResponseEntity<List<PreventiveSchedule>> getDueSoonSchedules() {
        return ResponseEntity.ok(scheduleService.getDueSoonSchedules());
    }

    @PostMapping
    public ResponseEntity<PreventiveSchedule> createSchedule(@RequestBody PreventiveScheduleRequest request) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ApiException("Only admins can create preventive schedules");
        }
        return ResponseEntity.ok(scheduleService.createSchedule(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PreventiveSchedule> updateSchedule(
            @PathVariable Long id,
            @RequestBody PreventiveScheduleRequest request) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ApiException("Only admins can update preventive schedules");
        }
        return ResponseEntity.ok(scheduleService.updateSchedule(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ApiException("Only admins can delete preventive schedules");
        }
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<PreventiveSchedule> markAsCompleted(@PathVariable Long id) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.PROFESSIONAL) {
            throw new ApiException("Only admins and professionals can mark schedules as completed");
        }
        return ResponseEntity.ok(scheduleService.markAsCompleted(id));
    }
}
