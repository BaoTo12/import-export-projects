package com.chibao.edu.parser;

import com.chibao.edu.common.FileParser;
import com.chibao.edu.common.ParseResult;
import com.chibao.edu.common.RowResult;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;


@Component
public class ExcelPatientParser implements FileParser {
    private static final int MAX_ROWS = 1000;
    private static final List<String> EXPECTED_HEADERS = List.of("firstName", "lastName", "email", "phone", "nationalId", "dob");


    @Override
    public boolean supports(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".xls") || lower.endsWith(".xlsx");
    }

    @Override
    public ParseResult parse(String filePath) throws IOException {
        File f = new File(filePath);
        if (!f.exists()) throw new IOException("File not found: " + filePath);
        try (Workbook wb = WorkbookFactory.create(f)) {
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) return ParseResult.error("Workbook has no sheets");


            Iterator<Row> rowIt = sheet.rowIterator();
            if (!rowIt.hasNext()) return ParseResult.error("Sheet is empty");


            DataFormatter formatter = new DataFormatter();
            Row headerRow = rowIt.next();
            List<String> headers = new ArrayList<>();
            for (Cell c : headerRow) headers.add(formatter.formatCellValue(c).trim());
            List<String> missing = EXPECTED_HEADERS.stream()
                    .filter(h -> headers.stream().noneMatch(x -> x.equalsIgnoreCase(h)))
                    .collect(Collectors.toList());
            boolean headerValid = missing.isEmpty();
            String headerMessage = headerValid ? null : "Missing headers: " + String.join(", ", missing);


            List<RowResult> rows = new ArrayList<>();
            int rowCount = 0;
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headers.size(); i++) headerIndex.put(headers.get(i).toLowerCase(), i);

            while (rowIt.hasNext()) {
                Row r = rowIt.next();
                if (isRowBlank(r)) continue;
                rowCount++;
                if (rowCount > MAX_ROWS) return ParseResult.tooManyRows("Too many rows. Max allowed: " + MAX_ROWS);


                RowResult rr = new RowResult();
                rr.setFirstName(getCellString(r, headerIndex, "firstname", formatter));
                rr.setLastName(getCellString(r, headerIndex, "lastname", formatter));
                rr.setEmail(getCellString(r, headerIndex, "email", formatter));
                rr.setPhone(getCellString(r, headerIndex, "phone", formatter));
                rr.setNationalId(getCellString(r, headerIndex, "nationalid", formatter));


                // dob handling
                String dobRaw = getCellString(r, headerIndex, "dob", formatter);
                LocalDate dob = null;
                Integer idx = headerIndex.get("dob");
                if (idx != null) {
                    Cell dobCell = r.getCell(idx);
                    if (dobCell != null) {
                        try {
                            if (dobCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dobCell)) {
                                Date d = dobCell.getDateCellValue();
                                dob = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                            } else {
                                dob = parseDateLenient(dobRaw);
                            }
                        } catch (Exception ex) {
                            dob = null;
                        }
                    }
                }
                rr.setDob(dob);
                // validations
                List<String> errs = new ArrayList<>();
                if (rr.getFirstName() == null || rr.getFirstName().isBlank()) errs.add("firstName required");
                if (rr.getNationalId() == null || rr.getNationalId().isBlank()) errs.add("nationalId required");
                if (rr.getEmail() != null && !rr.getEmail().isBlank() && !isValidEmail(rr.getEmail()))
                    errs.add("invalid email");
                if (rr.getDob() == null && dobRaw != null && !dobRaw.isBlank()) errs.add("dob not parseable");
                rr.setErrors(errs);
                rows.add(rr);
            }
            return new ParseResult(headerValid, rows, headerMessage, false);

        } catch (InvalidFormatException e) {
            throw new IOException("Invalid Excel format", e);
        }
    }

    private boolean isRowBlank(Row r) {
        DataFormatter formatter = new DataFormatter();
        for (Cell c : r) {
            String txt = formatter.formatCellValue(c).trim();
            if (!txt.isEmpty()) return false;
        }
        return true;
    }


    private String getCellString(Row r, Map<String, Integer> headerIndex, String colName, DataFormatter formatter) {
        Integer idx = headerIndex.get(colName.toLowerCase());
        if (idx == null) return null;
        Cell cell = r.getCell(idx);
        if (cell == null) return null;
        return formatter.formatCellValue(cell).trim();
    }


    private LocalDate parseDateLenient(String txt) {
        if (txt == null) return null;
        try {
            return LocalDate.parse(txt);
        } catch (Exception ignored) {
        }
        List<String> patterns = List.of("d/M/yyyy", "d/M/yy", "dd/MM/yyyy", "dd-MM-yyyy", "MM/dd/yyyy");
        for (String p : patterns) {
            try {
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern(p);
                return LocalDate.parse(txt, fmt);
            } catch (Exception ignored) {
            }
        }
        return null;
    }


    private boolean isValidEmail(String e) {
        try {
            jakarta.mail.internet.InternetAddress ia = new jakarta.mail.internet.InternetAddress(e);
            ia.validate();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
