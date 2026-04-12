package com.org.cmbms.maintenance.repository;

import com.org.cmbms.maintenance.model.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    Optional<WorkOrder> findByMaintenanceRequestId(Long maintenanceRequestId);
    List<WorkOrder> findByAssignedProfessionalId(Long assignedProfessionalId);
}

