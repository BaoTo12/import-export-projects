package com.chibao.edu.lisnter;

import com.chibao.edu.entity.ImportJob;
import com.chibao.edu.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImportStepExecutionListener implements StepExecutionListener {

    private final ImportJobRepository importJobRepository;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("Starting step: {}", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("Completed step: {} - Read: {}, Written: {}, Skipped: {}",
                stepExecution.getStepName(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount());

        Long jobExecutionId = stepExecution.getJobExecutionId();

        ImportJob importJob = importJobRepository
                .findByBatchJobExecutionId(jobExecutionId)
                .orElseThrow(() -> new RuntimeException("Import job not found"));

        importJob.setSuccessCount((int) stepExecution.getWriteCount());
        importJob.setSkippedCount((int) stepExecution.getSkipCount());
        importJob.setFailedCount((int) (stepExecution.getReadCount() - stepExecution.getWriteCount() - stepExecution.getSkipCount()));

        importJobRepository.save(importJob);

        return stepExecution.getExitStatus();
    }
}
