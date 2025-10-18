package com.chibao.edu.config;

import com.chibao.edu.dto.PatientDto;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

@Configuration
public class CsvItemReaderConfig {


    @Bean
    @StepScope
    public FlatFileItemReader<PatientDto> csvReader(@Value("#{jobParameters['filePath']}") String filePath) {
        FlatFileItemReader<PatientDto> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(filePath));
        reader.setLinesToSkip(1);


        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames(new String[]{"firstName","lastName","email","phone","nationalId","dob"});


        DefaultLineMapper<PatientDto> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);


        BeanWrapperFieldSetMapper<PatientDto> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(PatientDto.class);
        lineMapper.setFieldSetMapper(fieldSetMapper);


        reader.setLineMapper(lineMapper);
        return reader;
    }
}