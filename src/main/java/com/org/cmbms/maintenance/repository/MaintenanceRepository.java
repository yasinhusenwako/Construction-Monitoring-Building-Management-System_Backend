
package com.org.cmbms.maintenance.repository;

import com.org.cmbms.common.enums.Status;
import com.org.cmbms.maintenance.model.MaintenanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface MaintenanceRepository extends JpaRepository<MaintenanceRequest, Long>, JpaSpecificationExecutor<MaintenanceRequest> {
    List<MaintenanceRequest> findByDivisionId(Long divisionId);
    List<MaintenanceRequest> findByAssignedSupervisorId(Long supervisorId);
    List<MaintenanceRequest> findByAssignedProfessionalId(Long professionalId);
    List<MaintenanceRequest> findByCreatedBy(Long createdBy);
    List<MaintenanceRequest> findByStatus(Status status);
}
