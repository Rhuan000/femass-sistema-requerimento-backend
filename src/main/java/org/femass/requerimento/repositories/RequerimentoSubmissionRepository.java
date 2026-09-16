package org.femass.requerimento.repositories;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.femass.requerimento.entities.RequerimentoSubmission;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RequerimentoSubmissionRepository implements PanacheRepositoryBase<RequerimentoSubmission, UUID> {

    public List<RequerimentoSubmission> findByTemplateId(UUID templateId) {
        return list("templateId", templateId);
    }

    public RequerimentoSubmission findDraftByTemplateAndUsuario(
            UUID templateId,
            UUID usuarioId,
            String status
    ) {
        return find(
                "templateId = ?1 and usuario.id = ?2 and status = ?3 order by updatedAt desc",
                templateId,
                usuarioId,
                status
        ).firstResult();
    }

    public List<RequerimentoSubmission> findByUsuario(UUID usuarioId) {
        return list("usuario.id", usuarioId);
    }

    public List<RequerimentoSubmission> findByStatus(String status) {
        return list("status", status);
    }
}
