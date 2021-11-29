package br.com.itbn.sisdent.model;

import br.com.itbn.sisdent.comum.EntitySisdent;
import lombok.*;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Date;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Endereco extends EntitySisdent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name="id_endrco")
    private Long id;

    @Column(name = "rua")
    private String rua;

    @Column(name = "bairro")
    private String bairro;

    @Column(name = "complemento")
    private String complemento;

    @Column(name = "quadra")
    private String quadra;

    @Column(name = "cep")
    private Integer cep;

    @ManyToOne(cascade = CascadeType.ALL)
    private Estado estado;
}
