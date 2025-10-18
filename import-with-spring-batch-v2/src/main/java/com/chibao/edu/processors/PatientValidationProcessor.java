package com.chibao.edu.processors;

import com.chibao.edu.common.DuplicateHandlingStrategy;
import com.chibao.edu.dtos.PatientImportDTO;
import com.chibao.edu.models.Patient;
import com.chibao.edu.repository.PatientRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientValidationProcessor implements ItemProcessor {

    private final Validator validator;
    private final PatientRepository patientRepository;
    @Setter
    private DuplicateHandlingStrategy duplicateStrategy;

    @Override
    public Patient process(PatientImportDTO dto) throws Exception {
        // Skip if already marked invalid during reading
        if (!dto.isValid()) {
            log.warn("Skipping invalid row {}: {}", dto.getRowNumber(), dto.getValidationErrors());
            return null;
        }

        // Convert DTO to Entity
        Patient patient = convertToEntity(dto);

        // Validate using Jakarta Validation
        Set<ConstraintViolation<Patient>> violations = validator.validate(patient);
        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            dto.setValid(false);
            dto.setValidationErrors(errors);
            log.warn("Validation failed for row {}: {}", dto.getRowNumber(), errors);
            return null;
        }

        // Handle duplicates
        boolean exists = patientRepository.existsByPatientId(patient.getPatientId());
        if (exists) {
            return switch (duplicateStrategy) {
                case SKIP -> {
                    log.info("Skipping duplicate patient: {}", patient.getPatientId());
                    yield null;
                }
                case UPDATE -> {
                    Patient existing = patientRepository.findByPatientId(patient.getPatientId())
                            .orElseThrow();
                    updateExistingPatient(existing, patient);
                    log.info("Updating existing patient: {}", patient.getPatientId());
                    yield existing;
                }
                case FAIL -> throw new Exception("Duplicate patient ID found: " + patient.getPatientId());
            };
        }

        return patient;
    }

    private Patient convertToEntity(PatientImportDTO dto) {
        return Patient.builder()
                .patientId(dto.getPatientId())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .zipCode(dto.getZipCode())
                .bloodType(dto.getBloodType())
                .medicalHistory(dto.getMedicalHistory())
                .build();
    }

    private void updateExistingPatient(Patient existing, Patient newData) {
        existing.setFirstName(newData.getFirstName());
        existing.setLastName(newData.getLastName());
        existing.setDateOfBirth(newData.getDateOfBirth());
        existing.setGender(newData.getGender());
        existing.setEmail(newData.getEmail());
        existing.setPhoneNumber(newData.getPhoneNumber());
        existing.setAddress(newData.getAddress());
        existing.setCity(newData.getCity());
        existing.setState(newData.getState());
        existing.setZipCode(newData.getZipCode());
        existing.setBloodType(newData.getBloodType());
        existing.setMedicalHistory(newData.getMedicalHistory());
    }
}
