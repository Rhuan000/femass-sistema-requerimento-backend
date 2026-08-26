package org.femass.requerimento.repositories;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.femass.requerimento.entities.Papel;

import java.util.UUID;

@ApplicationScoped
public class PapelRepository
        implements PanacheRepositoryBase<Papel, UUID> {

    public Papel findByNome(String nome) {
        return find("nome", nome).firstResult();
    }
}
