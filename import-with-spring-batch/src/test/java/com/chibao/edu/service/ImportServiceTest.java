package com.chibao.edu.service;

import com.chibao.edu.common.DuplicateStrategy;
import com.chibao.edu.common.ImportJobStatus;
import com.chibao.edu.dto.ImportJobRequest;
import com.chibao.edu.dto.ImportJobResponse;
import com.chibao.edu.dto.ImportJobStatusResponse;
import com.chibao.edu.entity.ImportJob;
import com.chibao.edu.parser.FileParserFactory;
import com.chibao.edu.repository.ImportJobRepository;
import com.chibao.edu.service.impl.ImportService;
import com.chibao.edu.utils.FileValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job patientImportJob;

    @Mock
    private JobExplorer jobExplorer;

    @Mock
    private FileValidator fileValidator;

    @Mock
    private FileParserFactory fileParserFactory;

    @Mock
    private ImportJobRepository importJobRepository;

    @InjectMocks
    private ImportService importService;

    private MultipartFile validFile;
    private ImportJobRequest request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(importService, "uploadDir", "./test-uploads");

        validFile = new MockMultipartFile(
                "file",
                "patients.csv",
                "text/csv",
                "firstName,lastName,dateOfBirth,email,phone\nJohn,Doe,1990-01-01,john@example.com,+1234567890".getBytes()
        );

        request = new ImportJobRequest(DuplicateStrategy.SKIP);
    }

    @Test
    void initiateImport_shouldCreateJobAndReturnResponse() {
        // Arrange
        ImportJob savedJob = ImportJob.builder()
                .id("test-job-id")
                .fileName("patients.csv")
                .status(ImportJobStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(Optional.of(importJobRepository.save(any(ImportJob.class)))).thenReturn(Optional.ofNullable(savedJob));
        doNothing().when(fileValidator).validateFile(any());

        // Act
        ImportJobResponse response = importService.initiateImport(validFile, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getJobId()).isEqualTo("test-job-id");
        assertThat(response.getStatus()).isEqualTo(ImportJobStatus.PENDING);
        verify(importJobRepository).save(any(ImportJob.class));
    }

    @Test
    void getJobStatus_shouldReturnStatus() {
        // Arrange
        String jobId = "test-job-id";
        ImportJob importJob = ImportJob.builder()
                .id(jobId)
                .status(ImportJobStatus.COMPLETED)
                .totalRows(100)
                .successCount(95)
                .failedCount(5)
                .skippedCount(0)
                .startTime(LocalDateTime.now().minusMinutes(5))
                .endTime(LocalDateTime.now())
                .build();

        when(importJobRepository.findById(jobId)).thenReturn(Optional.of(importJob));

        // Act
        ImportJobStatusResponse response = importService.getJobStatus(jobId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getJobId()).isEqualTo(jobId);
        assertThat(response.getStatus()).isEqualTo(ImportJobStatus.COMPLETED);
        assertThat(response.getSuccessCount()).isEqualTo(95);
        assertThat(response.getProgress()).isEqualTo(100.0);
    }


    @Test
    void getJobStatus_withInvalidJobId_shouldThrowException() {
        // Arrange
        String jobId = "invalid-id";
        when(importJobRepository.findById(jobId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> importService.getJobStatus(jobId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Import job not found");
    }
}
