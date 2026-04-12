package com.org.cmbms.maintenance.repository;

import com.org.cmbms.common.enums.RequestType;
import com.org.cmbms.maintenance.model.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    List<StatusHistory> findByRequestTypeAndRequestId(RequestType requestType, Long requestId);
}

