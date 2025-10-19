package com.chibao.edu.dto;

import com.opencsv.bean.CsvBindByName;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientImportDTO {
    @CsvBindByName(column = "firstName", required = true)
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @CsvBindByName(column = "lastName", required = true)
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @CsvBindByName(column = "dateOfBirth", required = true)
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @CsvBindByName(column = "email", required = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @CsvBindByName(column = "phone", required = true)
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone must be valid")
    private String phone;

    @CsvBindByName(column = "address")
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @CsvBindByName(column = "bloodType")
    @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Blood type must be valid (e.g., A+, O-)")
    private String bloodType;

    private Integer rowNumber;
}
