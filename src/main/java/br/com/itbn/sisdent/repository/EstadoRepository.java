package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.Estado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoRepository extends JpaRepository<Estado, Long> {
    Optional<Estado> findByUf(String uf);
}
