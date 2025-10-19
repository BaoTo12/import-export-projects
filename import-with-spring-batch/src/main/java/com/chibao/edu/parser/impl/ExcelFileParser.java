package com.chibao.edu.parser.impl;

import com.chibao.edu.dto.PatientImportDTO;
import com.chibao.edu.parser.FileParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ExcelFileParser<T> implements FileParser<T> {

    @Override
    @SuppressWarnings("unchecked")
    public List<T> parse(InputStream inputStream, Class<T> type) {
        if (!PatientImportDTO.class.equals(type)) {
            throw new UnsupportedOperationException("ExcelFileParser currently only supports PatientImportDTO");
        }

        List<T> results = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("Excel file must contain a header row");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                PatientImportDTO dto = PatientImportDTO.builder()
                        .firstName(getCellValueAsString(row.getCell(0)))
                        .lastName(getCellValueAsString(row.getCell(1)))
                        .dateOfBirth(getCellValueAsLocalDate(row.getCell(2)))
                        .email(getCellValueAsString(row.getCell(3)))
                        .phone(getCellValueAsString(row.getCell(4)))
                        .address(getCellValueAsString(row.getCell(5)))
                        .bloodType(getCellValueAsString(row.getCell(6)))
                        .rowNumber(i + 1)
                        .build();

                results.add((T) dto);
            }

        } catch (Exception e) {
            log.error("Error parsing Excel file", e);
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage(), e);
        }

        return results;
    }

    @Override
    public boolean supports(String fileExtension) {
        return "xlsx".equalsIgnoreCase(fileExtension) || "xls".equalsIgnoreCase(fileExtension);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }

    private LocalDate getCellValueAsLocalDate(Cell cell) {
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }

        return null;
    }
}
