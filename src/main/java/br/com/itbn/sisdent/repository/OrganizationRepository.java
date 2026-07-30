package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByGlobalId(UUID globalId);
    Optional<Organization> findByName(String name);
}
