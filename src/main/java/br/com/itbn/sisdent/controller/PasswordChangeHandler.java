package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.service.InvalidCurrentPasswordException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PasswordChangeHandler {

    @ExceptionHandler(InvalidCurrentPasswordException.class)
    ProblemDetail handleInvalidCurrentPassword(InvalidCurrentPasswordException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage());
        problem.setTitle("Password change failed");
        return problem;
    }
}
