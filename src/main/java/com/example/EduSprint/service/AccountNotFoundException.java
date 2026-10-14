package com.example.EduSprint.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** The login has no account yet (call /account/create first). Answered as 404, not 500. */
public class AccountNotFoundException extends ResponseStatusException {
    public AccountNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Račun ne postoji");
    }
}
