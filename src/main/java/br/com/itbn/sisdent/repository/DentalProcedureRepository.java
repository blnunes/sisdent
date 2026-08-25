package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.CatalogStatus;
import br.com.itbn.sisdent.model.DentalProcedure;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DentalProcedureRepository extends JpaRepository<DentalProcedure, Long> {
    List<DentalProcedure> findAllByStatusOrderByNameAsc(CatalogStatus status);
}
