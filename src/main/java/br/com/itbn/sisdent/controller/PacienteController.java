package br.com.itbn.sisdent.controller;

import br.com.itbn.sisdent.dto.EnderecoDTO;
import br.com.itbn.sisdent.dto.EstadoDTO;
import br.com.itbn.sisdent.dto.PacienteDTO;
import br.com.itbn.sisdent.form.EnderecoForm;
import br.com.itbn.sisdent.form.PacienteForm;
import br.com.itbn.sisdent.mapper.EnderecoMapper;
import br.com.itbn.sisdent.mapper.EstadoMapper;
import br.com.itbn.sisdent.mapper.PacienteMapper;
import br.com.itbn.sisdent.model.Endereco;
import br.com.itbn.sisdent.model.Estado;
import br.com.itbn.sisdent.model.Paciente;
import br.com.itbn.sisdent.repository.EnderecoRepository;
import br.com.itbn.sisdent.repository.EstadoRepository;
import br.com.itbn.sisdent.repository.PacienteRepository;
import br.com.itbn.sisdent.service.PacienteService;
import com.sun.istack.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/pacientes", consumes = "application/json", produces = "application/json")
public class PacienteController {

    @Autowired
    private PacienteService pacienteService;

    @GetMapping
    public ResponseEntity<List<PacienteDTO>> listar() {
        List<PacienteDTO> dto = pacienteService.findAll();
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public PacienteDTO cadastrar(@RequestBody @NotNull PacienteForm body) {
        return pacienteService.create(new PacienteMapper().formToDTO(body));
    }

//    private Endereco cadastraEndereco(EnderecoForm endereco){
//        Optional<Endereco> e = enderecoRepository.findByCep(endereco.getCep());
//        if(e.isPresent()){
//            return e.get();
//        } else {
//            enderecoRepository.save()
//        }
//    }
//
//    private Estado cadastraEstado(EstadoDTO estado) {
//        Optional<Estado> e = estadoRepository.findByUf(estado.getUf());
//        if(e.isPresent()){
//            return e.get();
//        } else{
//            Estado novoEstado = new Estado();
//            novoEstado.setNome(estado.getNome());
//            novoEstado.setUf(estado.getUf());
//            return novoEstado;
//        }
//    }
//
//    @PutMapping("/{id}")
//    @Transactional
//    public ResponseEntity<Paciente> alterar(@PathVariable Long id, @RequestBody PacienteForm body) {
//        Optional<Paciente> p = pacienteRepository.findById(id);
//        if(p.isPresent()){
//            Paciente paciente = pacienteRepository.getById(id);
//            paciente.setNome(body.getNome());
//            paciente.setCpf(body.getCpf());
//            return ResponseEntity.ok(paciente);
//        }
//        return ResponseEntity.notFound().build();
//    }
}
