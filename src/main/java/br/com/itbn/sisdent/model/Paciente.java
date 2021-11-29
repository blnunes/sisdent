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
public class Paciente extends EntitySisdent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name="id_pcnte")
    private Long id;

    @Column(name = "nme_pcnte")
    private String nome;

    @Type(type = "date")
    @Column(name="dt_nscmto")
    private Date dataNascimento;

    @Column(name= "situacao")
    private boolean situacao;

    @Column(name = "sexo")
    private char sexo;

    @Column(name = "cpf")
    private String cpf;

    @ManyToOne(cascade = CascadeType.ALL)
    private Endereco endereco;

}
