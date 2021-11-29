package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.comum.SisdentDTO;
import com.sun.istack.NotNull;
import lombok.*;

import java.util.Date;


@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PacienteDTO extends SisdentDTO {

    @NotNull
    private Long id;

    @NotNull
    private String nome;

    @NotNull
    private Date dataNascimento;

    @NotNull
    private boolean situacao;

    @NotNull
    private char sexo;

    @NotNull
    private String cpf;

    private EnderecoDTO endereco;
}
