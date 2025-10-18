package com.chibao.edu.example_import_csv.dto;

import com.chibao.edu.example_import_csv.common.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "import_job_status")
public class ImportJobStatus {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private Status status; // PENDING, RUNNING, SUCCESS, FAILED, CANCELLED

    private Instant createdAt;
    private Instant updatedAt;

    private int totalRows;
    private int successCount;
    private int failedCount;

    // path to generated error CSV (or stored blob reference)
    private String errorReportPath;

    private String message; // free-text for errors
}