package org.femass.requerimento.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.femass.requerimento.entities.RequerimentoSubmission;
import org.femass.requerimento.entities.RequerimentoTemplate;
import org.femass.requerimento.repositories.RequerimentoSubmissionRepository;
import org.femass.requerimento.repositories.RequerimentoTemplateRepository;
import org.femass.requerimento.validators.RequerimentoSubmissionValidator;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RequerimentoSubmissionService {

    @Inject
    RequerimentoSubmissionRepository repository;

    @Inject
    RequerimentoSubmissionValidator validator;

    @Inject
    RequerimentoTemplateRepository templateRepository;

    public RequerimentoSubmission get(UUID id) {
        return repository.findById(id);
    }

    public List<RequerimentoSubmission> findByTemplate(UUID templateId) {
        return repository.findByTemplateId(templateId);
    }

    @Transactional
    public RequerimentoSubmission create(RequerimentoSubmission entity) {

        RequerimentoTemplate template =
                templateRepository.findById(entity.templateId);

        if (template == null) {
            throw new RuntimeException("Template não encontrado");
        }

        validator.validate(template, entity);

        entity.id = UUID.randomUUID();
        entity.createdAt = Instant.now();
        entity.status = "pending";

        repository.persist(entity);

        return entity;
    }

    @Transactional
    public void updateStatus(UUID id, String status) {

        RequerimentoSubmission db = repository.findById(id);

        if (db == null) {
            throw new RuntimeException("Submission not found");
        }

        db.status = status;
    }
}