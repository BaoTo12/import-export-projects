package com.chibao.edu.common;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class RowResult {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String nationalId;
    private LocalDate dob;
    private List<String> errors = new ArrayList<>();

    // ✅ New helper method
    public static RowResult error(String message) {
        RowResult rr = new RowResult();
        rr.getErrors().add(message);
        return rr;
    }
}