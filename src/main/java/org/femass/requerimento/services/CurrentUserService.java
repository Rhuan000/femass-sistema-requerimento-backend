package org.femass.requerimento.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.femass.requerimento.entities.IdentidadeUsuario;
import org.femass.requerimento.entities.Usuario;
import org.femass.requerimento.repositories.IdentidadeUsuarioRepository;

@ApplicationScoped
public class CurrentUserService {

    @Inject
    JsonWebToken jwt;

    @Inject
    IdentidadeUsuarioRepository identidadeUsuarioRepository;

    public Usuario get() {
        String externalId = jwt.getSubject();

        IdentidadeUsuario identidade =
                identidadeUsuarioRepository
                        .findByProviderAndExternalId("keycloak", externalId);

        if (identidade == null) {
            throw new IllegalStateException(
                    "Usuário autenticado não possui identidade cadastrada"
            );
        }

        return identidade.usuario;
    }
}
