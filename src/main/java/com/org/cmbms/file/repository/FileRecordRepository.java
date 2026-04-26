package com.org.cmbms.file.repository;

import com.org.cmbms.file.model.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRecordRepository extends JpaRepository<FileRecord, Long> {
    List<FileRecord> findByRequestIdAndRequestType(Long requestId, String requestType);
}

