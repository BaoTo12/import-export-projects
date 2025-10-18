package com.chibao.edu.example_import_csv.parser;

import com.chibao.edu.example_import_csv.common.ParseResult;
import com.chibao.edu.example_import_csv.dto.RowResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class CsvPatientParser {

    private static final int MAX_ROWS = 1000;
    private static final List<String> EXPECTED_HEADERS = List.of("firstName","lastName","email","phone","nationalId","dob");

    // simple RFC-5322-ish email regex (reasonable for validation, not perfect)
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    public ParseResult parse(InputStream inputStream) throws IOException {
        List<RowResult> rows = new ArrayList<>();
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            CSVFormat fmt = CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim();
            CSVParser parser = new CSVParser(reader, fmt);
            Map<String,Integer> headerMap = parser.getHeaderMap();

            // validate header names (loose vs strict)
            List<String> missing = EXPECTED_HEADERS.stream()
                    .filter(h -> !headerMap.containsKey(h))
                    .toList();

            boolean headerValid = missing.isEmpty();

            int count = 0;
            for (CSVRecord record : parser) {
                count++;
                if (count > MAX_ROWS) {
                    // ParseResult(headerValid, rows, message, tooManyRows)
                    return new ParseResult(headerValid, rows, "Too many rows. Max allowed: " + MAX_ROWS, true);
                }
                RowResult rr = mapRecord(record);
                rows.add(rr);
            }
            // ParseResult(headerValid, rows, message, tooManyRows)
            return new ParseResult(headerValid, rows, null, false);
        }
    }

    private RowResult mapRecord(CSVRecord r) {
        RowResult rr = new RowResult();
        rr.setFirstName(r.get("firstName"));
        rr.setLastName(r.isMapped("lastName") ? r.get("lastName") : null);
        rr.setEmail(r.isMapped("email") ? r.get("email") : null);
        rr.setPhone(r.isMapped("phone") ? r.get("phone") : null);
        rr.setNationalId(r.isMapped("nationalId") ? r.get("nationalId") : null);
        rr.setDob(parseDateSafe(r.isMapped("dob") ? r.get("dob") : null));
        // validate fields
        List<String> errs = new ArrayList<>();
        if (rr.getFirstName() == null || rr.getFirstName().isBlank()) errs.add("firstName required");
        if (rr.getNationalId() == null || rr.getNationalId().isBlank()) errs.add("nationalId required");
        if (rr.getEmail() != null && !rr.getEmail().isBlank() && !isValidEmail(rr.getEmail())) errs.add("invalid email");
        // ... more validations
        rr.setErrors(errs);
        return rr;
    }

    private LocalDate parseDateSafe(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s); // expects ISO yyyy-MM-dd; adapt as needed
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private boolean isValidEmail(String e) {
        if (e == null) return false;
        return EMAIL_PATTERN.matcher(e).matches();
    }

    // DTOs: ParseResult, RowResult
}