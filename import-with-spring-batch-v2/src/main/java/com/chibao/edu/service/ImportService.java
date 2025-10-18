package com.chibao.edu.service;

import com.chibao.edu.common.DuplicateHandlingStrategy;
import com.chibao.edu.common.ImportStatus;
import com.chibao.edu.config.BatchConfiguration;
import com.chibao.edu.dtos.ImportResponse;
import com.chibao.edu.dtos.ImportStatusResponse;
import com.chibao.edu.dtos.PatientImportDTO;
import com.chibao.edu.models.ImportJob;
import com.chibao.edu.processors.PatientValidationProcessor;
import com.chibao.edu.readers.CsvPatientReader;
import com.chibao.edu.readers.ExcelPatientReader;
import com.chibao.edu.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemReader;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final ImportJobRepository importJobRepository;
    private final JobLauncher jobLauncher;
    private final Job patientImportJob;
    private final BatchConfiguration batchConfiguration;
    private final PatientValidationProcessor processor;
    private final JobRepository jobRepository;

    private static final int MAX_ROWS = 1000;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("csv", "xlsx");
    private static final List<String> REQUIRED_HEADERS = List.of(
            "patientId", "firstName", "lastName", "dateOfBirth", "gender"
    );

    public ImportResponse validateAndPreview(MultipartFile file, DuplicateHandlingStrategy strategy)
            throws Exception {

        // Validate file
        validateFile(file);

        String jobId = UUID.randomUUID().toString();
        String fileName = file.getOriginalFilename();

        // Save import job
        ImportJob importJob = ImportJob.builder()
                .jobId(jobId)
                .fileName(fileName)
                .status(ImportStatus.VALIDATING)
                .duplicateStrategy(strategy)
                .build();
        importJobRepository.save(importJob);

        // Validate headers and preview
        List<ImportResponse.ValidationError> errors = new ArrayList<>();
        int rowCount = 0;

        try (InputStream is = file.getInputStream()) {
            assert fileName != null;
            ItemReader<PatientImportDTO> reader = createReader(is, fileName);

            PatientImportDTO dto;
            while ((dto = (PatientImportDTO) reader.read()) != null && rowCount < MAX_ROWS) {
                rowCount++;
                if (!dto.isValid() && dto.getValidationErrors() != null) {
                    errors.add(ImportResponse.ValidationError.builder()
                            .rowNumber(dto.getRowNumber())
                            .error(dto.getValidationErrors())
                            .build());
                }
            }
        }

        importJob.setTotalRecords(rowCount);
        importJob.setStatus(ImportStatus.PENDING);
        importJobRepository.save(importJob);

        return ImportResponse.builder()
                .jobId(jobId)
                .fileName(fileName)
                .totalRecords(rowCount)
                .validationErrors(errors)
                .message("File validated. Ready to import.")
                .build();
    }

    @Async
    public void startImport(String jobId, MultipartFile file, DuplicateHandlingStrategy strategy) throws Throwable {
        ImportJob importJob = (ImportJob) importJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new RuntimeException("Import job not found"));

        try {
            importJob.setStatus(ImportStatus.IN_PROGRESS);
            importJob.setStartedAt(LocalDateTime.now());
            importJobRepository.save(importJob);

            // Set duplicate strategy
            processor.setDuplicateStrategy(strategy);

            // Create reader
            InputStream inputStream = file.getInputStream();
            ItemReader<PatientImportDTO> reader = createReader(inputStream, Objects.requireNonNull(file.getOriginalFilename()));

            // Create step with reader
            Step step = batchConfiguration.createPatientImportStep(reader);

            // Create and run job
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("jobId", jobId)
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            // Rebuild job with the step
            org.springframework.batch.core.Job job = new org.springframework.batch.core.job.builder.JobBuilder("patientImportJob-" + jobId, jobRepository)
                    .start(step)
                    .build();

            JobExecution execution = jobLauncher.run(job, jobParameters);

            // Update job status
            updateImportJobStatus(importJob, execution);

        } catch (Exception e) {
            log.error("Import failed for job {}", jobId, e);
            importJob.setStatus(ImportStatus.FAILED);
            importJob.setErrorMessage(e.getMessage());
            importJob.setCompletedAt(LocalDateTime.now());
            importJobRepository.save(importJob);
        }
    }

    public ImportStatusResponse getImportStatus(String jobId) throws Throwable {
        ImportJob job = (ImportJob) importJobRepository.findByJobId(jobId)
                .orElseThrow(() -> new RuntimeException("Import job not found"));

        int progress = 0;
        if (job.getTotalRecords() != null && job.getTotalRecords() > 0) {
            progress = (int) ((job.getProcessedRecords() != null ? job.getProcessedRecords() : 0)
                    * 100.0 / job.getTotalRecords());
        }

        return ImportStatusResponse.builder()
                .jobId(job.getJobId())
                .fileName(job.getFileName())
                .status(job.getStatus())
                .totalRecords(job.getTotalRecords())
                .processedRecords(job.getProcessedRecords())
                .successCount(job.getSuccessCount())
                .failedCount(job.getFailedCount())
                .skippedCount(job.getSkippedCount())
                .progressPercentage(progress)
                .errorMessage(job.getErrorMessage())
                .hasErrorReport(job.getErrorReportPath() != null)
                .build();
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IOException("File name is null");
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IOException("Invalid file format. Only CSV and XLSX are supported.");
        }

        if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
            throw new IOException("File size exceeds 10MB limit");
        }
    }

    private ItemReader<PatientImportDTO> createReader(InputStream inputStream, String fileName)
            throws IOException {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        if ("csv".equals(extension)) {
            return new CsvPatientReader(inputStream);
        } else if ("xlsx".equals(extension)) {
            return new ExcelPatientReader(inputStream);
        } else {
            throw new IOException("Unsupported file format: " + extension);
        }
    }

    private void updateImportJobStatus(ImportJob importJob, JobExecution execution) {
        StepExecution stepExecution = execution.getStepExecutions().iterator().next();

        importJob.setProcessedRecords((int) stepExecution.getReadCount());
        importJob.setSuccessCount((int) stepExecution.getWriteCount());
        importJob.setFailedCount((int) stepExecution.getReadSkipCount() +
                (int) stepExecution.getProcessSkipCount() +
                (int) stepExecution.getWriteSkipCount());
        importJob.setSkippedCount((int) stepExecution.getFilterCount());

        if (execution.getStatus() == BatchStatus.COMPLETED) {
            importJob.setStatus(ImportStatus.COMPLETED);
        } else if (execution.getStatus() == BatchStatus.FAILED) {
            importJob.setStatus(ImportStatus.FAILED);
            importJob.setErrorMessage(execution.getAllFailureExceptions().toString());
        }

        importJob.setCompletedAt(LocalDateTime.now());
        importJobRepository.save(importJob);
    }
}
