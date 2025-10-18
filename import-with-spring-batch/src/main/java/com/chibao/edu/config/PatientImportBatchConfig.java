package com.chibao.edu.config;

import com.chibao.edu.dto.PatientDto;
import com.chibao.edu.entity.Patient;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class PatientImportBatchConfig {

    public Step patientImportStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  ItemReader<PatientDto> reader,
                                  ItemProcessor<PatientDto, Patient> processor,
                                  ItemWriter<Patient> writer) {
        return new StepBuilder("patientImportStep", jobRepository)
                .<PatientDto, Patient>chunk(100, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipPolicy(new AlwaysSkipItemSkipPolicy())  // Or custom skip logic
                .build();
    }

    public Job patientImportJob(JobRepository jobRepository, Step patientImportStep) {
        return new JobBuilder("patientImportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .flow(patientImportStep)
                .end()
                .build();
    }
}