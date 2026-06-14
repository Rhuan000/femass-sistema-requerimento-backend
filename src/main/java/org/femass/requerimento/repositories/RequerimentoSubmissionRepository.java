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

    public List<RequerimentoSubmission> findBySubmittedBy(String user) {
        return list("submittedBy", user);
    }

    public List<RequerimentoSubmission> findByStatus(String status) {
        return list("status", status);
    }
}