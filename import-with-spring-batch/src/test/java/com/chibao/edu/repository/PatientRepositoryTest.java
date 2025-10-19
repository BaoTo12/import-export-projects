package com.chibao.edu.repository;

import com.chibao.edu.entity.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PatientRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PatientRepository patientRepository;

    @Test
    void findByEmail_shouldReturnPatient() {
        // Arrange
        Patient patient = Patient.builder()
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("john@example.com")
                .phone("+1234567890")
                .build();

        entityManager.persistAndFlush(patient);

        // Act
        Optional<Patient> found = patientRepository.findByEmail("john@example.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("John");
    }

    @Test
    void existsByEmail_shouldReturnTrue() {
        // Arrange
        Patient patient = Patient.builder()
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .email("jane@example.com")
                .phone("+0987654321")
                .build();

        entityManager.persistAndFlush(patient);

        // Act
        boolean exists = patientRepository.existsByEmail("jane@example.com");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_shouldReturnFalse() {
        // Act
        boolean exists = patientRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertThat(exists).isFalse();
    }
}