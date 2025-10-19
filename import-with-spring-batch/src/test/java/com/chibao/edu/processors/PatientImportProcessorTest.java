package com.chibao.edu.processors;

import com.chibao.edu.common.DuplicateStrategy;
import com.chibao.edu.dto.PatientImportDTO;
import com.chibao.edu.entity.Patient;
import com.chibao.edu.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientImportProcessorTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientImportProcessor processor;

    private PatientImportDTO sampleDto;
    private Patient existingPatient;

    @BeforeEach
    void setUp() {
        sampleDto = PatientImportDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("john.doe@example.com")
                .phone("+1234567890")
                .address("123 Main St")
                .bloodType("O+")
                .build();

        existingPatient = Patient.builder()
                .id(1L)
                .firstName("Jane")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build();
    }

    @Test
    void processNewPatient_shouldCreateNewEntity() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(processor, "duplicateStrategyStr", DuplicateStrategy.SKIP.name());
        when(patientRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act
        Patient result = processor.process(sampleDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        verify(patientRepository).findByEmail("john.doe@example.com");
    }

    @Test
    void processWithSkipStrategy_shouldReturnNull() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(processor, "duplicateStrategyStr", DuplicateStrategy.SKIP.name());
        when(patientRepository.findByEmail(anyString())).thenReturn(Optional.of(existingPatient));

        // Act
        Patient result = processor.process(sampleDto);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void processWithUpdateStrategy_shouldUpdateExisting() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(processor, "duplicateStrategyStr", DuplicateStrategy.UPDATE.name());
        when(patientRepository.findByEmail(anyString())).thenReturn(Optional.of(existingPatient));

        // Act
        Patient result = processor.process(sampleDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void processWithFailStrategy_shouldThrowException() {
        // Arrange
        ReflectionTestUtils.setField(processor, "duplicateStrategyStr", DuplicateStrategy.FAIL.name());
        when(patientRepository.findByEmail(anyString())).thenReturn(Optional.of(existingPatient));

        // Act & Assert
        assertThatThrownBy(() -> processor.process(sampleDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Duplicate email found");
    }
}

