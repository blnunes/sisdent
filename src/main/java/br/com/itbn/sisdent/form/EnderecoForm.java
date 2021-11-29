package br.com.itbn.sisdent.form;

import br.com.itbn.sisdent.comum.FormSisdent;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnderecoForm extends FormSisdent {

    private Long id;

    private String rua;

    private String bairro;

    private String complemento;

    private String quadra;

    private Integer cep;

    private EstadoForm estado;


}
