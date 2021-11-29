package br.com.itbn.sisdent.service;

import br.com.itbn.sisdent.comum.ServiceSisdent;
import br.com.itbn.sisdent.dto.EnderecoDTO;
import br.com.itbn.sisdent.mapper.EnderecoMapper;
import br.com.itbn.sisdent.model.Endereco;
import br.com.itbn.sisdent.model.Estado;
import br.com.itbn.sisdent.repository.EnderecoRepository;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EnderecoService implements ServiceSisdent<EnderecoDTO> {


    @Autowired
    private EnderecoRepository enderecoRepository;

    @Override
    public List<EnderecoDTO> findAll(){
        return enderecoRepository.findAll().stream().map(
                endereco -> new EnderecoMapper().entityToDTO(endereco)
        ).collect(Collectors.toList());
    }

    @Override
    public EnderecoDTO getOne(String id) throws Exception {
        return new EnderecoMapper().entityToDTO(
                enderecoRepository.findById(
                        Long.parseLong(id)).orElseThrow(() -> new NotFoundException("Not found")));
    }

    @Override
    public EnderecoDTO update(String id, EnderecoDTO dto) throws Exception {
        Endereco e = enderecoRepository.findById(Long.parseLong(id)).map(
                endereco -> getEndereco(dto, endereco)
        ).orElseThrow(() -> new NotFoundException("Not found"));
        return new EnderecoMapper().entityToDTO(enderecoRepository.saveAndFlush(e));
    }

    private Endereco getEndereco(EnderecoDTO dto, Endereco endereco) {
        endereco.setEstado(new Estado());
        endereco.setQuadra(dto.getQuadra());
        endereco.setComplemento(dto.getQuadra());
        endereco.setBairro(dto.getBairro());
        endereco.setCep(dto.getCep());
        endereco.setRua(dto.getRua());
        endereco.setId(dto.getId());
        return endereco;
    }

    @Override
    public EnderecoDTO create(EnderecoDTO dto) {
        return null;
    }

    @Override
    public void delete(String id) {

    }

    public EnderecoDTO createIfNotFound(EnderecoDTO endereco) {
        try {
            return update(endereco.getId().toString(), endereco);
        } catch (Exception ex) {
            System.out.println("Update Fail - Create "+ endereco.toString());
            return create(endereco);
        }
    }
}
