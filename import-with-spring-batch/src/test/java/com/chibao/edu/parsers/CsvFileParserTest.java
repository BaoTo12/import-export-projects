package com.chibao.edu.parsers;

import com.chibao.edu.dto.PatientImportDTO;
import com.chibao.edu.parser.impl.CsvFileParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvFileParserTest {

    private CsvFileParser<PatientImportDTO> csvFileParser;

    @BeforeEach
    void setUp() {
        csvFileParser = new CsvFileParser<>();
    }

    @Test
    void parse_shouldParseCsvCorrectly() {
        // Arrange
        String csvContent = """
            firstName,lastName,dateOfBirth,email,phone,address,bloodType
            John,Doe,1990-01-01,john.doe@example.com,+1234567890,123 Main St,O+
            Jane,Smith,1985-05-15,jane.smith@example.com,+0987654321,456 Oak Ave,A+
            """;

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        // Act
        List<PatientImportDTO> result = csvFileParser.parse(inputStream, PatientImportDTO.class);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFirstName()).isEqualTo("John");
        assertThat(result.get(0).getEmail()).isEqualTo("john.doe@example.com");
        assertThat(result.get(1).getFirstName()).isEqualTo("Jane");
    }

    @Test
    void supports_shouldReturnTrueForCsv() {
        // Act & Assert
        assertThat(csvFileParser.supports("csv")).isTrue();
        assertThat(csvFileParser.supports("CSV")).isTrue();
    }

    @Test
    void supports_shouldReturnFalseForNonCsv() {
        // Act & Assert
        assertThat(csvFileParser.supports("xlsx")).isFalse();
        assertThat(csvFileParser.supports("txt")).isFalse();
    }
}
