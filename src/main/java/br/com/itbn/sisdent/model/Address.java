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

@Entity
@Table(name = "addresses")
public class Address extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String street;

    @Column
    private String district;

    @Column(nullable = false)
    private String city;

    private String additionalInfo;

    private String block;

    @Column(nullable = false, length = 20)
    private String postalCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrative_division_id")
    private AdministrativeDivision administrativeDivision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    protected Address() {
    }

    public Address(
            String street,
            String district,
            String city,
            String additionalInfo,
            String block,
            String postalCode,
            AdministrativeDivision administrativeDivision,
            Country country) {
        this.street = street;
        this.district = district;
        this.city = city;
        this.additionalInfo = additionalInfo;
        this.block = block;
        this.postalCode = postalCode;
        this.administrativeDivision = administrativeDivision;
        this.country = country;
    }

    public Long getId() {
        return id;
    }

    public String getStreet() {
        return street;
    }

    public String getDistrict() {
        return district;
    }

    public String getCity() {
        return city;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public String getBlock() {
        return block;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public AdministrativeDivision getAdministrativeDivision() {
        return administrativeDivision;
    }

    public Country getCountry() {
        return country;
    }

    public void update(String street, String district, String city, String additionalInfo, String block,
            String postalCode, AdministrativeDivision administrativeDivision, Country country) {
        this.street = street; this.district = district; this.city = city; this.additionalInfo = additionalInfo;
        this.block = block; this.postalCode = postalCode;
        this.administrativeDivision = administrativeDivision; this.country = country;
    }
}
