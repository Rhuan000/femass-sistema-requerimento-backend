package org.femass.requerimento.repositories;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.femass.requerimento.entities.RequerimentoTemplate;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RequerimentoTemplateRepository implements PanacheRepositoryBase<RequerimentoTemplate, UUID> {

    public List<RequerimentoTemplate> findActive() {
        return list("isActive = true");
    }

    public RequerimentoTemplate findById(UUID id) {
        return find("id", id).firstResult();
    }

    public List<RequerimentoTemplate> findByCategory(String category) {
        return list("category", category);
    }
}