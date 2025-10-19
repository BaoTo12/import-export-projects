package com.chibao.edu.utils;


import com.chibao.edu.dto.PatientImportDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationServiceTest {

    private ValidationService validationService;
    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        validationService = new ValidationService(validator);
    }

    @Test
    void validateValidDto_shouldPass() {
        // Arrange
        PatientImportDTO dto = PatientImportDTO.builder().firstName("John").lastName("Doe").dateOfBirth(LocalDate.of(1990, 1, 1)).email("john.doe@example.com").phone("+1234567890").bloodType("O+").build();

        // Act
        ValidationResult<PatientImportDTO> result = validationService.validate(dto, 1);

        // Assert
        assertThat(result.isValid()).isTrue();
        assertThat(result.getData()).isEqualTo(dto);
    }

    @Test
    void validateInvalidEmail_shouldFail() {
        // Arrange
        PatientImportDTO dto = PatientImportDTO.builder().firstName("John").lastName("Doe").dateOfBirth(LocalDate.of(1990, 1, 1)).email("invalid-email").phone("+1234567890").build();

        // Act
        ValidationResult<PatientImportDTO> result = validationService.validate(dto, 1);

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().get(0).getField()).isEqualTo("email");
    }

    @Test
    void validateMissingRequiredField_shouldFail() {
        // Arrange
        PatientImportDTO dto = PatientImportDTO.builder().firstName("John").dateOfBirth(LocalDate.of(1990, 1, 1)).email("john@example.com").phone("+1234567890").build();

        // Act
        ValidationResult<PatientImportDTO> result = validationService.validate(dto, 1);

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).hasSize(1);
    }

    @Test
    void validateFutureDateOfBirth_shouldFail() {
        // Arrange
        PatientImportDTO dto = PatientImportDTO.builder().firstName("John").lastName("Doe").dateOfBirth(LocalDate.now().plusYears(1)).email("john@example.com").phone("+1234567890").build();

        // Act
        ValidationResult<PatientImportDTO> result = validationService.validate(dto, 1);

        // Assert
        assertThat(result.isValid()).isFalse();
    }

}