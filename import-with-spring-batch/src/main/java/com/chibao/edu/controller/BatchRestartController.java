package com.chibao.edu.controller;

import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients/import-batch/restart")
public class BatchRestartController {

    private final JobOperator jobOperator;

    public BatchRestartController(JobOperator jobOperator) {
        this.jobOperator = jobOperator;
    }

    @PostMapping("/{executionId}")
    public String restartJob(@PathVariable Long executionId)
            throws JobRestartException, JobParametersInvalidException, NoSuchJobExecutionException, JobInstanceAlreadyCompleteException, NoSuchJobException {
        long newId = jobOperator.restart(executionId);
        return "Restarted job with new executionId=" + newId;
    }
}
