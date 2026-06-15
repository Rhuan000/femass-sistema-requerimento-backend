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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        entity.answers = buildAnswerSnapshot(template, entity.data);

        repository.persist(entity);

        return entity;
    }

    private List<Map<String, Object>> buildAnswerSnapshot(
            RequerimentoTemplate template,
            Map<String, Object> data
    ) {
        if (template.fields == null || data == null) {
            return List.of();
        }

        return template.fields.stream()
                .sorted(Comparator.comparingInt(this::position))
                .map(field -> {
                    String fieldKey = stringValue(field.get("fieldKey"));
                    String label = stringValue(field.get("label"));
                    if (fieldKey == null || !data.containsKey(fieldKey)) {
                        return null;
                    }

                    Map<String, Object> answer = new HashMap<>();
                    answer.put("fieldKey", fieldKey);
                    answer.put("label", field.getOrDefault("label", label));
                    answer.put("value", data.get(fieldKey));
                    return answer;
                })
                .filter(answer -> answer != null)
                .toList();
    }

    private int position(Map<String, Object> field) {
        Object value = field.get("position");
        return value instanceof Number number
                ? number.intValue()
                : Integer.MAX_VALUE;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
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
