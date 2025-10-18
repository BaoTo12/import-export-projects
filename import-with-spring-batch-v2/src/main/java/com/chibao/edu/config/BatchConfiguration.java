package com.chibao.edu.config;

import com.chibao.edu.processors.PatientValidationProcessor;
import com.chibao.edu.writers.PatientWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class BatchConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PatientValidationProcessor processor;
    private final PatientWriter writer;

    @Bean
    public Job patientImportJob(Step patientImportStep) {
        return new JobBuilder("patientImportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(patientImportStep)
                .build();
    }

    public Step createPatientImportStep(ItemReader reader) {
        return new StepBuilder("patientImportStep", jobRepository)
                .chunk(50, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipLimit(100)
                .skip(Exception.class)
                .build();
    }
}
