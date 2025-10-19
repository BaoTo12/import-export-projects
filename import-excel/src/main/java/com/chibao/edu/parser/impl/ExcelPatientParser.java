package com.chibao.edu.parser.impl;


import com.chibao.edu.common.ParseResult;
import com.chibao.edu.common.RowResult;
import com.chibao.edu.parser.FileParser;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class ExcelPatientParser implements FileParser {
    private static final int MAX_ROWS = 1000;
    private static final List<String> EXPECTED_HEADERS = List.of(
            "firstName", "lastName", "email", "phone", "nationalId", "dob");
    private static final Pattern EMAIL_PATTERN
            = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean supports(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".xls") || lower.endsWith(".xlsx");
    }

    @Override
    public ParseResult parse(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) throw new IOException("File not found: " + filePath);

        try (Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = getFirstSheet(workbook);
            if (sheet == null) return ParseResult.error("Workbook has no sheets");

            Iterator<Row> rowIterator = sheet.rowIterator();
            if (!rowIterator.hasNext()) return ParseResult.error("Sheet is empty");

            DataFormatter formatter = new DataFormatter();
            List<String> headers = readHeaders(rowIterator.next(), formatter);

            // Validate header
            ParseResult headerCheck = validateHeaders(headers);
            Map<String, Integer> headerIndex = mapHeaderIndexes(headers);

            // Parse data rows
            List<RowResult> rows = parseDataRows(rowIterator, formatter, headerIndex);
            return new ParseResult(headerCheck.isHeaderValid(), rows, headerCheck.getMessage(), false);

        } catch (InvalidFormatException e) {
            throw new IOException("Invalid Excel format", e);
        }
    }

    private Sheet getFirstSheet(Workbook wb) {
        return wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
    }

    private List<String> readHeaders(Row headerRow, DataFormatter formatter) {
        List<String> headers = new ArrayList<>();
        for (Cell c : headerRow) headers.add(formatter.formatCellValue(c).trim());
        return headers;
    }

    private ParseResult validateHeaders(List<String> headers) {
        List<String> missing = EXPECTED_HEADERS.stream()
                .filter(h -> headers.stream().noneMatch(x -> x.equalsIgnoreCase(h)))
                .collect(Collectors.toList());
        boolean headerValid = missing.isEmpty();
        String message = headerValid ? null : "Missing headers: " + String.join(", ", missing);
        return new ParseResult(headerValid, Collections.emptyList(), message, false);
    }

    private Map<String, Integer> mapHeaderIndexes(List<String> headers) {
        Map<String, Integer> headerIndex = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            headerIndex.put(headers.get(i).toLowerCase(), i);
        }
        return headerIndex;
    }

    private List<RowResult> parseDataRows(Iterator<Row> rowIterator, DataFormatter formatter, Map<String, Integer> headerIndex) {
        List<RowResult> results = new ArrayList<>();
        int rowCount = 0;

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if (isRowBlank(row)) continue;
            if (++rowCount > MAX_ROWS)
                throw new IllegalStateException("Too many rows. Max allowed: " + MAX_ROWS);

            results.add(parseSingleRow(row, formatter, headerIndex));
        }
        return results;
    }

    private RowResult parseSingleRow(Row row, DataFormatter formatter, Map<String, Integer> headerIndex) {
        RowResult rr = new RowResult();
        rr.setFirstName(getCellString(row, headerIndex, "firstname", formatter));
        rr.setLastName(getCellString(row, headerIndex, "lastname", formatter));
        rr.setEmail(getCellString(row, headerIndex, "email", formatter));
        rr.setPhone(getCellString(row, headerIndex, "phone", formatter));
        rr.setNationalId(getCellString(row, headerIndex, "nationalid", formatter));

        String dobRaw = getCellString(row, headerIndex, "dob", formatter);
        rr.setDob(parseDob(row, headerIndex, dobRaw));
        rr.setErrors(validateRow(rr, dobRaw));
        return rr;
    }

    private LocalDate parseDob(Row row, Map<String, Integer> headerIndex, String dobRaw) {
        Integer idx = headerIndex.get("dob");
        if (idx == null) return null;
        Cell dobCell = row.getCell(idx);
        if (dobCell == null) return null;

        try {
            if (dobCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dobCell)) {
                Date d = dobCell.getDateCellValue();
                return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            return parseDateLenient(dobRaw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> validateRow(RowResult rr, String dobRaw) {
        List<String> errs = new ArrayList<>();
        if (rr.getFirstName() == null || rr.getFirstName().isBlank()) errs.add("firstName required");
        if (rr.getNationalId() == null || rr.getNationalId().isBlank()) errs.add("nationalId required");
        if (rr.getEmail() != null && !rr.getEmail().isBlank() && !isValidEmail(rr.getEmail()))
            errs.add("invalid email");
        if (rr.getDob() == null && dobRaw != null && !dobRaw.isBlank()) errs.add("dob not parseable");
        return errs;
    }

    private boolean isRowBlank(Row r) {
        DataFormatter formatter = new DataFormatter();
        for (Cell c : r) {
            if (!formatter.formatCellValue(c).trim().isEmpty()) return false;
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
        return e != null && EMAIL_PATTERN.matcher(e).matches();
    }
}