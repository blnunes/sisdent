package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.comum.SisdentDTO;
import lombok.*;

import java.util.Date;


@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PacienteDTO extends SisdentDTO {

    private Long id;

    private String nome;

    private Date dataNascimento;

    private boolean situacao;

    private char sexo;

    private String cpf;

    private EnderecoDTO endereco;
}
