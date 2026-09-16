package org.femass.requerimento.repositories;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import org.femass.requerimento.entities.Documento;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class DocumentoRepository implements PanacheRepositoryBase<Documento, UUID> {

    public List<Documento> findBySubmissionId(UUID submissionId) {
        return list("submission.id", submissionId);
    }

    public Documento findByIdAndSubmissionId(UUID id, UUID submissionId) {
        return find("id = ?1 and submission.id = ?2", id, submissionId).firstResult();
    }
}
