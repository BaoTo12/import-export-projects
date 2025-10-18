package com.chibao.edu.example_import_csv.dto;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class RowResult {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String nationalId;
    private LocalDate dob;
    private List<String> errors = new ArrayList<>();
    // getters/setters
}