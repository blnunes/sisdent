package br.com.itbn.sisdent.form;

import br.com.itbn.sisdent.comum.FormSisdent;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EstadoForm extends FormSisdent {

    private Long id;

    private String nome;

    private String uf;

}
