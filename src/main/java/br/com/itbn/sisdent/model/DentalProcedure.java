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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "dental_procedures",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_procedure_speciality_name",
                columnNames = {"speciality_id", "name"}))
public class DentalProcedure extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "speciality_id", nullable = false)
    private Speciality speciality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CatalogStatus status = CatalogStatus.ACTIVE;

    protected DentalProcedure() {
    }

    DentalProcedure(String name, Speciality speciality) {
        this.name = name;
        this.speciality = speciality;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void activate() {
        this.status = CatalogStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = CatalogStatus.INACTIVE;
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

    public Speciality getSpeciality() {
        return speciality;
    }
}
