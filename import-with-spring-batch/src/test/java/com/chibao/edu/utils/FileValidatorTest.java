package com.chibao.edu.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileValidatorTest {

    private FileValidator fileValidator;

    @BeforeEach
    void setUp() {
        fileValidator = new FileValidator();
        ReflectionTestUtils.setField(fileValidator, "maxRows", 1000);
        ReflectionTestUtils.setField(fileValidator, "allowedExtensions", List.of("csv", "xlsx"));
    }

    @Test
    void validateValidCsvFile_shouldPass() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                "firstName,lastName\nJohn,Doe".getBytes()
        );

        // Act & Assert
        assertThatCode(() -> fileValidator.validateFile(file))
                .doesNotThrowAnyException();
    }

    @Test
    void validateEmptyFile_shouldThrowException() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                new byte[0]
        );

        // Act & Assert
        assertThatThrownBy(() -> fileValidator.validateFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File is empty");
    }

    @Test
    void validateUnsupportedExtension_shouldThrowException() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "some content".getBytes()
        );

        // Act & Assert
        assertThatThrownBy(() -> fileValidator.validateFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not supported");
    }

    @Test
    void validateFileTooLarge_shouldThrowException() {
        // Arrange
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                largeContent
        );

        // Act & Assert
        assertThatThrownBy(() -> fileValidator.validateFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds 10MB");
    }
}
