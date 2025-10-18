package com.chibao.edu.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResponse {
    private String jobId;
    private String fileName;
    private Integer totalRecords;
    private List validationErrors;
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        private Integer rowNumber;
        private String field;
        private String error;
        private String value;
    }
}