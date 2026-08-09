package br.com.itbn.sisdent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "catalog_translations", uniqueConstraints = @UniqueConstraint(
        name = "uk_catalog_translation", columnNames = {"resource_type", "resource_id", "locale"}))
public class CatalogTranslation extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 32)
    private CatalogResourceType resourceType;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(nullable = false, length = 10)
    private String locale;

    @Column(name = "translated_name", nullable = false)
    private String translatedName;

    protected CatalogTranslation() {
    }

    public CatalogTranslation(CatalogResourceType resourceType, Long resourceId, String locale, String translatedName) {
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.locale = locale;
        this.translatedName = translatedName;
    }

    public void rename(String translatedName) {
        this.translatedName = translatedName;
    }

    public CatalogResourceType getResourceType() { return resourceType; }
    public Long getResourceId() { return resourceId; }
    public String getLocale() { return locale; }
    public String getTranslatedName() { return translatedName; }
}
