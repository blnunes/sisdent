package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.IdentificationType;
import br.com.itbn.sisdent.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "permissions")
    Optional<User> findByIdentificationTypeAndIdentificationNumber(
            IdentificationType identificationType,
            String identificationNumber);

    @EntityGraph(attributePaths = "permissions")
    List<User> findAllByActiveTrueOrderByIdentificationNumber();

    @EntityGraph(attributePaths = "permissions")
    Page<User> findAllByActiveTrue(Pageable pageable);

    @EntityGraph(attributePaths = "permissions")
    Optional<User> findByIdAndActiveTrue(Long id);
}
