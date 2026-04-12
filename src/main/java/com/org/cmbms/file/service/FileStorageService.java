package com.org.cmbms.file.service;

import com.org.cmbms.file.model.FileRecord;
import com.org.cmbms.file.repository.FileRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    private final FileRecordRepository fileRecordRepository;

    public FileRecord save(Long requestId, String requestType, MultipartFile file, Long uploadedBy) throws IOException {
        Path targetDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(targetDir);
        String storedName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path target = targetDir.resolve(storedName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        FileRecord record = new FileRecord();
        record.setRequestId(requestId);
        record.setRequestType(requestType);
        record.setFileName(file.getOriginalFilename());
        record.setFilePath(target.toString());
        record.setUploadedBy(uploadedBy);
        record.setUploadedAt(LocalDateTime.now());
        return fileRecordRepository.save(record);
    }
}

