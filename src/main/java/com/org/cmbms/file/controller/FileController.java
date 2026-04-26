package com.org.cmbms.file.controller;

import com.org.cmbms.file.model.FileRecord;
import com.org.cmbms.file.repository.FileRecordRepository;
import com.org.cmbms.file.service.FileStorageService;
import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.maintenance.repository.MaintenanceRepository;
import com.org.cmbms.project.repository.ProjectRepository;
import com.org.cmbms.space.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLConnection;
import java.util.*;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileRecordRepository fileRecordRepository;
    private final FileStorageService fileStorageService;
    private final MaintenanceRepository maintenanceRepository;
    private final ProjectRepository projectRepository;
    private final SpaceRepository spaceRepository;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("entityType") String entityType,
            @RequestParam("entityId") String entityId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        try {
            // Validate entity type
            String normalizedType = entityType.toLowerCase();
            if (!Arrays.asList("maintenance", "project", "booking").contains(normalizedType)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid entity type. Must be: maintenance, project, or booking"));
            }

            // Validate file count
            if (files.length > 10) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Maximum 10 files allowed per upload"));
            }

            // Get numeric ID from business ID
            Long numericId = getNumericIdForEntity(normalizedType, entityId);
            if (numericId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Entity not found with ID: " + entityId));
            }

            List<Map<String, Object>> uploadedFiles = new ArrayList<>();
            Long userId = userPrincipal != null ? userPrincipal.getId() : null;

            for (MultipartFile file : files) {
                // Validate file
                validateFile(file);

                // Save file
                FileRecord record = fileStorageService.save(
                        numericId,
                        normalizedType.toUpperCase(),
                        file,
                        userId
                );

                // Build response for this file
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("id", record.getId().toString());
                fileInfo.put("name", record.getFileName());
                fileInfo.put("originalName", record.getFileName());
                fileInfo.put("size", file.getSize());
                fileInfo.put("type", file.getContentType());
                fileInfo.put("url", "/api/files/download/" + record.getId());
                fileInfo.put("uploadedAt", record.getUploadedAt().toString());
                fileInfo.put("entityType", entityType);
                fileInfo.put("entityId", entityId);

                uploadedFiles.add(fileInfo);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Files uploaded successfully");
            response.put("files", uploadedFiles);

            return ResponseEntity.ok(response);

        } catch (FileSizeExceededException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (InvalidFileTypeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal Server Error");
            errorResponse.put("message", "Failed to upload files: " + e.getMessage());
            errorResponse.put("status", 500);
            errorResponse.put("timestamp", new Date().toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/request/{type}/{id}")
    public ResponseEntity<List<FileRecord>> getFilesForRequest(@PathVariable String type, @PathVariable Long id) {
        return ResponseEntity.ok(fileRecordRepository.findByRequestIdAndRequestType(id, type.toUpperCase()));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) throws IOException {
        FileRecord record = fileRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        Resource resource = fileStorageService.getFileAsResource(record.getFilePath());
        
        String contentType = URLConnection.guessContentTypeFromName(record.getFileName());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + record.getFileName() + "\"")
                .body(resource);
    }

    /**
     * Get numeric database ID from business ID by looking up the entity
     */
    private Long getNumericIdForEntity(String entityType, String businessId) {
        try {
            switch (entityType) {
                case "maintenance":
                    return maintenanceRepository.findByMaintenanceId(businessId)
                            .map(m -> m.getId())
                            .orElse(null);
                case "project":
                    return projectRepository.findByProjectId(businessId)
                            .map(p -> p.getId())
                            .orElse(null);
                case "booking":
                    return spaceRepository.findByBookingId(businessId)
                            .map(b -> b.getId())
                            .orElse(null);
                default:
                    return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Validate file size and type
     */
    private void validateFile(MultipartFile file) throws FileSizeExceededException, InvalidFileTypeException {
        // Size validation (10MB max)
        long maxSize = 10 * 1024 * 1024; // 10MB in bytes
        if (file.getSize() > maxSize) {
            throw new FileSizeExceededException("File '" + file.getOriginalFilename() + "' exceeds 10MB limit");
        }

        // Type validation
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new InvalidFileTypeException("Unable to determine file type");
        }

        List<String> allowedTypes = Arrays.asList(
                "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml",
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "text/plain",
                "text/csv"
        );

        if (!allowedTypes.contains(contentType)) {
            throw new InvalidFileTypeException("File type not allowed: " + contentType);
        }
    }

    /**
     * Custom exception for file size validation
     */
    public static class FileSizeExceededException extends Exception {
        public FileSizeExceededException(String message) {
            super(message);
        }
    }

    /**
     * Custom exception for file type validation
     */
    public static class InvalidFileTypeException extends Exception {
        public InvalidFileTypeException(String message) {
            super(message);
        }
    }
}
