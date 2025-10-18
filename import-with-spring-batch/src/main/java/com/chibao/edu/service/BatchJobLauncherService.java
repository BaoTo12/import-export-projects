package com.chibao.edu.service;

import com.chibao.edu.common.ImportOption;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

@Service
public class BatchJobLauncherService {

    private final JobLauncher jobLauncher;
    private final Job patientImportJob;

    public BatchJobLauncherService(JobLauncher jobLauncher, Job patientImportJob) {
        this.jobLauncher = jobLauncher;
        this.patientImportJob = patientImportJob;
    }

    public JobExecution launch(String filePath, String option) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("filePath", filePath)
                .addString("option", option)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        return jobLauncher.run(patientImportJob, jobParameters);
    }
}
