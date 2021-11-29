package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnderecoRepository extends JpaRepository<Endereco, Long> {
    Optional<Endereco> findByCep(Integer cep);
}
