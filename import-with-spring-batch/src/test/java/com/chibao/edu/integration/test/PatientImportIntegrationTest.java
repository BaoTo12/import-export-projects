package com.chibao.edu.integration.test;

import com.chibao.edu.common.ImportJobStatus;
import com.chibao.edu.entity.ImportJob;
import com.chibao.edu.repository.ImportJobRepository;
import com.chibao.edu.repository.PatientRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PatientImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ImportJobRepository importJobRepository;

    @BeforeEach
    void setUp() {
        patientRepository.deleteAll();
        importJobRepository.deleteAll();
    }

    @Test
    void importPatients_withValidCsv_shouldCreateImportJob() throws Exception {
        // Arrange
        String csvContent = """
            firstName,lastName,dateOfBirth,email,phone,address,bloodType
            John,Doe,1990-01-01,john.doe@example.com,+1234567890,123 Main St,O+
            Jane,Smith,1985-05-15,jane.smith@example.com,+0987654321,456 Oak Ave,A+
            """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "patients.csv",
                "text/csv",
                csvContent.getBytes()
        );

        // Act
        MvcResult result = mockMvc.perform(multipart("/api/v1/patients/import")
                        .file(file)
                        .param("duplicateStrategy", "SKIP"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        // Assert
        String response = result.getResponse().getContentAsString();
        assertThat(response).contains("jobId");
    }

    @Test
    void importPatients_withInvalidFile_shouldReturnBadRequest() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "invalid content".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/patients/import")
                        .file(file)
                        .param("duplicateStrategy", "SKIP"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getImportStatus_withValidJobId_shouldReturnStatus() throws Exception {
        // Arrange
        ImportJob job = ImportJob.builder()
                .id("test-job-123")
                .fileName("test.csv")
                .filePath("/tmp/test.csv")
                .status(ImportJobStatus.COMPLETED)
                .totalRows(10)
                .successCount(8)
                .failedCount(2)
                .skippedCount(0)
                .build();

        importJobRepository.save(job);

        // Act & Assert
        mockMvc.perform(get("/api/v1/patients/import/{jobId}/status", "test-job-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("test-job-123"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.successCount").value(8))
                .andExpect(jsonPath("$.failedCount").value(2));
    }

    @Test
    void getImportStatus_withInvalidJobId_shouldReturnError() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/patients/import/{jobId}/status", "invalid-id"))
                .andExpect(status().is5xxServerError());
    }
}
