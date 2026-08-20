package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.error.ApplicationException;
import br.com.itbn.sisdent.error.ErrorCategory;
import br.com.itbn.sisdent.error.ErrorCode;
import java.util.Map;

/** A transport-neutral scheduling conflict. */
public class SchedulingConflictException extends ApplicationException {
    public SchedulingConflictException() {
        super(ErrorCode.SCHEDULING_PRACTITIONER_UNAVAILABLE, ErrorCategory.CONFLICT, Map.of(), null);
    }
}
