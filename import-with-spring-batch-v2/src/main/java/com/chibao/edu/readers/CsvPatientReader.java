package com.chibao.edu.readers;

import com.chibao.edu.dtos.PatientImportDTO;
import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import com.opencsv.CSVReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
public class CsvPatientReader implements ItemReader<PatientImportDTO> {

    private final CSVReader  csvReader;
    private int currentRow = 1; // Start at 1, header is row 0
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public CsvPatientReader(InputStream inputStream) throws IOException {
        this.csvReader = new CSVReader(new InputStreamReader(inputStream));
        // Skip header row
        try {
            csvReader.readNext();
        } catch (CsvValidationException e) {
            throw new IOException("Failed to read CSV header", e);
        }
    }

    @Override
    public PatientImportDTO read() throws Exception {
        String[] line = csvReader.readNext();
        if (line == null) {
            return null;
        }

        currentRow++;

        try {
            return PatientImportDTO.builder()
                    .rowNumber(currentRow)
                    .patientId(getValueOrNull(line, 0))
                    .firstName(getValueOrNull(line, 1))
                    .lastName(getValueOrNull(line, 2))
                    .dateOfBirth(parseDate(getValueOrNull(line, 3)))
                    .gender(getValueOrNull(line, 4))
                    .email(getValueOrNull(line, 5))
                    .phoneNumber(getValueOrNull(line, 6))
                    .address(getValueOrNull(line, 7))
                    .city(getValueOrNull(line, 8))
                    .state(getValueOrNull(line, 9))
                    .zipCode(getValueOrNull(line, 10))
                    .bloodType(getValueOrNull(line, 11))
                    .medicalHistory(getValueOrNull(line, 12))
                    .build();
        } catch (Exception e) {
            log.error("Error parsing row {}: {}", currentRow, e.getMessage());
            PatientImportDTO errorDto = new PatientImportDTO();
            errorDto.setRowNumber(currentRow);
            errorDto.setValid(false);
            errorDto.setValidationErrors("Parse error: " + e.getMessage());
            return errorDto;
        }
    }

    private String getValueOrNull(String[] line, int index) {
        if (index >= line.length) {
            return null;
        }
        String value = line[index].trim();
        return value.isEmpty() ? null : value;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, dateFormatter);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse date: {}", dateStr);
            return null;
        }
    }
}
