package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.service.SessionService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ControllerDelegationTest {
    @Test
    void delegatesTheCurrentSessionEndpoint() {
        SessionService sessions = mock(SessionService.class);
        new SessionController(sessions).current();
        verify(sessions).current();
    }
}
