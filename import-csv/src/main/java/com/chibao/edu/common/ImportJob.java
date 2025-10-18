package com.chibao.edu.common;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "import_job")
@Data
public class ImportJob {
    @Id
    private UUID jobId;


    private String filename;
    private String filePath;
    private String status;
    private String errorFile;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}