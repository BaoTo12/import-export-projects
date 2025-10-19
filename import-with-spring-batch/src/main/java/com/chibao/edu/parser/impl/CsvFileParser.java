package com.chibao.edu.parser.impl;

import com.chibao.edu.parser.FileParser;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class CsvFileParser<T> implements FileParser<T> {

    @Override
    public List<T> parse(InputStream inputStream, Class<T> type) {
        try {
            return new CsvToBeanBuilder<T>(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .withType(type)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withThrowExceptions(false)
                    .build()
                    .parse();
        } catch (Exception e) {
            log.error("Error parsing CSV file", e);
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(String fileExtension) {
        return "csv".equalsIgnoreCase(fileExtension);
    }
}
