package com.chibao.edu.dto;

import com.chibao.edu.common.ImportJobStatus;
import com.chibao.edu.entity.ImportError;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ImportJobStatusResponse {
    private String jobId;
    private ImportJobStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalRows;
    private Integer successCount;
    private Integer failedCount;
    private Integer skippedCount;
    private List<ImportError> errors;
    private Double progress;
}