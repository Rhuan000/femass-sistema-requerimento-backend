package org.femass.requerimento.entities;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(
        name = "identidade_usuario",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_identidade_provider_external",
                        columnNames = {"provider", "externalId"}
                )
        }
)
public class IdentidadeUsuario {

    @Id
    @GeneratedValue
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "usuario_id",
            nullable = false
    )
    public Usuario usuario;

    @Column(nullable = false, length = 50)
    public String provider;

    @Column(nullable = false, length = 255)
    public String externalId;
}
