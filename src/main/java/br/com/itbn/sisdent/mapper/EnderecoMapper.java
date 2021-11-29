package br.com.itbn.sisdent.mapper;

import br.com.itbn.sisdent.comum.MapperSisdent;
import br.com.itbn.sisdent.dto.EnderecoDTO;
import br.com.itbn.sisdent.form.EnderecoForm;
import br.com.itbn.sisdent.model.Endereco;

public class EnderecoMapper implements MapperSisdent<Endereco, EnderecoForm, EnderecoDTO> {

    @Override
    public EnderecoDTO formToDTO(EnderecoForm body) {
        return EnderecoDTO.builder()
                .id(body.getId())
                .bairro(body.getBairro())
                .cep(body.getCep())
                .complemento(body.getComplemento())
                .quadra(body.getQuadra())
                .rua(body.getRua())
                .estado(new EstadoMapper().formToDTO(body.getEstado()))
                .build();
    }

    @Override
    public EnderecoDTO entityToDTO(Endereco endereco) {
        return EnderecoDTO.builder()
                .id(endereco.getId())
                .complemento(endereco.getComplemento())
                .bairro(endereco.getBairro())
                .quadra(endereco.getQuadra())
                .rua(endereco.getRua())
                .cep(endereco.getCep())
                .estado(new EstadoMapper().entityToDTO(endereco.getEstado()))
                .build();
    }

    @Override
    public Endereco dtoToEntity(EnderecoDTO endereco) {
        return Endereco.builder()
                .id(endereco.getId())
                .complemento(endereco.getComplemento())
                .bairro(endereco.getBairro())
                .quadra(endereco.getQuadra())
                .rua(endereco.getRua())
                .cep(endereco.getCep())
                .estado(new EstadoMapper().dtoToEntity(endereco.getEstado()))
                .build();
    }
}
