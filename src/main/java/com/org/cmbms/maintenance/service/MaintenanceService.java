
package com.org.cmbms.maintenance.service;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.enums.Role;
import com.org.cmbms.common.enums.Status;
import com.org.cmbms.common.exception.ApiException;
import com.org.cmbms.maintenance.dto.CreateMaintenanceRequestDTO;
import com.org.cmbms.maintenance.model.MaintenanceRequest;
import com.org.cmbms.maintenance.repository.MaintenanceRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final WorkflowService workflowService;

    public MaintenanceRequest create(CreateMaintenanceRequestDTO dto, UserPrincipal user) {
        MaintenanceRequest request = new MaintenanceRequest();
        request.setMaintenanceId(dto.getMaintenanceId());
        request.setCategory(dto.getCategory());
        request.setPriority(dto.getPriority());
        request.setDescription(dto.getDescription());
        request.setLocation(dto.getLocation());
        request.setCreatedBy(user.getId());
        request.setCreatedAt(LocalDateTime.now());
        request.setDivisionId(dto.getDivisionId());
        MaintenanceRequest saved = maintenanceRepository.save(request);
        workflowService.initializeSubmittedStatus(saved, user.getId());
        return maintenanceRepository.save(saved);
    }

    public List<MaintenanceRequest> search(UserPrincipal user,
                                           String status,
                                           String priority,
                                           String maintenanceId,
                                           Long divisionId,
                                           Long createdBy) {
        if (user.getRole() == Role.PROFESSIONAL) {
            return maintenanceRepository.findByAssignedProfessionalId(user.getId());
        }
        if (user.getRole() == Role.SUPERVISOR && user.getDivisionId() == null) {
            throw new ApiException("Division not set for supervisor");
        }
        Specification<MaintenanceRequest> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), Status.fromValue(status)));
            }
            if (priority != null && !priority.isBlank()) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (maintenanceId != null && !maintenanceId.isBlank()) {
                predicates.add(cb.equal(root.get("maintenanceId"), maintenanceId));
            }
            if (divisionId != null) {
                predicates.add(cb.equal(root.get("divisionId"), divisionId));
            }
            if (createdBy != null) {
                predicates.add(cb.equal(root.get("createdBy"), createdBy));
            }
            if (user.getRole() == Role.SUPERVISOR) {
                predicates.add(cb.equal(root.get("divisionId"), user.getDivisionId()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return maintenanceRepository.findAll(spec);
    }
}
