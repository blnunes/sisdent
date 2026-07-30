package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.service.EmailDeliveryUnavailableException;
import br.com.itbn.sisdent.service.EmailEnrollmentThrottledException;
import br.com.itbn.sisdent.service.EmailEnrollmentUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class EmailEnrollmentProblemHandler {

    @ExceptionHandler(EmailEnrollmentUnavailableException.class)
    ProblemDetail handleUnavailable() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "Email enrollment could not be started.");
        problem.setTitle("Email enrollment unavailable");
        return problem;
    }

    @ExceptionHandler(EmailDeliveryUnavailableException.class)
    ProblemDetail handleDeliveryUnavailable() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Email enrollment is temporarily unavailable.");
        problem.setTitle("Verification delivery unavailable");
        return problem;
    }

    @ExceptionHandler(EmailEnrollmentThrottledException.class)
    ResponseEntity<ProblemDetail> handleThrottled(EmailEnrollmentThrottledException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS, exception.getMessage());
        problem.setTitle("Verification request throttled");
        problem.setProperty("retryAfterSeconds", exception.getRetryAfterSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", Long.toString(exception.getRetryAfterSeconds()))
                .body(problem);
    }
}
