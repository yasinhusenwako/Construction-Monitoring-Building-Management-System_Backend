
package com.org.cmbms.reporting.controller;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.security.SecurityUtils;
import com.org.cmbms.reporting.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview() {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        if (currentUser.getRole() == com.org.cmbms.common.enums.Role.PROFESSIONAL) {
            throw new com.org.cmbms.common.exception.ApiException("Access denied");
        }
        return ResponseEntity.ok(reportService.overview());
    }

    @GetMapping("/mttr")
    public ResponseEntity<Map<String, Object>> mttr() {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        if (currentUser.getRole() == com.org.cmbms.common.enums.Role.PROFESSIONAL) {
            throw new com.org.cmbms.common.exception.ApiException("Access denied");
        }
        return ResponseEntity.ok(reportService.mttr());
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> analytics() {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        if (currentUser.getRole() == com.org.cmbms.common.enums.Role.PROFESSIONAL) {
            throw new com.org.cmbms.common.exception.ApiException("Access denied");
        }
        return ResponseEntity.ok(reportService.analytics());
    }
}
