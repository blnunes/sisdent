package br.com.itbn.sisdent.model;

import br.com.itbn.sisdent.comum.EntitySisdent;
import lombok.*;

import javax.persistence.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Estado extends EntitySisdent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name= "idestado")
    private Long id;

    @Column(name = "nome_completo")
    private String nome;

    @Column(name = "uf")
    private String uf;


}
