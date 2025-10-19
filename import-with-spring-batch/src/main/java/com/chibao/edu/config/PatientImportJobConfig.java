package com.chibao.edu.config;

import com.chibao.edu.dto.PatientImportDTO;
import com.chibao.edu.entity.Patient;
import com.chibao.edu.lisnter.ImportJobExecutionListener;
import com.chibao.edu.lisnter.ImportStepExecutionListener;
import com.chibao.edu.processors.PatientImportProcessor;
import com.chibao.edu.readers.PatientItemReader;
import com.chibao.edu.writers.PatientItemWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.core.Job;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class PatientImportJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PatientItemReader patientItemReader;
    private final PatientImportProcessor patientImportProcessor;
    private final PatientItemWriter patientItemWriter;
    private final ImportJobExecutionListener importJobExecutionListener;
    private final ImportStepExecutionListener importStepExecutionListener;

    @Value("${import.batch.chunk-size}")
    private Integer chunkSize;

    @Bean
    public Job patientImportJob() {
        return new JobBuilder("patientImportJob", jobRepository)
                .listener(importJobExecutionListener)
                .start(patientImportStep())
                .build();
    }

    @Bean
    public Step patientImportStep() {
        return new StepBuilder("patientImportStep", jobRepository)
                .<PatientImportDTO, Patient>chunk(chunkSize, transactionManager)
                .reader(patientItemReader)
                .processor(patientImportProcessor)
                .writer(patientItemWriter)
                .listener(importStepExecutionListener)
                .faultTolerant()
                .skipLimit(Integer.MAX_VALUE)
                .skip(Exception.class)
                .build();
    }
}
