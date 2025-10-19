package com.chibao.edu.readers;

import com.chibao.edu.dtos.PatientImportDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.batch.item.ItemReader;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Iterator;

@Slf4j
public class ExcelPatientReader implements ItemReader<PatientImportDTO> {

    private final Iterator<Row> rowIterator;

    public ExcelPatientReader(InputStream inputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        this.rowIterator = sheet.iterator();

        // Skip header row
        if (rowIterator.hasNext()) {
            rowIterator.next();
        }
    }

    @Override
    public PatientImportDTO read() throws Exception {
        if (!rowIterator.hasNext()) {
            return null;
        }

        Row row = (Row) rowIterator.next();
        int currentRowNum = row.getRowNum() + 1;

        try {
            return PatientImportDTO.builder()
                    .rowNumber(currentRowNum)
                    .patientId(getCellValueAsString(row, 0))
                    .firstName(getCellValueAsString(row, 1))
                    .lastName(getCellValueAsString(row, 2))
                    .dateOfBirth(getCellValueAsDate(row, 3))
                    .gender(getCellValueAsString(row, 4))
                    .email(getCellValueAsString(row, 5))
                    .phoneNumber(getCellValueAsString(row, 6))
                    .address(getCellValueAsString(row, 7))
                    .city(getCellValueAsString(row, 8))
                    .state(getCellValueAsString(row, 9))
                    .zipCode(getCellValueAsString(row, 10))
                    .bloodType(getCellValueAsString(row, 11))
                    .medicalHistory(getCellValueAsString(row, 12))
                    .build();
        } catch (Exception e) {
            log.error("Error parsing row {}: {}", currentRowNum, e.getMessage());
            PatientImportDTO errorDto = new PatientImportDTO();
            errorDto.setRowNumber(currentRowNum);
            errorDto.setValid(false);
            errorDto.setValidationErrors("Parse error: " + e.getMessage());
            return errorDto;
        }
    }

    private String getCellValueAsString(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return null;
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }

    private LocalDate getCellValueAsDate(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return null;
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                Date date = cell.getDateCellValue();
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else if (cell.getCellType() == CellType.STRING) {
                String dateStr = cell.getStringCellValue();
                return LocalDate.parse(dateStr);
            }
        } catch (Exception e) {
            log.warn("Failed to parse date in row {}, cell {}", row.getRowNum(), cellIndex);
        }
        return null;
    }
}