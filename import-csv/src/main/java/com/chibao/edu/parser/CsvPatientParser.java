package com.chibao.edu.parser;

import com.chibao.edu.common.FileParser;
import com.chibao.edu.common.ParseResult;
import com.chibao.edu.common.RowResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class CsvPatientParser implements FileParser {
    private static final int MAX_ROWS = 1000;
    private static final List<String> EXPECTED_HEADERS = List.of("firstName","lastName","email","phone","nationalId","dob");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);


    @Override
    public boolean supports(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".csv");
    }

    @Override
    public ParseResult parse(String filePath) throws IOException {
        File f = new File(filePath);
        if (!f.exists()) throw new IOException("File not found: " + filePath);


        try (Reader reader = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
            CSVParser parser = CSVParser.parse(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim());
            Map<String, Integer> headerMap = parser.getHeaderMap();
            List<String> headers = headerMap.keySet().stream().collect(Collectors.toList());


            List<String> missing = EXPECTED_HEADERS.stream()
                    .filter(h -> headerMap.keySet().stream().noneMatch(x -> x.equalsIgnoreCase(h)))
                    .collect(Collectors.toList());
            boolean headerValid = missing.isEmpty();
            String headerMessage = headerValid ? null : "Missing headers: " + String.join(", ", missing);


            List<RowResult> rows = new ArrayList<>();
            int count = 0;
            for (CSVRecord record : parser) {
                count++;
                if (count > MAX_ROWS) return ParseResult.tooManyRows("Too many rows. Max allowed: " + MAX_ROWS);


                RowResult rr = new RowResult();
                rr.setFirstName(get(record, "firstName"));
                rr.setLastName(get(record, "lastName"));
                rr.setEmail(get(record, "email"));
                rr.setPhone(get(record, "phone"));
                rr.setNationalId(get(record, "nationalId"));


                String dobRaw = get(record, "dob");
                LocalDate dob = null;
                if (dobRaw != null && !dobRaw.isBlank()) {
                    try { dob = LocalDate.parse(dobRaw); }
                    catch (DateTimeParseException ex) { dob = parseDateLenient(dobRaw); }
                }
                rr.setDob(dob);


                List<String> errs = new ArrayList<>();
                if (rr.getFirstName() == null || rr.getFirstName().isBlank()) errs.add("firstName required");
                if (rr.getNationalId() == null || rr.getNationalId().isBlank()) errs.add("nationalId required");
                if (rr.getEmail() != null && !rr.getEmail().isBlank() && !isValidEmail(rr.getEmail())) errs.add("invalid email");
                if (rr.getDob() == null && dobRaw != null && !dobRaw.isBlank()) errs.add("dob not parseable");
                rr.setErrors(errs);
                rows.add(rr);
            }
            return new ParseResult(headerValid, rows, headerMessage, false);
        }
    }

    private String get(CSVRecord r, String col) {
        try { return r.isSet(col) ? r.get(col).trim() : null; } catch (IllegalArgumentException ex) { return null; }
    }

    private LocalDate parseDateLenient(String txt) {
        if (txt == null) return null;
        List<String> patterns = List.of("d/M/yyyy","d/M/yy","dd/MM/yyyy","dd-MM-yyyy","MM/dd/yyyy");
        for (String p : patterns) {
            try {
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern(p);
                return LocalDate.parse(txt, fmt);
            } catch (Exception ignored) {}
        }
        return null;
    }


    private boolean isValidEmail(String e) {
        if (e == null) return false;
        return EMAIL_PATTERN.matcher(e).matches();
    }
}
