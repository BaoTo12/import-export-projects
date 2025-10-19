package com.chibao.edu.lisnter;

import com.chibao.edu.common.ImportJobStatus;
import com.chibao.edu.entity.ImportJob;
import com.chibao.edu.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImportJobExecutionListener implements JobExecutionListener {

    private final ImportJobRepository importJobRepository;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Starting import job with execution ID: {}", jobExecution.getId());

        ImportJob importJob = importJobRepository
                .findByBatchJobExecutionId(jobExecution.getId())
                .orElseThrow(() -> new RuntimeException("Import job not found"));

        importJob.setStatus(ImportJobStatus.PROCESSING);
        importJobRepository.save(importJob);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("Completed import job with execution ID: {} - Status: {}",
                jobExecution.getId(), jobExecution.getStatus());

        ImportJob importJob = importJobRepository
                .findByBatchJobExecutionId(jobExecution.getId())
                .orElseThrow(() -> new RuntimeException("Import job not found"));

        importJob.setEndTime(LocalDateTime.now());

        if (jobExecution.getStatus().isUnsuccessful()) {
            importJob.setStatus(ImportJobStatus.FAILED);
            importJob.setErrorMessage(
                    jobExecution.getAllFailureExceptions().stream()
                            .map(Throwable::getMessage)
                            .reduce((a, b) -> a + "; " + b)
                            .orElse("Unknown error")
            );
        } else {
            importJob.setStatus(ImportJobStatus.COMPLETED);
        }

        importJobRepository.save(importJob);
    }
}
