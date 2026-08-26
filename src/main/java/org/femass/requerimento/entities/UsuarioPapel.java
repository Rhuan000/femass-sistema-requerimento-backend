package org.femass.requerimento.entities;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(
        name = "usuario_papel",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_usuario_papel",
                        columnNames = {"usuario_id", "papel_id"}
                )
        }
)
public class UsuarioPapel {

    @Id
    @GeneratedValue
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "usuario_id",
            nullable = false
    )
    public Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "papel_id",
            nullable = false
    )
    public Papel papel;
}
