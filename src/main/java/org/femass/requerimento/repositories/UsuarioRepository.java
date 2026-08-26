package org.femass.requerimento.repositories;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.femass.requerimento.entities.Usuario;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class UsuarioRepository
        implements PanacheRepositoryBase<Usuario, UUID> {

    public Usuario findByEmail(String email) {
        return find("email", email).firstResult();
    }

    public List<Usuario> findAtivos() {
        return list("ativo", true);
    }
}
