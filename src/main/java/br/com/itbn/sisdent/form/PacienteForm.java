package br.com.itbn.sisdent.form;

import br.com.itbn.sisdent.comum.FormSisdent;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PacienteForm extends FormSisdent {

    String cpf;
    Date dataNascimento;
    String nome;
    Character sexo;
    boolean situacao;
    EnderecoForm endereco;


}
