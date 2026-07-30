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
import br.com.itbn.sisdent.model.AccountEmailClaim;
import br.com.itbn.sisdent.model.EmailClaimType;
import br.com.itbn.sisdent.model.ClinicUnit;
import br.com.itbn.sisdent.model.Membership;
import br.com.itbn.sisdent.model.MembershipRole;
import br.com.itbn.sisdent.model.Organization;
import br.com.itbn.sisdent.model.Person;
import br.com.itbn.sisdent.repository.AccountEmailClaimRepository;
import br.com.itbn.sisdent.repository.AccountRepository;
import br.com.itbn.sisdent.repository.ClinicUnitRepository;
import br.com.itbn.sisdent.repository.MembershipRepository;
import br.com.itbn.sisdent.repository.OrganizationRepository;
import br.com.itbn.sisdent.repository.PersonRepository;
import br.com.itbn.sisdent.repository.AdministrativeDivisionRepository;
import br.com.itbn.sisdent.repository.AddressRepository;
import br.com.itbn.sisdent.repository.CountryRepository;
import br.com.itbn.sisdent.repository.PatientRepository;
import br.com.itbn.sisdent.repository.SpecialityRepository;
import org.jspecify.annotations.NullMarked;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Order(1)
@NullMarked
public class InitialDataLoader implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitialDataLoader.class);
    private static final String INITIAL_DATA_PATH = "data/initial-data.json";

    private final JsonMapper jsonMapper;
    private final CountryRepository countryRepository;
    private final AdministrativeDivisionRepository administrativeDivisionRepository;
    private final SpecialityRepository specialityRepository;
    private final AddressRepository addressRepository;
    private final PatientRepository patientRepository;
    private final OrganizationRepository organizationRepository;
    private final ClinicUnitRepository clinicUnitRepository;
    private final AccountRepository accountRepository;
    private final AccountEmailClaimRepository emailClaimRepository;
    private final PersonRepository personRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    public InitialDataLoader(
            JsonMapper jsonMapper,
            CountryRepository countryRepository,
            AdministrativeDivisionRepository administrativeDivisionRepository,
            SpecialityRepository specialityRepository,
            AddressRepository addressRepository,
            PatientRepository patientRepository, OrganizationRepository organizationRepository,
            ClinicUnitRepository clinicUnitRepository, AccountRepository accountRepository,
            AccountEmailClaimRepository emailClaimRepository, PersonRepository personRepository,
            MembershipRepository membershipRepository, PasswordEncoder passwordEncoder) {
        this.jsonMapper = jsonMapper;
        this.countryRepository = countryRepository;
        this.administrativeDivisionRepository = administrativeDivisionRepository;
        this.specialityRepository = specialityRepository;
        this.addressRepository = addressRepository;
        this.patientRepository = patientRepository;
        this.organizationRepository = organizationRepository; this.clinicUnitRepository = clinicUnitRepository;
        this.accountRepository = accountRepository; this.emailClaimRepository = emailClaimRepository;
        this.personRepository = personRepository; this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
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
        saveDemoProfiles(initialData.demoProfiles());

        LOGGER.info(
                "Initial data synchronized from {}: {} countries, {} administrative divisions, {} specialities, {} addresses and {} patients",
                INITIAL_DATA_PATH,
                initialData.countries().size(),
                initialData.administrativeDivisions().size(),
                initialData.specialities().size(),
                initialData.addresses().size(),
                initialData.patients().size());
    }

    private void saveDemoProfiles(List<DemoProfileData> profiles) {
        for (DemoProfileData profile : profiles) {
            Account account = accountRepository.findByEmail(Account.normalizeEmail(profile.email())).orElseGet(() -> {
                Person person = personRepository.save(new Person(profile.displayName()));
                Account created = accountRepository.save(new Account(person, null, profile.email(),
                        passwordEncoder.encode(profile.password()), profile.platformAdministrator(), false));
                emailClaimRepository.save(new AccountEmailClaim(created, profile.email(), EmailClaimType.VERIFIED));
                return created;
            });
            if (profile.organizationName() == null) continue;
            Organization organization = organizationRepository.findByName(profile.organizationName())
                    .orElseGet(() -> organizationRepository.save(new Organization(profile.organizationName())));
            ClinicUnit clinic = profile.clinicUnitName() == null ? null : clinicUnitRepository
                    .findByOrganization_IdAndName(organization.getId(), profile.clinicUnitName())
                    .orElseGet(() -> clinicUnitRepository.save(new ClinicUnit(organization, profile.clinicUnitName())));
            boolean exists = clinic == null
                    ? membershipRepository.existsByAccount_IdAndOrganization_IdAndClinicUnitIsNull(account.getId(), organization.getId())
                    : membershipRepository.existsByAccount_IdAndOrganization_IdAndClinicUnit_Id(account.getId(), organization.getId(), clinic.getId());
            if (!exists) membershipRepository.save(new Membership(account, organization, clinic, profile.membershipRole()));
        }
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
            String countryCode) {
        Country country = requireReference(countriesByCode, countryCode, "country code");
        List<AdministrativeDivision> savedDivisions = divisions.stream()
                .map(division -> administrativeDivisionRepository
                        .findByCountry_CodeAndCode(countryCode, division.code())
                        .orElseGet(() -> administrativeDivisionRepository.save(
                                new AdministrativeDivision(
                                        division.name(),
                                        division.code(),
                                        division.type(),
                                        country))))
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
            String addressCountryCode) {
        return addresses.stream()
                .collect(Collectors.toMap(
                        AddressData::reference,
                        address -> addressRepository
                                .findAllByCountry_CodeAndPostalCodeOrderByStreet(
                                        addressCountryCode,
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
                                        requireReference(
                                                divisionsByCode,
                                                address.administrativeDivisionCode(),
                                                "administrative division code"),
                                        requireReference(
                                                countriesByCode,
                                                addressCountryCode,
                                                "country code"))))));
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
                                seedDefaults.patientNationalityCode(),
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

    public record InitialData(
            List<CountryData> countries,
            SeedDefaults seedDefaults,
            List<AdministrativeDivisionData> administrativeDivisions,
            List<SpecialityData> specialities,
            List<AddressData> addresses,
            List<PatientData> patients,
            List<DemoProfileData> demoProfiles) {
    }

    public record CountryData(String name, String code, Continent continent) {
    }

    public record SeedDefaults(
            String addressCountryCode,
            String patientNationalityCode,
            DocumentType identificationType,
            String identificationPrefix) {
    }

    public record AdministrativeDivisionData(String name, String code, String type) {
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
            String administrativeDivisionCode) {
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

    public record DemoProfileData(
            String displayName, String email, String password, boolean platformAdministrator,
            String organizationName, String clinicUnitName, MembershipRole membershipRole) {
    }
}
