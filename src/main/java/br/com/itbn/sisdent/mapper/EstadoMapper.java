package br.com.itbn.sisdent.mapper;

import br.com.itbn.sisdent.comum.MapperSisdent;
import br.com.itbn.sisdent.dto.EstadoDTO;
import br.com.itbn.sisdent.form.EstadoForm;
import br.com.itbn.sisdent.model.Estado;

public class EstadoMapper implements MapperSisdent<Estado, EstadoForm, EstadoDTO> {

    @Override
    public EstadoDTO formToDTO(EstadoForm body) {
        return EstadoDTO.builder()
                .id(body.getId())
                .nome(body.getNome())
                .uf(body.getUf())
                .build();
    }

    @Override
    public EstadoDTO entityToDTO(Estado estado) {
        return EstadoDTO.builder()
                .id(estado.getId())
                .nome(estado.getNome())
                .uf(estado.getUf())
                .build();
    }

    @Override
    public Estado dtoToEntity(EstadoDTO estado) {
        return Estado.builder()
                .id(estado.getId())
                .nome(estado.getNome())
                .uf(estado.getUf())
                .build();
    }
}
