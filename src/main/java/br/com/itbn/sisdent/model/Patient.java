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

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false, unique = true, length = 11)
    private String taxId;

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
            Address address,
            Collection<Speciality> specialities) {
        this.name = name;
        this.birthDate = birthDate;
        this.active = active;
        this.gender = gender;
        this.taxId = taxId;
        this.address = address;
        this.specialities.addAll(specialities);
    }

    public Long getId() {
        return id;
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

    public Address getAddress() {
        return address;
    }

    public Set<Speciality> getSpecialities() {
        return Set.copyOf(specialities);
    }
}
