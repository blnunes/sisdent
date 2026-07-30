package br.com.itbn.sisdent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "administrative_divisions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_states_country_abbreviation",
                columnNames = {"country_id", "abbreviation"}))
public class AdministrativeDivision extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "abbreviation", nullable = false, length = 32)
    private String code;

    @Column(name = "division_type", nullable = false, length = 32)
    private String type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    protected AdministrativeDivision() {
    }

    public AdministrativeDivision(String name, String code, String type, Country country) {
        this.name = name;
        this.code = code;
        this.type = type;
        this.country = country;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getType() {
        return type;
    }

    public Country getCountry() {
        return country;
    }

    public void update(String name, String code, String type, Country country) {
        this.name = name;
        this.code = code;
        this.type = type;
        this.country = country;
    }
}
