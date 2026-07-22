package br.com.itbn.sisdent.config;

import br.com.itbn.sisdent.model.Address;
import br.com.itbn.sisdent.model.Gender;
import br.com.itbn.sisdent.model.Patient;
import br.com.itbn.sisdent.model.State;
import br.com.itbn.sisdent.model.Speciality;
import br.com.itbn.sisdent.repository.AddressRepository;
import br.com.itbn.sisdent.repository.PatientRepository;
import br.com.itbn.sisdent.repository.StateRepository;
import br.com.itbn.sisdent.repository.SpecialityRepository;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@NullMarked
public class InitialDataLoader implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitialDataLoader.class);
    private static final String INITIAL_DATA_PATH = "data/initial-data.json";

    private final JsonMapper jsonMapper;
    private final StateRepository stateRepository;
    private final SpecialityRepository specialityRepository;
    private final AddressRepository addressRepository;
    private final PatientRepository patientRepository;

    public InitialDataLoader(
            JsonMapper jsonMapper,
            StateRepository stateRepository,
            SpecialityRepository specialityRepository,
            AddressRepository addressRepository,
            PatientRepository patientRepository) {
        this.jsonMapper = jsonMapper;
        this.stateRepository = stateRepository;
        this.specialityRepository = specialityRepository;
        this.addressRepository = addressRepository;
        this.patientRepository = patientRepository;
    }

    @Override
    @Transactional(rollbackFor = IOException.class)
    public void run(ApplicationArguments arguments) throws IOException {
        InitialData initialData = readInitialData();

        if (hasExistingData()) {
            long insertedSpecialities = saveMissingSpecialities(initialData.specialities());
            LOGGER.info(
                    "Initial data loading skipped because the database is not empty; "
                            + "{} missing specialities were loaded",
                    insertedSpecialities);
            return;
        }

        Map<String, State> statesByAbbreviation = saveStates(initialData.states());
        Map<String, Speciality> specialitiesByName = saveSpecialities(initialData.specialities());
        Map<String, Address> addressesByReference = saveAddresses(
                initialData.addresses(), statesByAbbreviation);
        savePatients(initialData.patients(), addressesByReference, specialitiesByName);

        LOGGER.info(
                "Initial data loaded from {}: {} states, {} specialities, {} addresses and {} patients",
                INITIAL_DATA_PATH,
                initialData.states().size(),
                initialData.specialities().size(),
                initialData.addresses().size(),
                initialData.patients().size());
    }

    private boolean hasExistingData() {
        return stateRepository.count() > 0
                || specialityRepository.count() > 0
                || addressRepository.count() > 0
                || patientRepository.count() > 0;
    }

    private InitialData readInitialData() throws IOException {
        ClassPathResource resource = new ClassPathResource(INITIAL_DATA_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            return jsonMapper.readValue(inputStream, InitialData.class);
        }
    }

    private Map<String, State> saveStates(List<StateData> states) {
        List<State> savedStates = states.stream()
                .map(state -> new State(state.name(), state.abbreviation()))
                .map(stateRepository::save)
                .toList();

        return savedStates.stream()
                .collect(Collectors.toMap(State::getAbbreviation, Function.identity()));
    }

    private Map<String, Speciality> saveSpecialities(List<SpecialityData> specialities) {
        return specialities.stream()
                .map(speciality -> specialityRepository.save(new Speciality(speciality.name())))
                .collect(Collectors.toMap(Speciality::getName, Function.identity()));
    }

    private long saveMissingSpecialities(List<SpecialityData> specialities) {
        return specialities.stream()
                .filter(speciality -> specialityRepository.findByName(speciality.name()).isEmpty())
                .map(speciality -> specialityRepository.save(new Speciality(speciality.name())))
                .count();
    }

    private Map<String, Address> saveAddresses(
            List<AddressData> addresses,
            Map<String, State> statesByAbbreviation) {
        return addresses.stream()
                .collect(Collectors.toMap(
                        AddressData::reference,
                        address -> addressRepository.save(new Address(
                                address.street(),
                                address.district(),
                                address.additionalInfo(),
                                address.block(),
                                address.postalCode(),
                                requireReference(
                                        statesByAbbreviation,
                                        address.stateAbbreviation(),
                                        "state abbreviation")))));
    }

    private void savePatients(
            List<PatientData> patients,
            Map<String, Address> addressesByReference,
            Map<String, Speciality> specialitiesByName) {
        patients.stream()
                .map(patient -> new Patient(
                        patient.name(),
                        patient.birthDate(),
                        patient.active(),
                        patient.gender(),
                        patient.taxId(),
                        requireReference(
                                addressesByReference,
                                patient.addressReference(),
                                "address reference"),
                        patient.specialityNames().stream()
                                .map(name -> requireReference(
                                        specialitiesByName,
                                        name,
                                        "speciality name"))
                                .toList()))
                .forEach(patientRepository::save);
    }

    private <T> T requireReference(Map<String, T> values, String key, String referenceType) {
        T value = values.get(key);
        if (value == null) {
            throw new IllegalStateException(
                    "Unknown " + referenceType + " in " + INITIAL_DATA_PATH + ": " + key);
        }
        return value;
    }

    public record InitialData(
            List<StateData> states,
            List<SpecialityData> specialities,
            List<AddressData> addresses,
            List<PatientData> patients) {
    }

    public record StateData(String name, String abbreviation) {
    }

    public record SpecialityData(String name) {
    }

    public record AddressData(
            String reference,
            String street,
            String district,
            String additionalInfo,
            String block,
            String postalCode,
            String stateAbbreviation) {
    }

    public record PatientData(
            String name,
            LocalDate birthDate,
            boolean active,
            Gender gender,
            String taxId,
            String addressReference,
            List<String> specialityNames) {
    }
}
