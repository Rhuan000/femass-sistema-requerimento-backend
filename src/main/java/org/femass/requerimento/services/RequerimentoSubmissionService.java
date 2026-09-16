package org.femass.requerimento.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.femass.requerimento.entities.RequerimentoSubmission;
import org.femass.requerimento.entities.RequerimentoTemplate;
import org.femass.requerimento.entities.Usuario;
import org.femass.requerimento.exceptions.AuthorizationException;
import org.femass.requerimento.exceptions.BusinessValidationException;
import org.femass.requerimento.exceptions.ResourceNotFoundException;
import org.femass.requerimento.repositories.RequerimentoSubmissionRepository;
import org.femass.requerimento.repositories.RequerimentoTemplateRepository;
import org.femass.requerimento.validators.RequerimentoSubmissionValidator;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class RequerimentoSubmissionService {

    public static final String STATUS_RASCUNHO = "RASCUNHO";
    public static final String STATUS_ENVIADO = "ENVIADO";

    @Inject RequerimentoSubmissionRepository repository;
    @Inject RequerimentoSubmissionValidator validator;
    @Inject RequerimentoTemplateRepository templateRepository;
    @Inject CurrentUserService currentUserService;

    public RequerimentoSubmission get(UUID id) {
        RequerimentoSubmission submission = repository.findById(id);
        if (submission == null) {
            throw new ResourceNotFoundException("Requerimento não encontrado");
        }
        return submission;
    }

    public List<RequerimentoSubmission> findByTemplate(UUID templateId) {
        return repository.findByTemplateId(templateId);
    }

    public RequerimentoSubmission findCurrentUserDraftByTemplate(UUID templateId) {
        Usuario currentUser = currentUserService.get();
        RequerimentoSubmission draft = repository.findDraftByTemplateAndUsuario(
                templateId,
                currentUser.id,
                STATUS_RASCUNHO
        );
        if (draft == null) {
            throw new ResourceNotFoundException(
                    "Rascunho não encontrado para este usuário e template"
            );
        }
        return draft;
    }

    @Transactional
    public RequerimentoSubmission createDraft(RequerimentoSubmission entity) {
        if (entity.templateId == null) {
            throw new BusinessValidationException("O template é obrigatório");
        }
        if (templateRepository.findById(entity.templateId) == null) {
            throw new ResourceNotFoundException("Template não encontrado");
        }

        Instant now = Instant.now();
        entity.id = UUID.randomUUID();
        entity.createdAt = now;
        entity.updatedAt = now;
        entity.status = STATUS_RASCUNHO;
        entity.usuario = currentUserService.get();
        entity.data = entity.data == null ? new HashMap<>() : new HashMap<>(entity.data);
        entity.answers = List.of();
        repository.persist(entity);
        return entity;
    }

    @Transactional
    public RequerimentoSubmission saveDraft(UUID id, Map<String, Object> data) {
        RequerimentoSubmission draft = getOwnedDraft(id);
        draft.data = data == null ? new HashMap<>() : new HashMap<>(data);
        draft.updatedAt = Instant.now();
        return draft;
    }

    @Transactional
    public RequerimentoSubmission submit(UUID id) {
        RequerimentoSubmission draft = getOwnedDraft(id);
        RequerimentoTemplate template = templateRepository.findById(draft.templateId);
        if (template == null) {
            throw new ResourceNotFoundException("Template não encontrado");
        }

        validator.validate(template, draft);
        draft.answers = buildAnswerSnapshot(template, draft.data);
        draft.status = STATUS_ENVIADO;
        draft.submittedAt = Instant.now();
        draft.updatedAt = draft.submittedAt;
        return draft;
    }

    public RequerimentoSubmission getOwnedDraft(UUID id) {
        RequerimentoSubmission submission = getOwnedSubmission(id);
        if (!STATUS_RASCUNHO.equals(submission.status)) {
            throw new BusinessValidationException("Somente requerimentos em rascunho podem ser alterados");
        }
        return submission;
    }

    public RequerimentoSubmission getOwnedSubmission(UUID id) {
        RequerimentoSubmission submission = get(id);
        Usuario currentUser = currentUserService.get();
        if (submission.usuario == null || !submission.usuario.id.equals(currentUser.id)) {
            throw new AuthorizationException("Você não pode acessar este requerimento");
        }
        return submission;
    }

    private List<Map<String, Object>> buildAnswerSnapshot(RequerimentoTemplate template, Map<String, Object> data) {
        if (template.fields == null || data == null) {
            return List.of();
        }
        return template.fields.stream()
                .sorted(Comparator.comparingInt(this::position))
                .map(field -> {
                    String fieldKey = stringValue(field.get("fieldKey"));
                    if (fieldKey == null || !data.containsKey(fieldKey)) return null;
                    Map<String, Object> answer = new HashMap<>();
                    answer.put("fieldKey", fieldKey);
                    answer.put("label", field.get("label"));
                    answer.put("value", data.get(fieldKey));
                    return answer;
                })
                .filter(answer -> answer != null)
                .toList();
    }

    private int position(Map<String, Object> field) {
        Object value = field.get("position");
        return value instanceof Number number ? number.intValue() : Integer.MAX_VALUE;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
