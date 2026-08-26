
package org.femass.requerimento.entities;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity

@Table(
    name = "usuario",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_usuario_email",
            columnNames = "email"
        )
    }
)public class Usuario {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(nullable = false, length = 150)
    public String nome;

    @Column(nullable = false, length = 150)
    public String email;

    @Column(nullable = false)
    public Boolean ativo = true;

    @Column(nullable = false)
    public Instant createdAt;

    public Instant updatedAt;

    @OneToMany(
            mappedBy = "usuario",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    public Set<IdentidadeUsuario> identidades = new HashSet<>();

    @OneToMany(
            mappedBy = "usuario",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    public Set<UsuarioPapel> papeis = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();

        if (ativo == null) {
            ativo = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void adicionarIdentidade(
            String provider,
            String externalId
    ) {
        IdentidadeUsuario identidade = new IdentidadeUsuario();

        identidade.usuario = this;
        identidade.provider = provider;
        identidade.externalId = externalId;

        identidades.add(identidade);
    }

    public void adicionarPapel(Papel papel) {
        UsuarioPapel usuarioPapel = new UsuarioPapel();

        usuarioPapel.usuario = this;
        usuarioPapel.papel = papel;

        papeis.add(usuarioPapel);
    }
}
