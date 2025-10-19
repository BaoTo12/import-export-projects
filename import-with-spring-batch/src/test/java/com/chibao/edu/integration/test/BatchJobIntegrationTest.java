package com.chibao.edu.integration.test;

import com.chibao.edu.common.DuplicateStrategy;
import com.chibao.edu.dto.PatientImportDTO;
import com.chibao.edu.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class BatchJobIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private PatientRepository patientRepository;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        patientRepository.deleteAll();
    }

    @Test
    void testPatientImportJob_shouldProcessSuccessfully() throws Exception {
        // Arrange
        List<PatientImportDTO> testData = List.of(
                createTestPatient("John", "Doe", "john@example.com"),
                createTestPatient("Jane", "Smith", "jane@example.com")
        );

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobId", "test-job")
                .addString("filePath", "/tmp/test.csv")
                .addString("duplicateStrategy", DuplicateStrategy.SKIP.name())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // Act
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        jobExecution.getExecutionContext().put("parsedData", testData);

        // Assert
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(patientRepository.count()).isGreaterThan(0);
    }

    private PatientImportDTO createTestPatient(String firstName, String lastName, String email) {
        return PatientImportDTO.builder()
                .firstName(firstName)
                .lastName(lastName)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email(email)
                .phone("+1234567890")
                .address("123 Main St")
                .bloodType("O+")
                .build();
    }
}