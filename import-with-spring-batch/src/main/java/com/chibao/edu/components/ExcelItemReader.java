package com.chibao.edu.components;

import com.chibao.edu.dto.PatientDto;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Iterator;

@Component
public class ExcelItemReader extends ItemStreamSupport implements ItemStreamReader<PatientDto> {
    private Iterator<Row> rowIterator;
    private Sheet sheet;
    private DataFormatter formatter = new DataFormatter();
    private int currentRow = 1; // assume header in row 0
    private RowMapper rowMapper = new RowMapper();

    public ExcelItemReader() {}


    public void openFile(String path) throws IOException {
        try {
            Workbook wb = WorkbookFactory.create(new File(path));
            this.sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet != null) this.rowIterator = sheet.rowIterator();
            // advance past header
            if (rowIterator != null && rowIterator.hasNext()) rowIterator.next();
        } catch (InvalidFormatException e) {
            throw new IOException("Invalid Excel format", e);
        }
    }


    @Override
    public PatientDto read() throws Exception {
        if (rowIterator == null) return null;
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if (isRowBlank(row)) continue;
            return rowMapper.map(row, formatter);
        }
        return null;
    }

    private boolean isRowBlank(Row r) {
        for (Cell c : r) {
            String txt = formatter.formatCellValue(c).trim();
            if (!txt.isEmpty()) return false;
        }
        return true;
    }






    private static class RowMapper {
        PatientDto map(Row r, DataFormatter formatter) {
            PatientDto dto = new PatientDto();
            // assume header ordering: firstName,lastName,email,phone,nationalId,dob
            dto.setFirstName(formatter.formatCellValue(r.getCell(0)).trim());
            dto.setLastName(formatter.formatCellValue(r.getCell(1)).trim());
            dto.setEmail(formatter.formatCellValue(r.getCell(2)).trim());
            dto.setPhone(formatter.formatCellValue(r.getCell(3)).trim());
            dto.setNationalId(formatter.formatCellValue(r.getCell(4)).trim());
            String dobRaw = formatter.formatCellValue(r.getCell(5)).trim();
            if (!dobRaw.isBlank()) {
                try {
                    if (r.getCell(5).getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(r.getCell(5))) {
                        dto.setDob(r.getCell(5).getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                    } else {
                        dto.setDob(LocalDate.parse(dobRaw));
                    }
                } catch (Exception e) {
                    dto.setDob(null);
                }
            }
            return dto;
        }
    }
}
