package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.SessionResponse;
import br.com.itbn.sisdent.service.SessionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session")
public class SessionController {
    private final SessionService sessionService;
    public SessionController(SessionService sessionService) { this.sessionService = sessionService; }
    @GetMapping
    public SessionResponse current() { return sessionService.current(); }
}
