package org.femass.requerimento.entities;

import jakarta.persistence.*;

import java.util.UUID;

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
    @GeneratedValue
    public UUID id;

    @Column(nullable = false, length = 50)
    public String nome;

    @Column(length = 255)
    public String descricao;
}
