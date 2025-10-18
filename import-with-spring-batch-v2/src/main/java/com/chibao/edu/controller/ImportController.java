package com.chibao.edu.controller;

import com.chibao.edu.common.DuplicateHandlingStrategy;
import com.chibao.edu.dtos.ImportResponse;
import com.chibao.edu.dtos.ImportStatusResponse;
import com.chibao.edu.service.ImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @PostMapping("/import")
    public ResponseEntity importPatients(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "duplicateStrategy", defaultValue = "SKIP")
            DuplicateHandlingStrategy duplicateStrategy,
            @RequestParam(value = "autoStart", defaultValue = "true") boolean autoStart) {

        try {
            log.info("Received import request for file: {}", file.getOriginalFilename());

            // Validate and preview
            ImportResponse response = importService.validateAndPreview(file, duplicateStrategy);

            // Start import if autoStart is true
            if (autoStart) {
                importService.startImport(response.getJobId(), file, duplicateStrategy);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Import validation failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ImportResponse.builder()
                            .message("Validation failed: " + e.getMessage())
                            .build());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/import/{jobId}/start")
    public ResponseEntity startImport(
            @PathVariable String jobId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "duplicateStrategy", defaultValue = "SKIP")
            DuplicateHandlingStrategy duplicateStrategy) {

        try {
            importService.startImport(jobId, file, duplicateStrategy);
            return ResponseEntity.ok("Import started successfully");
        } catch (Throwable e) {
            log.error("Failed to start import", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to start import: " + e.getMessage());
        }
    }

    @GetMapping("/import/{jobId}/status")
    public ResponseEntity getImportStatus(@PathVariable String jobId) {
        try {
            ImportStatusResponse status = importService.getImportStatus(jobId);
            return ResponseEntity.ok(status);
        } catch (Throwable e) {
            log.error("Failed to get import status", e);
            return ResponseEntity.notFound().build();
        }
    }
}