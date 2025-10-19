package com.chibao.edu.readers;

import com.chibao.edu.dto.PatientImportDTO;
import com.chibao.edu.utils.ValidationResult;
import com.chibao.edu.utils.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class PatientItemReader implements ItemReader<PatientImportDTO> {

    private final ValidationService validationService;

    @Value("#{jobParameters['filePath']}")
    private String filePath;

    @Value("#{jobExecutionContext['parsedData']}")
    private List<PatientImportDTO> parsedData;

    private int currentIndex = 0;
    private List<PatientImportDTO> validatedData;

    @Override
    public PatientImportDTO read() {
        if (validatedData == null) {
            initializeValidatedData();
        }

        if (currentIndex < validatedData.size()) {
            return validatedData.get(currentIndex++);
        }

        return null; // Signals end of data
    }

    private void initializeValidatedData() {
        if (parsedData == null || parsedData.isEmpty()) {
            log.warn("No parsed data found in job execution context");
            validatedData = new ArrayList<>();
            return;
        }

        ValidationResult<PatientImportDTO> result = validationService.validateBatch(parsedData);
        validatedData = result.getValidData() != null ? result.getValidData() : new ArrayList<>();

        log.info("Validated {} records out of {} parsed records",
                validatedData.size(), parsedData.size());
    }
}
