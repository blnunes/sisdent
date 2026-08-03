package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {
}
