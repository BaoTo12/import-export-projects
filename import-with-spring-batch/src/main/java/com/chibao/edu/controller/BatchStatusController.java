package com.chibao.edu.controller;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients/import-batch/status")
public class BatchStatusController {

    private final JobExplorer jobExplorer;

    public BatchStatusController(JobExplorer jobExplorer) {
        this.jobExplorer = jobExplorer;
    }

    @GetMapping("/{executionId}")
    public String getStatus(@PathVariable Long executionId) {
        JobExecution exec = jobExplorer.getJobExecution(executionId);
        if (exec == null) return "Job not found";
        return "Status: " + exec.getStatus() + ", StepCount: " + exec.getStepExecutions().size();
    }
}