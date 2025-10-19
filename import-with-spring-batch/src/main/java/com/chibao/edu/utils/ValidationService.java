package com.chibao.edu.utils;

import com.chibao.edu.dto.PatientImportDTO;
import com.chibao.edu.entity.ImportError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {

    private final Validator validator;

    public <T> ValidationResult<T> validate(T dto, Integer rowNumber) {
        Set<ConstraintViolation<T>> violations = validator.validate(dto);

        if (violations.isEmpty()) {
            return ValidationResult.<T>builder()
                    .valid(true)
                    .data(dto)
                    .build();
        }

        List<ImportError> errors = violations.stream()
                .map(violation -> ImportError.builder()
                        .rowNumber(rowNumber)
                        .field(violation.getPropertyPath().toString())
                        .message(violation.getMessage())
                        .value(violation.getInvalidValue() != null ?
                                violation.getInvalidValue().toString() : null)
                        .build())
                .toList();

        return ValidationResult.<T>builder()
                .valid(false)
                .errors(errors)
                .build();
    }

    public ValidationResult<PatientImportDTO> validateBatch(List<PatientImportDTO> dtos) {
        List<ImportError> allErrors = new ArrayList<>();
        List<PatientImportDTO> validDtos = new ArrayList<>();

        for (int i = 0; i < dtos.size(); i++) {
            PatientImportDTO dto = dtos.get(i);
            dto.setRowNumber(i + 1);

            ValidationResult<PatientImportDTO> result = validate(dto, i + 1);

            if (result.isValid()) {
                validDtos.add(dto);
            } else {
                allErrors.addAll(result.getErrors());
            }
        }

        return ValidationResult.<PatientImportDTO>builder()
                .valid(allErrors.isEmpty())
                .validData(validDtos)
                .errors(allErrors)
                .build();
    }
}

