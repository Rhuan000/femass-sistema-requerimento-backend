package org.femass.requerimento.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.femass.requerimento.entities.RequerimentoTemplate;
import org.femass.requerimento.entities.TemplateStatus;
import org.femass.requerimento.exceptions.BusinessValidationException;
import org.femass.requerimento.exceptions.ResourceNotFoundException;
import org.femass.requerimento.repositories.RequerimentoTemplateRepository;
import org.femass.requerimento.validators.RequerimentoTemplateValidator;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class RequerimentoTemplateService {
    @Inject RequerimentoTemplateRepository repository;
    @Inject RequerimentoTemplateValidator validator;
    @Inject CurrentUserService currentUserService;

    public List<RequerimentoTemplate> listActive() {
        return repository.findActive();
    }

    public List<RequerimentoTemplate> listAll() {
        exigirSolicitante();
        return repository.listAll();
    }

    public RequerimentoTemplate get(UUID id) {
        RequerimentoTemplate template = repository.findById(id);
        if (template == null) throw new ResourceNotFoundException("Template não encontrado");
        return template;
    }

    @Transactional
    public RequerimentoTemplate create(RequerimentoTemplate entity) {
        exigirSolicitante();
        validator.validateFields(entity.fields);
        entity.id = UUID.randomUUID();
        entity.fields = normalizeFields(entity.fields);
        entity.createdAt = Instant.now();
        entity.updatedAt = entity.createdAt;
        entity.status = TemplateStatus.PENDENTE;
        entity.isActive = false;
        entity.version = null;
        repository.persist(entity);
        return entity;
    }

    @Transactional
    public RequerimentoTemplate update(RequerimentoTemplate entity) {
        exigirSolicitante();
        RequerimentoTemplate db = get(entity.id);
        if (db.status != TemplateStatus.EDITANDO) {
            throw new BusinessValidationException("Somente templates em EDITANDO podem ser alterados");
        }
        validator.validateFields(entity.fields);
        db.name = entity.name;
        db.description = entity.description;
        db.category = entity.category;
        db.fields = normalizeFields(entity.fields);
        db.updatedAt = Instant.now();
        return db;
    }

    @Transactional
    public RequerimentoTemplate submitForApproval(UUID id) {
        exigirSolicitante();
        RequerimentoTemplate db = get(id);
        if (db.status != TemplateStatus.EDITANDO) {
            throw new BusinessValidationException("Somente templates em EDITANDO podem ser reenviados");
        }
        validator.validateFields(db.fields);
        changeStatus(db, TemplateStatus.PENDENTE);
        return db;
    }

    @Transactional
    public RequerimentoTemplate approve(UUID id) {
        exigirDiretor();
        RequerimentoTemplate db = get(id);
        if (db.status != TemplateStatus.PENDENTE) {
            throw new BusinessValidationException("A aprovação exige estado PENDENTE");
        }
        changeStatus(db, TemplateStatus.APROVADO);
        return db;
    }

    @Transactional
    public RequerimentoTemplate returnForEditing(UUID id) {
        exigirDiretor();
        RequerimentoTemplate db = get(id);
        if (db.status != TemplateStatus.PENDENTE) {
            throw new BusinessValidationException("A devolução exige estado PENDENTE");
        }
        changeStatus(db, TemplateStatus.EDITANDO);
        return db;
    }

    @Transactional
    public RequerimentoTemplate activate(UUID id) {
        exigirDiretor();
        RequerimentoTemplate db = get(id);
        if (db.status != TemplateStatus.APROVADO && db.status != TemplateStatus.DESATIVADO) {
            throw new BusinessValidationException("Somente templates aprovados ou desativados podem ser ativados");
        }
        changeStatus(db, TemplateStatus.ATIVO);
        if (db.version == null) db.version = 1;
        return db;
    }

    @Transactional
    public RequerimentoTemplate deactivate(UUID id) {
        exigirDiretor();
        RequerimentoTemplate db = get(id);
        if (db.status != TemplateStatus.ATIVO) {
            throw new BusinessValidationException("Somente templates ATIVOS podem ser desativados");
        }
        changeStatus(db, TemplateStatus.DESATIVADO);
        return db;
    }

    private void exigirSolicitante() {
        var usuario = currentUserService.get();
        if (!usuario.possuiPapel("DIRETOR") && !usuario.possuiPapel("COORDENADOR")
                && !usuario.possuiPapel("SECRETARIA")) {
            throw new org.femass.requerimento.exceptions.AuthorizationException(
                    "Usuário não pode solicitar ou editar templates");
        }
    }

    private void exigirDiretor() {
        if (!currentUserService.get().possuiPapel("DIRETOR")) {
            throw new org.femass.requerimento.exceptions.AuthorizationException(
                    "Somente o diretor pode decidir ou alterar a disponibilidade de templates");
        }
    }

    private void changeStatus(RequerimentoTemplate db, TemplateStatus status) {
        db.status = status;
        db.isActive = status == TemplateStatus.ATIVO;
        db.updatedAt = Instant.now();
    }

    private List<Map<String, Object>> normalizeFields(List<Map<String, Object>> fields) {
        return fields.stream().map(field -> {
            Map<String, Object> copy = new HashMap<>(field);
            if (!copy.containsKey("id") || copy.get("id") == null) {
                copy.put("id", UUID.randomUUID().toString());
            }
            return copy;
        }).toList();
    }
}
