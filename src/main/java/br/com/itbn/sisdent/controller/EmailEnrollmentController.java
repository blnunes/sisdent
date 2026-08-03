package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.EmailEnrollmentRequest;
import br.com.itbn.sisdent.dto.EmailEnrollmentResponse;
import br.com.itbn.sisdent.dto.EmailVerificationRequest;
import br.com.itbn.sisdent.dto.EmailVerificationResponse;
import br.com.itbn.sisdent.service.EmailEnrollmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EmailEnrollmentController {
    private final EmailEnrollmentService emailEnrollmentService;

    public EmailEnrollmentController(EmailEnrollmentService emailEnrollmentService) {
        this.emailEnrollmentService = emailEnrollmentService;
    }

    @PostMapping("/account/email-enrollment")
    public EmailEnrollmentResponse start(@Valid @RequestBody EmailEnrollmentRequest request) {
        return emailEnrollmentService.start(request.email());
    }

    @PostMapping("/account/email-enrollment/resend")
    public EmailEnrollmentResponse resend() {
        return emailEnrollmentService.resend();
    }

    @PostMapping("/auth/email-verification")
    public EmailVerificationResponse verify(@RequestBody EmailVerificationRequest request) {
        return emailEnrollmentService.verify(request.token());
    }
}
