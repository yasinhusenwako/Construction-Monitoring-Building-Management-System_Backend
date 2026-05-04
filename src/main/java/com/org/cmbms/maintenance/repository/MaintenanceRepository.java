
package com.org.cmbms.maintenance.repository;

import com.org.cmbms.common.enums.Status;
import com.org.cmbms.maintenance.model.MaintenanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface MaintenanceRepository extends JpaRepository<MaintenanceRequest, Long>, JpaSpecificationExecutor<MaintenanceRequest> {
    List<MaintenanceRequest> findByDivisionId(String divisionId);
    List<MaintenanceRequest> findByAssignedSupervisorId(String supervisorId); // Changed to String
    List<MaintenanceRequest> findByAssignedProfessionalId(String professionalId); // Changed to String
    List<MaintenanceRequest> findByCreatedBy(String createdBy); // Changed to String
    List<MaintenanceRequest> findByStatus(Status status);
    Optional<MaintenanceRequest> findByMaintenanceId(String maintenanceId);
}
