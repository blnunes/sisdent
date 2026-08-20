package br.com.itbn.sisdent.repository;
import br.com.itbn.sisdent.model.ClinicUnitWorkingHours; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface ClinicUnitWorkingHoursRepository extends JpaRepository<ClinicUnitWorkingHours,Long>{ List<ClinicUnitWorkingHours> findAllByClinicUnit_Id(int clinicUnitId); List<ClinicUnitWorkingHours> findAllByClinicUnit_Id(Long clinicUnitId); }
