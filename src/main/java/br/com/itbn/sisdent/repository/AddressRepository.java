package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.Address;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    @Override
    @EntityGraph(attributePaths = "state")
    List<Address> findAll(Sort sort);

    @EntityGraph(attributePaths = "state")
    Optional<Address> findByPostalCode(String postalCode);
}
