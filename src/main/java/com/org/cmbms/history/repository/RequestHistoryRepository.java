package com.org.cmbms.history.repository;

import com.org.cmbms.history.model.RequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Long> {
    List<RequestHistory> findByRequestIdAndRequestTypeOrderByCreatedAtDesc(Long requestId, String requestType);
}
