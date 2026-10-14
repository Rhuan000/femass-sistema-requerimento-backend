package org.femass.requerimento.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.femass.requerimento.entities.IdentidadeUsuario;
import org.femass.requerimento.entities.Usuario;
import org.femass.requerimento.repositories.IdentidadeUsuarioRepository;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;

import java.util.Optional;

@ApplicationScoped
public class CurrentUserService {

    @Inject
    JsonWebToken jwt;

    @Inject
    IdentidadeUsuarioRepository identidadeUsuarioRepository;

    @Transactional
    public Usuario get() {
        return find().orElseThrow(() -> new IllegalStateException(
                "Usuário autenticado não possui identidade cadastrada"
        ));
    }

    /**
     * Igual a {@link #get()}, mas não falha quando o usuário autenticado
     * ainda não está vinculado a um usuário do banco.
     */
    @Transactional
    public Optional<Usuario> find() {
        String externalId = jwt.getSubject();
        if (externalId == null) {
            return Optional.empty();
        }

        IdentidadeUsuario identidade =
                identidadeUsuarioRepository
                        .findByProviderAndExternalId("keycloak", externalId);

        if (identidade == null) {
            return Optional.empty();
        }

        Usuario usuario = identidade.usuario;
        // Carrega os papéis ainda na transação: possuiPapel() é chamado depois dela.
        usuario.papeis.forEach(up -> Hibernate.initialize(up.papel));
        return Optional.of(usuario);
    }
}
