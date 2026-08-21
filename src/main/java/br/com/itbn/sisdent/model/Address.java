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

    public Address(Builder builder) {
        street = builder.street;
        district = builder.district;
        city = builder.city;
        additionalInfo = builder.additionalInfo;
        block = builder.block;
        postalCode = builder.postalCode;
        administrativeDivision = builder.administrativeDivision;
        country = builder.country;
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

    public static Builder builder() {
        return new Builder();
    }

    public void update(Address source) {
        street = source.street; district = source.district; city = source.city; additionalInfo = source.additionalInfo;
        block = source.block; postalCode = source.postalCode;
        administrativeDivision = source.administrativeDivision; country = source.country;
    }

    public static final class Builder {
        private String street;
        private String district;
        private String city;
        private String additionalInfo;
        private String block;
        private String postalCode;
        private AdministrativeDivision administrativeDivision;
        private Country country;

        public Builder street(String value) { street = value; return this; }
        public Builder district(String value) { district = value; return this; }
        public Builder city(String value) { city = value; return this; }
        public Builder additionalInfo(String value) { additionalInfo = value; return this; }
        public Builder block(String value) { block = value; return this; }
        public Builder postalCode(String value) { postalCode = value; return this; }
        public Builder administrativeDivision(AdministrativeDivision value) { administrativeDivision = value; return this; }
        public Builder country(Country value) { country = value; return this; }
    }
}
