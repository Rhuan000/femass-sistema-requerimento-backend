package org.femass.requerimento.repositories;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.femass.requerimento.entities.IdentidadeUsuario;

import java.util.UUID;

@ApplicationScoped
public class IdentidadeUsuarioRepository
        implements PanacheRepositoryBase<IdentidadeUsuario, UUID> {

    public IdentidadeUsuario findByProviderAndExternalId(
            String provider,
            String externalId
    ) {
        return find(
                "provider = ?1 and externalId = ?2",
                provider,
                externalId
        ).firstResult();
    }
}
