package com.chibao.edu.controller;

import com.chibao.edu.common.ImportJob;
import com.chibao.edu.common.ImportOption;
import com.chibao.edu.dto.ImportPreviewResponse;
import com.chibao.edu.repository.ImportJobRepository;
import com.chibao.edu.service.ImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients/import")
@RequiredArgsConstructor
public class ImportController {
    private final ImportService importService;
    private final ImportJobRepository importJobRepository;


    // Upload file and return preview (stores file temporarily)
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> uploadAndPreview(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "file is required"));


        String original = file.getOriginalFilename();
        UUID jobId = UUID.randomUUID();
        Path dir = Paths.get(System.getProperty("java.io.tmpdir"), "excel-import", jobId.toString());
        Files.createDirectories(dir);
        Path target = dir.resolve(original);
        file.transferTo(target.toFile());


// call service to parse preview (service will use Excel parser)
        ImportPreviewResponse preview = importService.previewFromFile(target.toString());


// save import job record with path and PREVIEWED status
        ImportJob j = new ImportJob();
        j.setJobId(jobId);
        j.setFilename(original);
        j.setFilePath(target.toString());
        j.setStatus("PREVIEWED");
        j.setCreatedAt(LocalDateTime.now());
        importJobRepository.save(j);


        return ResponseEntity.ok(Map.of("jobId", jobId, "preview", preview));
    }


    // Start import based on stored file
    @PostMapping("/start")
    public ResponseEntity<?> startImport(@RequestParam("jobId") UUID jobId,
                                         @RequestParam("option") ImportOption option) {
        importService.startImportFromStoredFile(jobId, option);
        return ResponseEntity.accepted().body(Map.of("jobId", jobId));
    }


    @GetMapping("/{jobId}/status")
    public ResponseEntity<?> status(@PathVariable UUID jobId) {
        return ResponseEntity.of(importJobRepository.findById(jobId));
    }
}