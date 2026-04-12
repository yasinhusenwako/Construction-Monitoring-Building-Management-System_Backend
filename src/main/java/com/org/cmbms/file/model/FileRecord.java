package com.org.cmbms.file.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "files")
@Getter
@Setter
public class FileRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long requestId;
    private String requestType;
    private String fileName;
    private String filePath;
    private Long uploadedBy;
    private LocalDateTime uploadedAt;
}

