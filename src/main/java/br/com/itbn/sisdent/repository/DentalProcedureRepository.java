package br.com.itbn.sisdent.repository;
import br.com.itbn.sisdent.model.DentalProcedure; import org.springframework.data.jpa.repository.JpaRepository;
public interface DentalProcedureRepository extends JpaRepository<DentalProcedure,Long> {}
