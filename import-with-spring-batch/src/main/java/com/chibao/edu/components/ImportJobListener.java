package com.chibao.edu.components;

import com.chibao.edu.common.Status;
import com.chibao.edu.entity.ImportJobStatus;
import com.chibao.edu.repository.ImportJobStatusRepository;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class ImportJobListener implements JobExecutionListener {

    private final ImportJobStatusRepository jobRepo;

    public ImportJobListener(ImportJobStatusRepository jobRepo) {
        this.jobRepo = jobRepo;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        UUID jobId = UUID.randomUUID();
        jobExecution.getExecutionContext().put("jobId", jobId.toString());

        ImportJobStatus job = new ImportJobStatus();
        job.setId(jobId);
        job.setCreatedAt(Instant.now());
        job.setStatus(Status.RUNNING);
        jobRepo.save(job);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        UUID jobId = UUID.fromString(jobExecution.getExecutionContext().getString("jobId"));
        ImportJobStatus job = jobRepo.findById(jobId).orElse(null);
        if (job == null) return;

        switch (jobExecution.getStatus()) {
            case COMPLETED:
                job.setStatus(Status.SUCCESS);
                break;
            case FAILED:
                job.setStatus(Status.FAILED);
                job.setMessage(jobExecution.getAllFailureExceptions().toString());
                break;
            default:
                job.setStatus(Status.FAILED);
                break;
        }
        jobRepo.save(job);
    }
}