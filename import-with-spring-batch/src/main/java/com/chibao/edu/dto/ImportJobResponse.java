package com.chibao.edu.dto;

import com.chibao.edu.common.ImportJobStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ImportJobResponse {
    private String jobId;
    private ImportJobStatus status;
    private LocalDateTime createdAt;
    private String fileName;
    private Integer totalRows;
}