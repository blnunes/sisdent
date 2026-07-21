package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.model.Message;
import br.com.itbn.sisdent.repository.MessageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageRepository repository;

    public MessageController(MessageRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> hello() {
        return repository.findById(1L)
                .map(Message::getText)
                .map(text -> ResponseEntity.ok(Map.of("message", text)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
