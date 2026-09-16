package org.femass.requerimento.entities;

import jakarta.persistence.*;


@Entity
@Table(
        name = "papel",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_papel_nome",
                        columnNames = "nome"
                )
        }
)
public class Papel {
    @Id
    public Long id;

    @Column(nullable = false, length = 50)
    public String nome;

    @Column(length = 255)
    public String descricao;
}
