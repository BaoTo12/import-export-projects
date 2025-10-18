package com.chibao.edu.example_import_csv.repository;

import com.chibao.edu.example_import_csv.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByNationalId(String nationalId);
    boolean existsByNationalId(String nationalId);
}

