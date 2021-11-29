package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {



}
