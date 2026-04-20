
package com.org.cmbms.project.controller;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.enums.Role;
import com.org.cmbms.common.security.SecurityUtils;
import com.org.cmbms.file.model.FileRecord;
import com.org.cmbms.project.dto.BoqRequest;
import com.org.cmbms.project.dto.BoqResponse;
import com.org.cmbms.project.dto.ProjectDocUploadResponse;
import com.org.cmbms.project.dto.ProjectRequestDTO;
import com.org.cmbms.project.model.Project;
import com.org.cmbms.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<Project> create(@Valid @RequestBody ProjectRequestDTO request) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(projectService.create(request, currentUser));
    }

    @GetMapping
    public ResponseEntity<List<Project>> all(@RequestParam(required = false) String status,
                                             @RequestParam(required = false) String priority,
                                             @RequestParam(required = false) String projectId,
                                             @RequestParam(required = false) Long divisionId,
                                             @RequestParam(required = false) Long createdBy,
                                             @RequestParam(required = false) LocalDate startDate,
                                             @RequestParam(required = false) LocalDate endDate) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(projectService.search(currentUser, status, priority, projectId, divisionId, createdBy, startDate, endDate));
    }

    @PostMapping("/{id}/upload-doc")
    public ResponseEntity<ProjectDocUploadResponse> uploadDoc(@PathVariable("id") Long id,
                                                              @RequestParam("file") MultipartFile file) throws IOException {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        FileRecord record = projectService.uploadDoc(id, file, currentUser.getId(), currentUser.getRole());
        return ResponseEntity.ok(new ProjectDocUploadResponse(record.getId(), record.getFileName(), record.getFilePath()));
    }

    @PostMapping("/boq")
    public ResponseEntity<BoqResponse> submitBoq(@Valid @RequestBody BoqRequest request) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(projectService.submitBoq(request.getProjectId(), currentUser));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<Project> review(@PathVariable Long id) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(projectService.supervisorReview(id, currentUser));
    }

    @PatchMapping("/{id}/review")
    public ResponseEntity<Project> adminStartReview(@PathVariable Long id) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        try {
            return ResponseEntity.ok(projectService.adminStartReview(id, currentUser));
        } catch (Exception ex) {
            throw new com.org.cmbms.common.exception.ApiException(ex.getMessage());
        }
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<Project> approve(@PathVariable Long id) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(projectService.adminApprove(id, currentUser));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<Project> reject(@PathVariable Long id) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(projectService.adminReject(id, currentUser));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<Project> close(@PathVariable Long id) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(projectService.adminClose(id, currentUser));
    }
}
