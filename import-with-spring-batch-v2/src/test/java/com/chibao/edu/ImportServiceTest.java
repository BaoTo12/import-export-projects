package com.chibao.edu;

import com.chibao.edu.dtos.ImportResponse;
import com.chibao.edu.models.ImportJob;
import com.chibao.edu.repository.ImportJobRepository;
import com.chibao.edu.service.ImportService;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
//
//@ExtendWith(MockitoExtension.class)
//class ImportServiceTest {
//
//    @Mock
//    private ImportJobRepository importJobRepository;
//
//    @InjectMocks
//    private ImportService importService;
//
//    @Test
//    void testValidateAndPreview_ValidCsvFile() throws IOException {
//        // Arrange
//        String csvContent = """
//            patientId,firstName,lastName,dateOfBirth,gender,email,phoneNumber,address,city,state,zipCode,bloodType,medicalHistory
//            P001,John,Doe,1990-01-01,MALE,john@test.com,1234567890,123 Main St,NYC,NY,10001,A+,None
//            """;
//
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "patients.csv",
//                "text/csv",
//                csvContent.getBytes()
//        );
//
//        when(importJobRepository.save(any(ImportJob.class)))
//                .thenAnswer(i -> i.getArgument(0));
//
//        // Act
//        ImportResponse response = importService.validateAndPreview(
//                file,
//                ImportJob.DuplicateHandlingStrategy.SKIP
//        );
//
//        // Assert
//        assertThat(response).isNotNull();
//        assertThat(response.getJobId()).isNotNull();
//        assertThat(response.getFileName()).isEqualTo("patients.csv");
//        assertThat(response.getTotalRecords()).isGreaterThan(0);
//
//        verify(importJobRepository, times(2)).save(any(ImportJob.class));
//    }
//
//    @Test
//    void testValidateFile_InvalidExtension() {
//        // Arrange
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "patients.txt",
//                "text/plain",
//                "test".getBytes()
//        );
//
//        // Act & Assert
//        assertThatThrownBy(() ->
//                importService.validateAndPreview(file, ImportJob.DuplicateHandlingStrategy.SKIP))
//                .isInstanceOf(IOException.class)
//                .hasMessageContaining("Invalid file format");
//    }
//}
