package br.com.itbn.sisdent.config;

import br.com.itbn.sisdent.model.Address;
import br.com.itbn.sisdent.model.AdministrativeDivision;
import br.com.itbn.sisdent.model.Continent;
import br.com.itbn.sisdent.model.Country;
import br.com.itbn.sisdent.model.DocumentType;
import br.com.itbn.sisdent.model.Gender;
import br.com.itbn.sisdent.model.Patient;
import br.com.itbn.sisdent.model.Speciality;
import br.com.itbn.sisdent.model.Account;
import br.com.itbn.sisdent.model.ClinicUnit;
import br.com.itbn.sisdent.model.Membership;
import br.com.itbn.sisdent.model.MembershipRole;
import br.com.itbn.sisdent.model.Organization;
import br.com.itbn.sisdent.model.Person;
import br.com.itbn.sisdent.model.PatientOrganizationLink;
import br.com.itbn.sisdent.model.PatientLinkBasis;
import br.com.itbn.sisdent.model.Practitioner;
import br.com.itbn.sisdent.model.Appointment;
import br.com.itbn.sisdent.model.AppointmentStatus;
import br.com.itbn.sisdent.model.PerformedProcedure;
import br.com.itbn.sisdent.repository.AccountRepository;
import br.com.itbn.sisdent.repository.ClinicUnitRepository;
import br.com.itbn.sisdent.repository.MembershipRepository;
import br.com.itbn.sisdent.repository.OrganizationRepository;
import br.com.itbn.sisdent.repository.PersonRepository;
import br.com.itbn.sisdent.repository.PatientOrganizationLinkRepository;
import br.com.itbn.sisdent.repository.PractitionerRepository;
import br.com.itbn.sisdent.repository.AppointmentRepository;
import br.com.itbn.sisdent.repository.PerformedProcedureRepository;
import br.com.itbn.sisdent.repository.AdministrativeDivisionRepository;
import br.com.itbn.sisdent.repository.AddressRepository;
import br.com.itbn.sisdent.repository.CountryRepository;
import br.com.itbn.sisdent.repository.PatientRepository;
import br.com.itbn.sisdent.repository.SpecialityRepository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

@Component
@Order(1)
@NullMarked
public class InitialDataLoader implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitialDataLoader.class);
    private static final String INITIAL_DATA_PATH = "data/initial-data.json";
    private static final Duration SEEDED_APPOINTMENT_DURATION = Duration.ofMinutes(30);
    private static final Duration SCHEDULED_APPOINTMENT_WINDOW = Duration.ofHours(5);

    private final JsonMapper jsonMapper;
    private final CountryRepository countryRepository;
    private final AdministrativeDivisionRepository administrativeDivisionRepository;
    private final SpecialityRepository specialityRepository;
    private final AddressRepository addressRepository;
    private final PatientRepository patientRepository;
    private final OrganizationRepository organizationRepository;
    private final ClinicUnitRepository clinicUnitRepository;
    private final AccountRepository accountRepository;
    private final PersonRepository personRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final PatientOrganizationLinkRepository patientLinkRepository;
    private final PractitionerRepository practitionerRepository;
    private final AppointmentRepository appointmentRepository;
    private final PerformedProcedureRepository performedProcedureRepository;

    public InitialDataLoader(
            JsonMapper jsonMapper,
            CountryRepository countryRepository,
            AdministrativeDivisionRepository administrativeDivisionRepository,
            SpecialityRepository specialityRepository,
            AddressRepository addressRepository,
            PatientRepository patientRepository, OrganizationRepository organizationRepository,
            ClinicUnitRepository clinicUnitRepository, AccountRepository accountRepository,
            PersonRepository personRepository,
            MembershipRepository membershipRepository, PasswordEncoder passwordEncoder,
            PatientOrganizationLinkRepository patientLinkRepository, PractitionerRepository practitionerRepository,
            AppointmentRepository appointmentRepository, PerformedProcedureRepository performedProcedureRepository) {
        this.jsonMapper = jsonMapper;
        this.countryRepository = countryRepository;
        this.administrativeDivisionRepository = administrativeDivisionRepository;
        this.specialityRepository = specialityRepository;
        this.addressRepository = addressRepository;
        this.patientRepository = patientRepository;
        this.organizationRepository = organizationRepository; this.clinicUnitRepository = clinicUnitRepository;
        this.accountRepository = accountRepository;
        this.personRepository = personRepository; this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.patientLinkRepository = patientLinkRepository; this.practitionerRepository = practitionerRepository;
        this.appointmentRepository = appointmentRepository;
        this.performedProcedureRepository = performedProcedureRepository;
    }

    @Override
    @Transactional(rollbackFor = IOException.class)
    public void run(ApplicationArguments arguments) throws IOException {
        InitialData initialData = readInitialData();
        Map<String, Country> countriesByCode = saveCountries(initialData.countries());
        Map<String, AdministrativeDivision> divisionsByCode = saveAdministrativeDivisions(
                initialData.administrativeDivisions(),
                countriesByCode,
                initialData.seedDefaults().addressCountryCode());
        Map<String, Speciality> specialitiesByName = saveSpecialities(initialData.specialities());
        Map<String, Address> addressesByReference = saveAddresses(
                initialData.addresses(),
                divisionsByCode,
                countriesByCode,
                initialData.seedDefaults().addressCountryCode());
        savePatients(
                initialData.patients(),
                addressesByReference,
                specialitiesByName,
                countriesByCode,
                initialData.seedDefaults());
        saveOrganizations(initialData.organizations());
        saveClinicUnits(initialData.clinicUnits());
        saveDemoProfiles(initialData.demoProfiles());
        saveOperationalDemo(initialData.operationalDemo(), specialitiesByName);

        LOGGER.info(
                "Initial data synchronized from {}: {} countries, {} administrative divisions, {} specialities, {} addresses and {} patients",
                INITIAL_DATA_PATH,
                initialData.countries().size(),
                initialData.administrativeDivisions().size(),
                initialData.specialities().size(),
                initialData.addresses().size(),
                initialData.patients().size());
    }

    private void saveOperationalDemo(List<DemoOrganizationData> organizations, Map<String, Speciality> specialities) {
        Instant seedTime = Instant.now();
        for (DemoOrganizationData data : organizations) {
            Organization organization = organizationRepository.findByName(data.organizationName()).orElseThrow();
            ClinicUnit clinic = clinicUnitRepository.findByOrganization_IdAndName(organization.getId(), data.clinicUnitName()).orElseThrow();
            List<PatientOrganizationLink> links = data.patientTaxIds().stream().map(taxId -> {
                Patient patient = patientRepository.findByTaxId(taxId).orElseThrow();
                return patientLinkRepository.findFirstByPatient_GlobalIdAndOrganization_GlobalId(patient.getGlobalId(), organization.getGlobalId())
                        .orElseGet(() -> patientLinkRepository.save(new PatientOrganizationLink(patient, organization, clinic, PatientLinkBasis.ATTENDANCE)));
            }).toList();
            List<Practitioner> practitioners = data.practitioners().stream().map(practitioner -> practitionerRepository
                    .findAllByOrganization_GlobalIdOrderByDisplayName(organization.getGlobalId()).stream()
                    .filter(existing -> existing.getDisplayName().equals(practitioner.displayName())).findFirst()
                    .orElseGet(() -> practitionerRepository.save(new Practitioner(organization, null, practitioner.displayName(),
                            practitioner.registrationNumber(), practitioner.specialityNames().stream().map(specialities::get)
                                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)))))).toList();
            if (!appointmentRepository.existsByClinicUnit_Id(clinic.getId())) {
                SeedAppointmentTimes times = randomAppointmentTimes(seedTime, ThreadLocalRandom.current());
                Appointment scheduled = new Appointment(organization, clinic, links.getFirst(), practitioners.getFirst(),
                        times.scheduledStart(), times.scheduledEnd(), "Europe/Lisbon");
                appointmentRepository.save(scheduled);
                Appointment completed = new Appointment(organization, clinic, links.get(Math.min(1, links.size() - 1)), practitioners.getFirst(),
                        times.completedStart(), times.completedEnd(), "Europe/Lisbon");
                completed.transition(AppointmentStatus.COMPLETED);
                appointmentRepository.save(completed);
                PerformedProcedure performedProcedure = new PerformedProcedure(
                        completed,
                        practitioners.getFirst().getSpecialities().iterator().next().getProcedures().iterator().next(),
                        completed.getEndAt(),
                        "Seeded completed appointment record");
                performedProcedureRepository.save(performedProcedure);
            }
        }
    }

    /** Creates distinct upcoming schedules without keeping clock-dependent data in the seed JSON. */
    static SeedAppointmentTimes randomAppointmentTimes(Instant seedTime, RandomGenerator random) {
        long offsetMinutes = random.nextLong(1, SCHEDULED_APPOINTMENT_WINDOW.toMinutes() + 1);
        Instant scheduledStart = seedTime.plus(Duration.ofMinutes(offsetMinutes));
        Instant completedStart = scheduledStart.minus(Duration.ofDays(1));
        return new SeedAppointmentTimes(
                scheduledStart,
                scheduledStart.plus(SEEDED_APPOINTMENT_DURATION),
                completedStart,
                completedStart.plus(SEEDED_APPOINTMENT_DURATION));
    }

    private void saveDemoProfiles(List<DemoProfileData> profiles) {
        for (DemoProfileData profile : profiles) {
            Account account = accountRepository.findByEmail(Account.normalizeEmail(profile.email())).orElseGet(() -> {
                Person person = personRepository.save(new Person(profile.displayName()));
                return accountRepository.save(new Account(person, profile.email(),
                        passwordEncoder.encode(profile.password()), profile.platformAdministrator()));
            });
            String organizationName = Objects.requireNonNullElse(profile.organizationName(), "");
            if (organizationName.isEmpty()) continue;
            Organization organization = organizationRepository.findByName(organizationName).orElseThrow(() ->
                    new IllegalStateException("Unknown organization in " + INITIAL_DATA_PATH + ": " + organizationName));
            String clinicUnitName = Objects.requireNonNullElse(profile.clinicUnitName(), "");
            ClinicUnit clinic = clinicUnitName.isEmpty() ? null : clinicUnitRepository
                    .findByOrganization_IdAndName(organization.getId(), clinicUnitName)
                    .orElseThrow(() -> new IllegalStateException(
                            "Unknown clinic unit in " + INITIAL_DATA_PATH + ": " + clinicUnitName));
            boolean exists = clinic == null
                    ? membershipRepository.existsByAccount_IdAndOrganization_IdAndClinicUnitIsNull(account.getId(), organization.getId())
                    : membershipRepository.existsByAccount_IdAndOrganization_IdAndClinicUnit_Id(account.getId(), organization.getId(), clinic.getId());
            if (!exists) membershipRepository.save(new Membership(account, organization, clinic, profile.membershipRole()));
            if (profile.membershipRole() == MembershipRole.ORGANIZATION_ADMIN && clinic == null) {
                account.assignAccountManagementOrganizationIfAbsent(organization);
            }
        }
    }

    private void saveOrganizations(List<OrganizationData> organizations) {
        organizations.forEach(data -> {
            if (organizationRepository.findByName(data.name()).isEmpty()) {
                organizationRepository.save(new Organization(data.name()));
            }
        });
    }

    private void saveClinicUnits(List<ClinicUnitData> clinicUnits) {
        clinicUnits.forEach(data -> {
            Organization organization = organizationRepository.findByName(data.organizationName()).orElseThrow(() ->
                    new IllegalStateException("Unknown organization in " + INITIAL_DATA_PATH + ": " + data.organizationName()));
            if (clinicUnitRepository.findByOrganization_IdAndName(organization.getId(), data.name()).isEmpty()) {
                clinicUnitRepository.save(new ClinicUnit(organization, data.name()));
            }
        });
    }

    private Map<String, Country> saveCountries(List<CountryData> countries) {
        return countries.stream()
                .map(country -> countryRepository.findByCode(country.code())
                        .orElseGet(() -> countryRepository.save(new Country(
                                country.name(),
                                country.code(),
                                country.continent()))))
                .collect(Collectors.toMap(Country::getCode, Function.identity()));
    }

    private InitialData readInitialData() throws IOException {
        ClassPathResource resource = new ClassPathResource(INITIAL_DATA_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            return jsonMapper.readValue(inputStream, InitialData.class);
        }
    }

    private Map<String, AdministrativeDivision> saveAdministrativeDivisions(
            List<AdministrativeDivisionData> divisions,
            Map<String, Country> countriesByCode,
            String defaultCountryCode) {
        List<AdministrativeDivision> savedDivisions = divisions.stream()
                .map(division -> {
                    String countryCode = countryCode(division.countryCode(), defaultCountryCode);
                    Country country = requireReference(countriesByCode, countryCode, "country code");
                    return administrativeDivisionRepository.findByCountry_CodeAndCode(countryCode, division.code())
                        .orElseGet(() -> administrativeDivisionRepository.save(
                                new AdministrativeDivision(
                                        division.name(),
                                        division.code(),
                                        division.type(),
                                        country)));
                })
                .toList();

        return savedDivisions.stream()
                .collect(Collectors.toMap(AdministrativeDivision::getCode, Function.identity()));
    }

    private Map<String, Speciality> saveSpecialities(List<SpecialityData> specialities) {
        return specialities.stream()
                .map(speciality -> {
                    Speciality entity = specialityRepository.findByName(speciality.name())
                            .orElseGet(() -> new Speciality(speciality.name()));
                    entity.addMissingProcedures(speciality.procedures());
                    return specialityRepository.save(entity);
                })
                .collect(Collectors.toMap(Speciality::getName, Function.identity()));
    }

    private Map<String, Address> saveAddresses(
            List<AddressData> addresses,
            Map<String, AdministrativeDivision> divisionsByCode,
            Map<String, Country> countriesByCode,
            String defaultCountryCode) {
        return addresses.stream()
                .collect(Collectors.toMap(
                        AddressData::reference,
                        address -> {
                            String countryCode = countryCode(address.countryCode(), defaultCountryCode);
                            AdministrativeDivision division = requireReference(
                                    divisionsByCode,
                                    address.administrativeDivisionCode(),
                                    "administrative division code");
                            if (!division.getCountry().getCode().equals(countryCode)) {
                                throw new IllegalStateException("Address country must match its administrative division in "
                                        + INITIAL_DATA_PATH + ": " + address.reference());
                            }
                            return addressRepository
                                .findAllByCountry_CodeAndPostalCodeOrderByStreet(
                                        countryCode,
                                        address.postalCode())
                                .stream()
                                .filter(existing -> existing.getStreet().equals(address.street()))
                                .findFirst()
                                .orElseGet(() -> addressRepository.save(new Address(
                                        address.street(),
                                        address.district(),
                                        address.city() == null ? address.district() : address.city(),
                                        address.additionalInfo(),
                                        address.block(),
                                        address.postalCode(),
                                        division,
                                        requireReference(
                                                countriesByCode,
                                                countryCode,
                                                "country code"))));
                        }));
    }

    private void savePatients(
            List<PatientData> patients,
            Map<String, Address> addressesByReference,
            Map<String, Speciality> specialitiesByName,
            Map<String, Country> countriesByCode,
            SeedDefaults seedDefaults) {
        patients.stream()
                .filter(patient -> patientRepository.findByTaxId(patient.taxId()).isEmpty())
                .map(patient -> new Patient(
                        patient.name(),
                        patient.birthDate(),
                        patient.active(),
                        patient.gender(),
                        patient.taxId(),
                        seedDefaults.identificationType(),
                        seedDefaults.identificationPrefix() + patient.taxId(),
                        requireReference(
                                countriesByCode,
                                seedDefaults.patientNationalityCode(),
                                "document issuer country code"),
                        requireReference(
                                countriesByCode,
                                patient.nationalityCode() == null || patient.nationalityCode().isBlank()
                                        ? seedDefaults.patientNationalityCode()
                                        : patient.nationalityCode(),
                                "nationality country code"),
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

    private String countryCode(@Nullable String value, String defaultCountryCode) {
        return value == null || value.isBlank() ? defaultCountryCode : value;
    }

    public record InitialData(
            List<CountryData> countries,
            SeedDefaults seedDefaults,
            List<AdministrativeDivisionData> administrativeDivisions,
            List<SpecialityData> specialities,
            List<AddressData> addresses,
            List<PatientData> patients,
            List<OrganizationData> organizations,
            List<ClinicUnitData> clinicUnits,
            List<DemoProfileData> demoProfiles,
            List<DemoOrganizationData> operationalDemo) {
    }

    public record CountryData(String name, String code, Continent continent) {
    }

    public record SeedDefaults(
            String addressCountryCode,
            String patientNationalityCode,
            DocumentType identificationType,
            String identificationPrefix) {
    }

    public record AdministrativeDivisionData(String name, String code, String type, String countryCode) {
    }

    public record SpecialityData(String name, List<String> procedures) {
    }

    public record AddressData(
            String reference,
            String street,
            String district,
            String city,
            String additionalInfo,
            String block,
            String postalCode,
            String administrativeDivisionCode,
            String countryCode) {
    }

    public record PatientData(
            String name,
            LocalDate birthDate,
            boolean active,
            Gender gender,
            String taxId,
            String nationalityCode,
            String addressReference,
            List<String> specialityNames) {
    }

    public record OrganizationData(String name) {
    }

    public record ClinicUnitData(String organizationName, String name) {
    }

    public record DemoProfileData(
            String displayName, String email, String password, boolean platformAdministrator,
            @Nullable String organizationName, @Nullable String clinicUnitName,
            @Nullable MembershipRole membershipRole) {
    }

    public record DemoOrganizationData(String organizationName, String clinicUnitName, List<String> patientTaxIds,
            List<DemoPractitionerData> practitioners) {
    }

    record SeedAppointmentTimes(Instant scheduledStart, Instant scheduledEnd, Instant completedStart, Instant completedEnd) {
    }

    public record DemoPractitionerData(String displayName, String registrationNumber, List<String> specialityNames) {
    }
}
