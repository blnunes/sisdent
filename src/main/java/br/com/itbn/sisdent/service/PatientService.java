package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.dto.AddressRequest;
import br.com.itbn.sisdent.dto.PatientRequest;
import br.com.itbn.sisdent.dto.PatientResponse;
import br.com.itbn.sisdent.dto.StateRequest;
import br.com.itbn.sisdent.mapper.ResponseMapper;
import br.com.itbn.sisdent.model.Address;
import br.com.itbn.sisdent.model.Patient;
import br.com.itbn.sisdent.model.State;
import br.com.itbn.sisdent.repository.AddressRepository;
import br.com.itbn.sisdent.repository.PatientRepository;
import br.com.itbn.sisdent.repository.StateRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final AddressRepository addressRepository;
    private final StateRepository stateRepository;

    public PatientService(
            PatientRepository patientRepository,
            AddressRepository addressRepository,
            StateRepository stateRepository) {
        this.patientRepository = patientRepository;
        this.addressRepository = addressRepository;
        this.stateRepository = stateRepository;
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
        Address address = findOrCreateAddress(request.address());
        Patient patient = new Patient(
                request.name(),
                request.birthDate(),
                request.active(),
                request.gender(),
                request.taxId(),
                address);
        return ResponseMapper.toResponse(patientRepository.save(patient));
    }

    private Address findOrCreateAddress(AddressRequest request) {
        return addressRepository.findByPostalCode(request.postalCode())
                .orElseGet(() -> addressRepository.save(new Address(
                        request.street(),
                        request.district(),
                        request.additionalInfo(),
                        request.block(),
                        request.postalCode(),
                        findOrCreateState(request.state()))));
    }

    private State findOrCreateState(StateRequest request) {
        return stateRepository.findByAbbreviation(request.abbreviation())
                .orElseGet(() -> stateRepository.save(
                        new State(request.name(), request.abbreviation())));
    }
}
