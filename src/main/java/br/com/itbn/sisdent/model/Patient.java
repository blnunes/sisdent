package br.com.itbn.sisdent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "patients")
public class Patient extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "global_id", nullable = false, unique = true, updatable = false)
    private UUID globalId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(length = 32)
    private String taxId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentType identificationType;

    @Column(nullable = false, length = 64)
    private String identificationNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_issuer_country_id", nullable = false)
    private Country documentIssuerCountry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nationality_country_id", nullable = false)
    private Country nationality;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "patient_specialities",
            joinColumns = @JoinColumn(name = "patient_id"),
            inverseJoinColumns = @JoinColumn(name = "speciality_id"))
    @OrderBy("name ASC")
    private Set<Speciality> specialities = new LinkedHashSet<>();

    protected Patient() {
    }

    public Patient(
            String name,
            LocalDate birthDate,
            boolean active,
            Gender gender,
            String taxId,
            DocumentType identificationType,
            String identificationNumber,
            Country documentIssuerCountry,
            Country nationality,
            Address address,
            Collection<Speciality> specialities) {
        this.name = name;
        this.birthDate = birthDate;
        this.active = active;
        this.gender = gender;
        this.taxId = taxId;
        this.identificationType = identificationType;
        this.identificationNumber = identificationNumber;
        this.documentIssuerCountry = documentIssuerCountry;
        this.nationality = nationality;
        this.address = address;
        this.specialities.addAll(specialities);
    }

    public Long getId() {
        return id;
    }

    public UUID getGlobalId() {
        return globalId;
    }

    public Person getPerson() {
        return person;
    }

    public String getName() {
        return name;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public boolean isActive() {
        return active;
    }

    public Gender getGender() {
        return gender;
    }

    public String getTaxId() {
        return taxId;
    }

    public DocumentType getIdentificationType() {
        return identificationType;
    }

    public String getIdentificationNumber() {
        return identificationNumber;
    }

    public Country getDocumentIssuerCountry() {
        return documentIssuerCountry;
    }

    public Country getNationality() {
        return nationality;
    }

    public Address getAddress() {
        return address;
    }

    public Set<Speciality> getSpecialities() {
        return Set.copyOf(specialities);
    }

    public void update(String name, LocalDate birthDate, boolean active, Gender gender, String taxId,
            DocumentType identificationType, String identificationNumber, Country documentIssuerCountry,
            Country nationality, Address address, Collection<Speciality> specialities) {
        this.name = name; this.birthDate = birthDate; this.active = active; this.gender = gender;
        this.taxId = taxId; this.identificationType = identificationType;
        this.identificationNumber = identificationNumber; this.documentIssuerCountry = documentIssuerCountry;
        this.nationality = nationality; this.address = address;
        this.specialities.clear(); this.specialities.addAll(specialities);
    }

    public void deactivate() { this.active = false; }
}
