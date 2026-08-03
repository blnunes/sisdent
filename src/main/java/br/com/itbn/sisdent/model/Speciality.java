package br.com.itbn.sisdent.model;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "specialities")
public class Speciality extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CatalogStatus status = CatalogStatus.ACTIVE;

    @OneToMany(
            mappedBy = "speciality",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY)
    private Set<DentalProcedure> procedures = new LinkedHashSet<>();

    protected Speciality() {
    }

    public Speciality(String name) {
        this.name = name;
    }

    public Speciality(String name, Collection<String> procedureNames) {
        this.name = name;
        procedureNames.forEach(this::addProcedure);
    }

    public void rename(String name) {
        this.name = name;
    }

    public DentalProcedure addProcedure(String name) {
        Optional<DentalProcedure> existingProcedure = procedures.stream()
                .filter(procedure -> procedure.getName().equalsIgnoreCase(name))
                .findFirst();
        if (existingProcedure.isPresent()) {
            DentalProcedure procedure = existingProcedure.get();
            procedure.rename(name);
            procedure.activate();
            return procedure;
        }
        DentalProcedure procedure = new DentalProcedure(name, this);
        procedures.add(procedure);
        return procedure;
    }

    public Optional<DentalProcedure> findProcedure(Long procedureId) {
        return procedures.stream()
                .filter(procedure -> procedureId.equals(procedure.getId()))
                .findFirst();
    }

    public void retainProcedures(Set<Long> procedureIds) {
        procedures.stream()
                .filter(procedure -> procedure.getId() != null && !procedureIds.contains(procedure.getId()))
                .forEach(DentalProcedure::deactivate);
    }

    public void addMissingProcedures(Collection<String> procedureNames) {
        Set<String> existingNames = procedures.stream()
                .filter(procedure -> procedure.getStatus() == CatalogStatus.ACTIVE)
                .map(DentalProcedure::getName)
                .collect(Collectors.toSet());
        procedureNames.stream()
                .filter(name -> !existingNames.contains(name))
                .forEach(this::addProcedure);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CatalogStatus getStatus() {
        return status;
    }

    public void activate() {
        this.status = CatalogStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = CatalogStatus.INACTIVE;
        procedures.forEach(DentalProcedure::deactivate);
    }

    public Set<DentalProcedure> getProcedures() {
        return Set.copyOf(procedures);
    }
}
