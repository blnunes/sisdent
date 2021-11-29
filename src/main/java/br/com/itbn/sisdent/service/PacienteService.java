package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.comum.ServiceSisdent;
import br.com.itbn.sisdent.dto.PacienteDTO;
import br.com.itbn.sisdent.mapper.EnderecoMapper;
import br.com.itbn.sisdent.mapper.PacienteMapper;
import br.com.itbn.sisdent.model.Paciente;
import br.com.itbn.sisdent.repository.PacienteRepository;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PacienteService implements ServiceSisdent<PacienteDTO> {

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private EnderecoService enderecoService;

    @Override
    public List<PacienteDTO> findAll(){
        return pacienteRepository.findAll().stream().map(
                paciente -> new PacienteMapper().entityToDTO(paciente)
        ).collect(Collectors.toList());
    }

    @Override
    public PacienteDTO getOne(String id) throws Exception {
        return new PacienteMapper().entityToDTO(
                pacienteRepository.findById(Long.parseLong(id)).orElseThrow(() -> new NotFoundException("Not found")));
    }


    @Override
    public PacienteDTO update(String id, PacienteDTO dto) throws Exception {
        Paciente p = pacienteRepository.findById(Long.parseLong(id)).map(
                paciente -> getPaciente(dto, paciente)
        ).orElseThrow(() -> new NotFoundException("Not found"));
        pacienteRepository.saveAndFlush(p);
        return null;
    }

    private Paciente getPaciente(PacienteDTO dto, Paciente paciente) {
        paciente.setCpf(dto.getCpf());
        paciente.setNome(dto.getNome());
        paciente.setEndereco(
                new EnderecoMapper().dtoToEntity(
                        enderecoService.createIfNotFound(dto.getEndereco())));
        paciente.setSexo(dto.getSexo());
        paciente.setDataNascimento(dto.getDataNascimento());
        paciente.setSituacao(dto.isSituacao());
        return paciente;
    }


    @Override
    public PacienteDTO create(PacienteDTO dto) {
        System.out.println(new PacienteMapper().dtoToEntity(dto));
        return new PacienteMapper().entityToDTO(
                pacienteRepository.save(new PacienteMapper().dtoToEntity(dto)));
    }

    @Override
    public void delete(String id) {

    }
}
