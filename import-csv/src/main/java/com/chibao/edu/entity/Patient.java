package com.chibao.edu.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "patients", uniqueConstraints = @UniqueConstraint(columnNames = "national_id"))
@Data
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "first_name")
    private String firstName;


    @Column(name = "last_name")
    private String lastName;


    private String email;
    private String phone;


    @Column(name = "national_id", nullable = false, unique = true)
    private String nationalId;


    private LocalDate dob;
}