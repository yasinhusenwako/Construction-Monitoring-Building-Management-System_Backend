
package com.org.cmbms.reporting.service;

import com.org.cmbms.common.enums.Status;
import com.org.cmbms.maintenance.model.MaintenanceRequest;
import com.org.cmbms.maintenance.model.WorkOrder;
import com.org.cmbms.maintenance.repository.MaintenanceRepository;
import com.org.cmbms.maintenance.repository.WorkOrderRepository;
import com.org.cmbms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final MaintenanceRepository maintenanceRepository;
    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;

    public Map<String, Object> overview() {
        List<MaintenanceRequest> all = maintenanceRepository.findAll();
        Map<String, Long> byStatus = all.stream()
                .filter(r -> r.getStatus() != null && r.getStatus().getValue() != null)
                .collect(Collectors.groupingBy(r -> r.getStatus().getValue(), Collectors.counting()));
        Map<String, Object> out = new HashMap<>();
        out.put("totalRequests", all.size());
        out.put("statusBreakdown", byStatus);
        out.put("divisionRequestVolume", all.stream()
                .filter(r -> r.getDivisionId() != null)
                .collect(Collectors.groupingBy(MaintenanceRequest::getDivisionId, Collectors.counting())));
        return out;
    }

    public Map<String, Object> mttr() {
        List<MaintenanceRequest> completed = maintenanceRepository.findByStatus(Status.COMPLETED);
        double avgHours = completed.stream()
                .filter(r -> r.getCreatedAt() != null)
                .mapToLong(r -> Duration.between(r.getCreatedAt(), LocalDateTime.now()).toHours())
                .average()
                .orElse(0);
        return Map.of("mttrHours", avgHours, "sampleSize", completed.size());
    }

    public Map<String, Object> analytics() {
        List<MaintenanceRequest> all = maintenanceRepository.findAll();
        Map<String, Long> byDivision = all.stream()
                .filter(r -> r.getDivisionId() != null)
                .collect(Collectors.groupingBy(MaintenanceRequest::getDivisionId, Collectors.counting()));
        Map<String, Long> supervisorPerformance = all.stream()
                .filter(r -> r.getAssignedSupervisorId() != null)
                .collect(Collectors.groupingBy(MaintenanceRequest::getAssignedSupervisorId, Collectors.counting()));
        Map<String, Long> professionalWorkload = workOrderRepository.findAll().stream()
                .filter(w -> w.getAssignedProfessionalId() != null)
                .collect(Collectors.groupingBy(WorkOrder::getAssignedProfessionalId, Collectors.counting()));

        Map<String, Object> out = new HashMap<>();
        out.put("requestsByDivision", byDivision);
        out.put("supervisorPerformance", supervisorPerformance);
        out.put("professionalWorkload", professionalWorkload);

        List<?> supervisors = userRepository.findByRole(com.org.cmbms.common.enums.Role.SUPERVISOR);
        List<?> professionals = userRepository.findByRole(com.org.cmbms.common.enums.Role.PROFESSIONAL);

        out.put("supervisorCount", supervisors != null ? supervisors.size() : 0);
        out.put("professionalCount", professionals != null ? professionals.size() : 0);
        return out;
    }
}
