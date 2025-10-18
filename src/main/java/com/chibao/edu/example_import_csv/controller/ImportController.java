package com.chibao.edu.example_import_csv.controller;

import com.chibao.edu.example_import_csv.common.ImportOption;
import com.chibao.edu.example_import_csv.dto.ImportJobStatusDto;
import com.chibao.edu.example_import_csv.dto.ImportPreviewResponse;
import com.chibao.edu.example_import_csv.service.ImportService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients/import")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    // Preview or kick-off import depending on query params
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportPreviewResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "preview", defaultValue = "true") boolean preview,
            @RequestParam(value = "option", required = false) ImportOption option
    ) throws IOException {
        if (!isCsv(file)) {
            return ResponseEntity.badRequest().body(ImportPreviewResponse.error("Only CSV files allowed"));
        }
        if (file.getSize() > 5_000_000) { // example size check
            return ResponseEntity.badRequest().body(ImportPreviewResponse.error("File too large"));
        }

        if (preview) {
            // parse and return preview
            ImportPreviewResponse resp = importService.preview(file.getInputStream());
            return ResponseEntity.ok(resp);
        } else {
            if (option == null) return ResponseEntity.badRequest().body(ImportPreviewResponse.error("option required for import"));
            UUID jobId = importService.startImport(file.getInputStream(), option);
            return ResponseEntity.accepted().body(ImportPreviewResponse.started(jobId));
        }
    }

    @GetMapping("/{jobId}/status")
    public ResponseEntity<ImportJobStatusDto> status(@PathVariable UUID jobId) {
        ImportJobStatusDto dto = importService.getStatus(jobId);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{jobId}/errors")
    public ResponseEntity<Resource> downloadErrors(@PathVariable UUID jobId) {
        Resource res = importService.getErrorReport(jobId);
        if (res == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"errors-"+jobId+".csv\"")
                .body(res);
    }

    private boolean isCsv(MultipartFile file) {
        String fn = file.getOriginalFilename();
        return fn != null && fn.toLowerCase().endsWith(".csv");
    }
}
