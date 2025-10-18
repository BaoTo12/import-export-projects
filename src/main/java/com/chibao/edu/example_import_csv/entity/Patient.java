package com.chibao.edu.example_import_csv.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
@Entity
@Table(name = "patients", uniqueConstraints = {
        @UniqueConstraint(columnNames = "national_id")
})
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "national_id", nullable = false, unique = true)
    private String nationalId;

    @Column(name = "dob")
    private LocalDate dob;


}