package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.comum.SisdentDTO;
import lombok.*;


@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EstadoDTO extends SisdentDTO {

    private Long id;

    private String nome;

    private String uf;
}
