package br.com.itbn.sisdent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "persons")
public class Person extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "global_id", nullable = false, unique = true, updatable = false)
    private UUID globalId = UUID.randomUUID();

    @Column(name = "display_name", nullable = false)
    private String displayName;

    protected Person() {
    }

    public Person(String displayName) {
        this.displayName = displayName.trim();
    }

    public Long getId() { return id; }
    public UUID getGlobalId() { return globalId; }
    public String getDisplayName() { return displayName; }
}
