package br.com.itbn.sisdent.repository;

import br.com.itbn.sisdent.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
}
