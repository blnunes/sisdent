package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.Speciality;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialityRepository extends JpaRepository<Speciality, Long> {
}
