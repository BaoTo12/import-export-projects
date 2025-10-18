package com.chibao.edu.components;

import com.chibao.edu.common.ImportOption;
import com.chibao.edu.dto.PatientDto;
import com.chibao.edu.entity.Patient;
import com.chibao.edu.exception.DuplicateException;
import com.chibao.edu.repository.PatientRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@StepScope
public class PatientProcessor implements ItemProcessor<PatientDto, Patient> {


    private final PatientRepository patientRepository;
    private final ImportOption option;


    public PatientProcessor(PatientRepository patientRepository, @Value("#{jobParameters['option']}") String option) {
        this.patientRepository = patientRepository;
        this.option = ImportOption.valueOf(option);
    }


    @Override
    public Patient process(PatientDto dto) throws Exception {
// basic validation
        if (dto.getFirstName() == null || dto.getFirstName().isBlank()) throw new ValidationException("firstName required");
        if (dto.getNationalId() == null || dto.getNationalId().isBlank()) throw new ValidationException("nationalId required");


        Optional<Patient> existing = patientRepository.findByNationalId(dto.getNationalId());
        if (existing.isPresent()) {
            switch (option) {
                case SKIP:
                    return null; // filtered out
                case UPDATE:
                    Patient e = existing.get();
                    e.setFirstName(dto.getFirstName());
                    e.setLastName(dto.getLastName());
                    e.setEmail(dto.getEmail());
                    e.setPhone(dto.getPhone());
                    e.setDob(dto.getDob());
                    return e;
                case FAIL:
                    throw new DuplicateException("Duplicate nationalId: " + dto.getNationalId());
            }
        }


        Patient p = new Patient();
        p.setFirstName(dto.getFirstName());
        p.setLastName(dto.getLastName());
        p.setEmail(dto.getEmail());
        p.setPhone(dto.getPhone());
        p.setNationalId(dto.getNationalId());
        p.setDob(dto.getDob());
        return p;
    }
}