package br.com.itbn.sisdent.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnknownCountryException extends RuntimeException {

    public UnknownCountryException(String code) {
        super("Unknown country code: " + code);
    }
}
