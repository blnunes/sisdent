package br.com.itbn.sisdent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "organizations")
public class Organization extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "global_id", nullable = false, unique = true, updatable = false)
    private UUID globalId = UUID.randomUUID();
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private boolean active = true;

    protected Organization() {}
    public Organization(String name) { this.name = name.trim(); }
    public Long getId() { return id; }
    public UUID getGlobalId() { return globalId; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}
