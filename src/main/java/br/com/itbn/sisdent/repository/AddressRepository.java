package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.Address;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {

    @Override
    @EntityGraph(attributePaths = {"administrativeDivision", "administrativeDivision.country", "country"})
    List<Address> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = {"administrativeDivision", "administrativeDivision.country", "country"})
    Page<Address> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"administrativeDivision", "administrativeDivision.country", "country"})
    List<Address> findAllByCountry_CodeAndPostalCodeOrderByStreet(String countryCode, String postalCode);
}
