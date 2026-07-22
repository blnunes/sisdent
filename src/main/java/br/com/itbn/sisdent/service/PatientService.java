package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.PatientRequest;
import br.com.itbn.sisdent.dto.PatientResponse;
import br.com.itbn.sisdent.mapper.ResponseMapper;
import br.com.itbn.sisdent.model.Address;
import br.com.itbn.sisdent.model.Patient;
import br.com.itbn.sisdent.repository.PatientRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final AddressService addressService;
    private final SpecialityService specialityService;

    public PatientService(
            PatientRepository patientRepository,
            AddressService addressService,
            SpecialityService specialityService) {
        this.patientRepository = patientRepository;
        this.addressService = addressService;
        this.specialityService = specialityService;
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> findAll() {
        return patientRepository.findAll(Sort.by("name")).stream()
                .map(ResponseMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<PatientResponse> findById(Long id) {
        return patientRepository.findById(id)
                .map(ResponseMapper::toResponse);
    }

    @Transactional
    public PatientResponse create(PatientRequest request) {
        Address address = addressService.findOrCreate(request.address());
        Patient patient = new Patient(
                request.name(),
                request.birthDate(),
                request.active(),
                request.gender(),
                request.taxId(),
                address,
                specialityService.findAllByIds(request.specialityIds()));
        return ResponseMapper.toResponse(patientRepository.save(patient));
    }

}
