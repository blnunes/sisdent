package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ApplicationException;
import br.com.itbn.sisdent.error.ErrorCategory;
import java.util.Map;

/** A country code supplied by a client was not found in the catalogue. */
public class UnknownCountryException extends ApplicationException {

    public UnknownCountryException(String code) {
        super(ErrorCode.CATALOG_UNKNOWN_COUNTRY, ErrorCategory.RESOURCE_NOT_FOUND, Map.of(), null);
    }
}
