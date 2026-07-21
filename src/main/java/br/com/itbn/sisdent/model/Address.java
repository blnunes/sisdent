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
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String district;

    private String additionalInfo;

    private String block;

    @Column(nullable = false, unique = true, length = 8)
    private String postalCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "state_id", nullable = false)
    private State state;

    protected Address() {
    }

    public Address(
            String street,
            String district,
            String additionalInfo,
            String block,
            String postalCode,
            State state) {
        this.street = street;
        this.district = district;
        this.additionalInfo = additionalInfo;
        this.block = block;
        this.postalCode = postalCode;
        this.state = state;
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

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public String getBlock() {
        return block;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public State getState() {
        return state;
    }
}
