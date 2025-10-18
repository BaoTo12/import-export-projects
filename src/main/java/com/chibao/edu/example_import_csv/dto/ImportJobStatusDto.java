package com.chibao.edu.example_import_csv.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportJobStatusDto {
    private UUID jobId;
    private String status;
    private int totalRows;
    private int successCount;
    private int failedCount;
    private String errorReportPath; // or boolean hasErrors
    private String message;

    public static ImportJobStatusDto from(ImportJobStatus job) {
        if (job == null) return null;
        return new ImportJobStatusDto(
                job.getId(),
                job.getStatus() == null ? null : job.getStatus().name(),
                job.getTotalRows(),
                job.getSuccessCount(),
                job.getFailedCount(),
                job.getErrorReportPath(),
                job.getMessage()
        );
    }
}
