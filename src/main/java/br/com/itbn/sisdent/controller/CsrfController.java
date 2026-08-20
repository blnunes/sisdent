package br.com.itbn.sisdent.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Supplies the anti-CSRF token to the single-page application without exposing its session cookie. */
@RestController
class CsrfController {

    @GetMapping("/api/csrf")
    CsrfToken token(CsrfToken token) {
        return token;
    }
}
