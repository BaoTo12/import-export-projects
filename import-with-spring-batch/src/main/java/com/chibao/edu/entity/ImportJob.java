package com.chibao.edu.entity;

import com.chibao.edu.common.DuplicateStrategy;
import com.chibao.edu.common.ImportJobStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportJob {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private Long batchJobExecutionId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportJobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DuplicateStrategy duplicateStrategy;

    @Column(nullable = false)
    private Integer totalRows;

    @Column(nullable = false)
    private Integer successCount;

    @Column(nullable = false)
    private Integer failedCount;

    @Column(nullable = false)
    private Integer skippedCount;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        startTime = LocalDateTime.now();
        successCount = 0;
        failedCount = 0;
        skippedCount = 0;
    }

    public void incrementSuccess() {
        this.successCount++;
    }

    public void incrementFailed() {
        this.failedCount++;
    }

    public void incrementSkipped() {
        this.skippedCount++;
    }
}
