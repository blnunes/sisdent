package br.com.itbn.sisdent.mapper;

import br.com.itbn.sisdent.comum.MapperSisdent;
import br.com.itbn.sisdent.dto.PacienteDTO;
import br.com.itbn.sisdent.form.PacienteForm;
import br.com.itbn.sisdent.model.Paciente;

public class PacienteMapper implements MapperSisdent<Paciente, PacienteForm, PacienteDTO> {

    @Override
    public PacienteDTO formToDTO(PacienteForm body) {
        return PacienteDTO.builder()
                .cpf(body.getCpf())
                .dataNascimento(body.getDataNascimento())
                .nome(body.getNome())
                .situacao(body.isSituacao())
                .sexo(body.getSexo())
                .endereco(new EnderecoMapper().formToDTO(body.getEndereco()))
                .build();
    }

    @Override
    public PacienteDTO entityToDTO(Paciente paciente) {
        return PacienteDTO.builder()
                .id(paciente.getId())
                .cpf(paciente.getCpf())
                .dataNascimento(paciente.getDataNascimento())
                .nome(paciente.getNome())
                .sexo(paciente.getSexo())
                .situacao(paciente.isSituacao())
                .endereco(new EnderecoMapper().entityToDTO(paciente.getEndereco()))
                .build();
    }

    @Override
    public Paciente dtoToEntity(PacienteDTO paciente) {
        return Paciente.builder()
                .id(paciente.getId())
                .cpf(paciente.getCpf())
                .dataNascimento(paciente.getDataNascimento())
                .nome(paciente.getNome())
                .sexo(paciente.getSexo())
                .situacao(paciente.isSituacao())
                .endereco(new EnderecoMapper().dtoToEntity(paciente.getEndereco()))
                .build();
    }

}
