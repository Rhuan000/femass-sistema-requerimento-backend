package org.femass.requerimento.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.femass.requerimento.entities.RequerimentoTemplate;
import org.femass.requerimento.repositories.RequerimentoTemplateRepository;
import org.femass.requerimento.validators.RequerimentoTemplateValidator;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RequerimentoTemplateService {

    @Inject
    RequerimentoTemplateRepository repository;

    @Inject
    RequerimentoTemplateValidator validator;

    public List<RequerimentoTemplate> listActive() {
        return repository.findActive();
    }

    public RequerimentoTemplate get(UUID id) {
        return repository.findById(id);
    }

    @Transactional
    public RequerimentoTemplate create(RequerimentoTemplate entity) {
        validator.validateFields(entity.fields);

        entity.id = UUID.randomUUID();
        entity.createdAt = Instant.now();
        entity.updatedAt = Instant.now();
        entity.isActive = true;
        entity.version = 1;

        repository.persist(entity);
        return entity;
    }

    @Transactional
    public RequerimentoTemplate update(RequerimentoTemplate entity) {

        RequerimentoTemplate db = repository.findById(entity.id);

        if (db == null) {
            throw new RuntimeException("Template not found");
        }

        db.name = entity.name;
        db.description = entity.description;
        db.category = entity.category;
        db.fields = entity.fields;
        db.updatedAt = Instant.now();

        repository.persist(db);

        return db;
    }

    @Transactional
    public void deactivate(UUID id) {

        RequerimentoTemplate db = repository.findById(id);

        if (db != null) {
            db.isActive = false;
            db.updatedAt = Instant.now();
        }
    }
}