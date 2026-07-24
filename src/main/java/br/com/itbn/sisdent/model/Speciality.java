package br.com.itbn.sisdent.model;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
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
public class Speciality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(
            mappedBy = "speciality",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Set<Procedure> procedures = new LinkedHashSet<>();

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

    public Procedure addProcedure(String name) {
        Procedure procedure = new Procedure(name, this);
        procedures.add(procedure);
        return procedure;
    }

    public Optional<Procedure> findProcedure(Long procedureId) {
        return procedures.stream()
                .filter(procedure -> procedureId.equals(procedure.getId()))
                .findFirst();
    }

    public void retainProcedures(Set<Long> procedureIds) {
        procedures.removeIf(procedure ->
                procedure.getId() != null && !procedureIds.contains(procedure.getId()));
    }

    public void addMissingProcedures(Collection<String> procedureNames) {
        Set<String> existingNames = procedures.stream()
                .map(Procedure::getName)
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

    public Set<Procedure> getProcedures() {
        return Set.copyOf(procedures);
    }
}
