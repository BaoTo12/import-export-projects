package com.chibao.edu.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientDto {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String nationalId;
    private LocalDate dob;
}