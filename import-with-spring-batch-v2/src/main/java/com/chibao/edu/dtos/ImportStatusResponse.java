package com.chibao.edu.dtos;

import com.chibao.edu.common.ImportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportStatusResponse {
    private String jobId;
    private String fileName;
    private ImportStatus status;
    private Integer totalRecords;
    private Integer processedRecords;
    private Integer successCount;
    private Integer failedCount;
    private Integer skippedCount;
    private Integer progressPercentage;
    private String errorMessage;
    private boolean hasErrorReport;
}