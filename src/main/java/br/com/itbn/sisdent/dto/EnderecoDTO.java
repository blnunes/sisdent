package br.com.itbn.sisdent.dto;

import br.com.itbn.sisdent.comum.SisdentDTO;
import br.com.itbn.sisdent.model.Estado;
import lombok.*;

import javax.persistence.*;
import java.util.Date;


@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EnderecoDTO extends SisdentDTO {

    private Long id;

    private String rua;

    private String bairro;

    private String complemento;

    private String quadra;

    private Integer cep;

    private EstadoDTO estado;
}
