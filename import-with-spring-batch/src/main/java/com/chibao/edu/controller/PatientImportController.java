package com.chibao.edu.controller;

import com.chibao.edu.common.DuplicateStrategy;
import com.chibao.edu.dto.ImportJobRequest;
import com.chibao.edu.dto.ImportJobResponse;
import com.chibao.edu.dto.ImportJobStatusResponse;
import com.chibao.edu.service.impl.ImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientImportController {

    private final ImportService importService;

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportJobResponse> importPatients(
            @RequestParam("file") MultipartFile file,
            @RequestParam("duplicateStrategy") String duplicateStrategy) {

        try {
            log.info("Received import request for file: {}", file.getOriginalFilename());

            ImportJobRequest request = new ImportJobRequest(
                    DuplicateStrategy.valueOf(duplicateStrategy.toUpperCase())
            );

            ImportJobResponse response = importService.initiateImport(file, request);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request parameters", e);
            throw new RuntimeException("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error processing import request", e);
            throw new RuntimeException("Failed to process import: " + e.getMessage());
        }
    }

    @GetMapping("/import/{jobId}/status")
    public ResponseEntity<ImportJobStatusResponse> getImportStatus(@PathVariable String jobId) {
        try {
            log.info("Fetching status for job: {}", jobId);

            ImportJobStatusResponse response = importService.getJobStatus(jobId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching job status", e);
            throw new RuntimeException("Failed to fetch job status: " + e.getMessage());
        }
    }
}
