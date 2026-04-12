package com.org.cmbms.file.repository;

import com.org.cmbms.file.model.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRecordRepository extends JpaRepository<FileRecord, Long> {
}

