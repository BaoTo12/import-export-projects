package com.chibao.edu.controller;

import com.chibao.edu.common.ImportOption;
import com.chibao.edu.service.BatchJobLauncherService;
import org.springframework.batch.core.JobExecution;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;

@RestController
@RequestMapping("/api/v1/patients/import-batch")
public class BatchImportController {

    private final BatchJobLauncherService batchService;

    public BatchImportController(BatchJobLauncherService batchService) {
        this.batchService = batchService;
    }

    @PostMapping
    public String startBatchImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam("option") ImportOption option) throws Exception {

        // Save temporarily
        File temp = Files.createTempFile("import-", "-" + file.getOriginalFilename()).toFile();
        file.transferTo(temp);

        JobExecution exec = batchService.launch(temp.getAbsolutePath(), option.name());
        return "Job started with executionId=" + exec.getId();
    }
}
