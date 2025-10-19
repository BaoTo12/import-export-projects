package com.chibao.edu.service.impl;

import com.chibao.edu.common.DuplicateStrategy;
import com.chibao.edu.common.ImportJobStatus;
import com.chibao.edu.dto.ImportJobRequest;
import com.chibao.edu.dto.ImportJobResponse;
import com.chibao.edu.dto.ImportJobStatusResponse;
import com.chibao.edu.dto.PatientImportDTO;
import com.chibao.edu.entity.ImportJob;
import com.chibao.edu.parser.FileParser;
import com.chibao.edu.parser.FileParserFactory;
import com.chibao.edu.repository.ImportJobRepository;
import com.chibao.edu.utils.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.batch.core.Job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final JobLauncher jobLauncher;
    private final Job patientImportJob;
    private final JobExplorer jobExplorer;
    private final FileValidator fileValidator;
    private final FileParserFactory fileParserFactory;
    private final ImportJobRepository importJobRepository;

    @Value("${import.upload-dir:./uploads}")
    private String uploadDir;

    @Async("batchTaskExecutor")
    public void processImportAsync(String jobId, String filePath, DuplicateStrategy duplicateStrategy) {
        try {
            log.info("Starting async import processing for job: {}", jobId);

            // Parse file
            FileParser<PatientImportDTO> parser = fileParserFactory.getParser(getFileExtension(filePath));
            List<PatientImportDTO> parsedData = parser.parse(Files.newInputStream(Paths.get(filePath)), PatientImportDTO.class);

            // Update total rows
            ImportJob importJob = importJobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Import job not found"));
            importJob.setTotalRows(parsedData.size());
            importJobRepository.save(importJob);

            // Prepare job parameters
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("jobId", jobId)
                    .addString("filePath", filePath)
                    .addString("duplicateStrategy", duplicateStrategy.name())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            // Create execution context with parsed data
            JobExecution jobExecution = jobLauncher.run(patientImportJob, jobParameters);
            jobExecution.getExecutionContext().put("parsedData", parsedData);

            // Update import job with batch execution ID
            importJob.setBatchJobExecutionId(jobExecution.getId());
            importJobRepository.save(importJob);

            log.info("Import job {} completed with status: {}", jobId, jobExecution.getStatus());

        } catch (Exception e) {
            log.error("Error processing import job: {}", jobId, e);
            handleImportFailure(jobId, e);
        }
    }

    public ImportJobResponse initiateImport(MultipartFile file, ImportJobRequest request) {
        try {
            // Validate file
            fileValidator.validateFile(file);

            // Save file
            String filePath = saveUploadedFile(file);

            // Create import job record
            ImportJob importJob = ImportJob.builder()
                    .id(UUID.randomUUID().toString())
                    .fileName(file.getOriginalFilename())
                    .filePath(filePath)
                    .status(ImportJobStatus.PENDING)
                    .duplicateStrategy(request.duplicateStrategy())
                    .totalRows(0)
                    .startTime(LocalDateTime.now())
                    .build();

            importJob = importJobRepository.save(importJob);

            // Start async processing
            processImportAsync(importJob.getId(), filePath, request.duplicateStrategy());

            return ImportJobResponse.builder()
                    .jobId(importJob.getId())
                    .status(importJob.getStatus())
                    .createdAt(importJob.getCreatedAt())
                    .fileName(importJob.getFileName())
                    .totalRows(importJob.getTotalRows())
                    .build();

        } catch (Exception e) {
            log.error("Error initiating import", e);
            throw new RuntimeException("Failed to initiate import: " + e.getMessage(), e);
        }
    }

    public ImportJobStatusResponse getJobStatus(String jobId) {
        ImportJob importJob = importJobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Import job not found: " + jobId));

        Double progress = calculateProgress(importJob);

        return ImportJobStatusResponse.builder()
                .jobId(importJob.getId())
                .status(importJob.getStatus())
                .startTime(importJob.getStartTime())
                .endTime(importJob.getEndTime())
                .totalRows(importJob.getTotalRows())
                .successCount(importJob.getSuccessCount())
                .failedCount(importJob.getFailedCount())
                .skippedCount(importJob.getSkippedCount())
                .progress(progress)
                .build();
    }

    private String saveUploadedFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath.toString();
    }

    private void handleImportFailure(String jobId, Exception e) {
        try {
            ImportJob importJob = importJobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Import job not found"));

            importJob.setStatus(ImportJobStatus.FAILED);
            importJob.setErrorMessage(e.getMessage());
            importJob.setEndTime(LocalDateTime.now());

            importJobRepository.save(importJob);
        } catch (Exception ex) {
            log.error("Error handling import failure", ex);
        }
    }

    private Double calculateProgress(ImportJob importJob) {
        if (importJob.getTotalRows() == null || importJob.getTotalRows() == 0) {
            return 0.0;
        }

        int processed = importJob.getSuccessCount() + importJob.getFailedCount() + importJob.getSkippedCount();
        return (double) processed / importJob.getTotalRows() * 100;
    }

    private String getFileExtension(String filePath) {
        return filePath.substring(filePath.lastIndexOf(".") + 1);
    }
}
