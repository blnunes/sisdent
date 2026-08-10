package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.model.Continent;

public record CountryResponse(Long id, String name, String displayName, String code, Continent continent) {
}
