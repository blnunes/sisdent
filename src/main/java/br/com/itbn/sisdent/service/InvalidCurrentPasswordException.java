package br.com.itbn.sisdent.service;

public class InvalidCurrentPasswordException extends RuntimeException {

    public InvalidCurrentPasswordException() {
        super("Current password is incorrect");
    }
}
