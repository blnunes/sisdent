package br.com.itbn.sisdent.model;

import java.util.EnumSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "app_users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_users_identification",
                columnNames = {"identification_type", "identification_number"}))
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "identification_type", nullable = false)
    private IdentificationType identificationType;

    @Column(name = "identification_number", nullable = false, length = 64)
    private String identificationNumber;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_permissions",
            joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "permission", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Permission> permissions = EnumSet.noneOf(Permission.class);

    @Column(nullable = false)
    private boolean active = true;

    protected User() {
    }

    public User(
            IdentificationType identificationType,
            String identificationNumber,
            String password,
            Role role,
            Set<Permission> permissions) {
        this.identificationType = identificationType;
        this.identificationNumber = identificationNumber;
        this.password = password;
        this.role = role;
        setPermissions(permissions);
    }

    public Long getId() {
        return id;
    }

    public IdentificationType getIdentificationType() {
        return identificationType;
    }

    public String getIdentificationNumber() {
        return identificationNumber;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public Set<Permission> getPermissions() {
        return Set.copyOf(permissions);
    }

    public boolean isActive() {
        return active;
    }

    public void update(
            IdentificationType identificationType,
            String identificationNumber,
            String encodedPassword,
            Role role) {
        this.identificationType = identificationType;
        this.identificationNumber = identificationNumber;
        if (encodedPassword != null) {
            this.password = encodedPassword;
        }
        if (this.role != role) {
            this.role = role;
            setPermissions(role.defaultPermissions());
        }
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions.clear();
        if (role == Role.ADMIN) {
            this.permissions.addAll(Role.ADMIN.defaultPermissions());
            return;
        }
        this.permissions.addAll(permissions);
    }

    public void deactivate() {
        this.active = false;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
