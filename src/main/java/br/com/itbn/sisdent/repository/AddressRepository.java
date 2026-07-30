package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.Address;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    @Override
    @EntityGraph(attributePaths = {"state", "country"})
    List<Address> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = {"state", "country"})
    Page<Address> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"state", "country"})
    Optional<Address> findByPostalCode(String postalCode);
}
