package br.com.itbn.sisdent.repository;
import br.com.itbn.sisdent.model.ClinicUnitBreak; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface ClinicUnitBreakRepository extends JpaRepository<ClinicUnitBreak,Long>{ List<ClinicUnitBreak> findAllByClinicUnit_Id(Long clinicUnitId); }
