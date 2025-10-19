package com.chibao.edu.processors;

import com.chibao.edu.common.DuplicateStrategy;
import com.chibao.edu.dto.PatientImportDTO;
import com.chibao.edu.entity.Patient;
import com.chibao.edu.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class PatientImportProcessor implements ItemProcessor<PatientImportDTO, Patient> {

    private final PatientRepository patientRepository;

    @Value("#{jobParameters['duplicateStrategy']}")
    private String duplicateStrategyStr;

    @Override
    public Patient process(PatientImportDTO item) throws Exception {
        DuplicateStrategy strategy = DuplicateStrategy.valueOf(duplicateStrategyStr);

        Optional<Patient> existingPatient = patientRepository.findByEmail(item.getEmail());

        if (existingPatient.isPresent()) {
            return handleDuplicate(existingPatient.get(), item, strategy);
        }

        return mapToEntity(item);
    }

    private Patient handleDuplicate(Patient existing, PatientImportDTO dto, DuplicateStrategy strategy) {
        return switch (strategy) {
            case SKIP -> {
                log.debug("Skipping duplicate email: {}", dto.getEmail());
                yield null; // Skip this item
            }
            case UPDATE -> {
                log.debug("Updating existing patient with email: {}", dto.getEmail());
                updateExisting(existing, dto);
                yield existing;
            }
            case FAIL -> throw new RuntimeException("Duplicate email found: " + dto.getEmail());
        };
    }

    private Patient mapToEntity(PatientImportDTO dto) {
        return Patient.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .dateOfBirth(dto.getDateOfBirth())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .bloodType(dto.getBloodType())
                .build();
    }

    private void updateExisting(Patient existing, PatientImportDTO dto) {
        existing.setFirstName(dto.getFirstName());
        existing.setLastName(dto.getLastName());
        existing.setDateOfBirth(dto.getDateOfBirth());
        existing.setPhone(dto.getPhone());
        existing.setAddress(dto.getAddress());
        existing.setBloodType(dto.getBloodType());
    }
}