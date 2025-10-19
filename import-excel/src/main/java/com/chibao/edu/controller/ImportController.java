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
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
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


        String original = Paths.get(Objects.requireNonNull(file.getOriginalFilename())).getFileName().toString();
        UUID jobId = UUID.randomUUID();
        // ? ➡️ Tạo đường dẫn tới thư mục tạm của hệ thống,
        // ? trong đó có thư mục con excel-import và tiếp theo là thư mục theo jobId.
        Path dir = Paths.get(System.getProperty("java.io.tmpdir"), "excel-import", jobId.toString());
        Files.createDirectories(dir);
        // ? ➡️ Xác định đường dẫn file đích và lưu file upload vào đó.
        // ? nối dir/original
        Path target = dir.resolve(original);
        // ? ố gắng di chuyển hoặc ghi nội dung upload vào file đích
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

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