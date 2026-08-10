package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.CatalogResourceType;
import br.com.itbn.sisdent.model.CatalogTranslation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CatalogTranslationRepository extends JpaRepository<CatalogTranslation, Long> {
    Optional<CatalogTranslation> findByResourceTypeAndResourceIdAndLocale(
            CatalogResourceType resourceType, Long resourceId, String locale);
    List<CatalogTranslation> findByResourceTypeAndResourceId(CatalogResourceType resourceType, Long resourceId);
    List<CatalogTranslation> findByResourceType(CatalogResourceType resourceType);
}
