package br.com.itbn.sisdent.repository;
import br.com.itbn.sisdent.model.Practitioner; import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface PractitionerRepository extends JpaRepository<Practitioner,Long> {
 @EntityGraph(attributePaths={"organization","account","specialities"}) List<Practitioner> findAllByOrganization_GlobalIdOrderByDisplayName(UUID organizationId);
 @EntityGraph(attributePaths={"organization","account","specialities"}) Optional<Practitioner> findByGlobalIdAndOrganization_GlobalId(UUID id,UUID organizationId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from Practitioner p where p.globalId=:id and p.organization.globalId=:organizationId") Optional<Practitioner> lockByGlobalIdAndOrganization_GlobalId(UUID id,UUID organizationId);
}
