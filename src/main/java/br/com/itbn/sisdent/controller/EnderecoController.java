package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.EnderecoDTO;
import br.com.itbn.sisdent.dto.EstadoDTO;
import br.com.itbn.sisdent.dto.PacienteDTO;
import br.com.itbn.sisdent.form.EnderecoForm;
import br.com.itbn.sisdent.form.PacienteForm;
import br.com.itbn.sisdent.model.Endereco;
import br.com.itbn.sisdent.model.Estado;
import br.com.itbn.sisdent.model.Paciente;
import br.com.itbn.sisdent.repository.EnderecoRepository;
import br.com.itbn.sisdent.repository.EstadoRepository;
import br.com.itbn.sisdent.repository.PacienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/enderecos", consumes = "application/json", produces = "application/json")
public class EnderecoController {

    @Autowired
    private EnderecoRepository enderecoRepository;



    @GetMapping
    public ResponseEntity<List<EnderecoDTO>> listar() {
        List<EnderecoDTO> dto = enderecoRepository.findAll().stream().map(this::mapperEnderecoDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/cep/{cep}")
    public ResponseEntity<Endereco> findByCep(@PathVariable Integer cep) {
        Optional<Endereco> endereco = enderecoRepository.findByCep(cep);
        return endereco.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }



    private EnderecoDTO mapperEnderecoDTO(Endereco endereco) {
        return EnderecoDTO.builder()
                .id(endereco.getId())
                .complemento(endereco.getComplemento())
                .bairro(endereco.getBairro())
                .quadra(endereco.getQuadra())
                .rua(endereco.getRua())
                .cep(endereco.getCep())
                .estado(mapperEstadoDTO(endereco.getEstado()))
                .build();
    }

    private EstadoDTO mapperEstadoDTO(Estado estado) {
        return EstadoDTO.builder()
                .id(estado.getId())
                .nome(estado.getNome())
                .uf(estado.getUf())
                .build();
    }

}
