package org.femass.requerimento.mappers;

import jakarta.enterprise.context.ApplicationScoped;
import org.femass.requerimento.dtos.RequerimentoSubmissionAnswerDTO;
import org.femass.requerimento.dtos.RequerimentoSubmissionDTO;
import org.femass.requerimento.entities.RequerimentoSubmission;
import org.femass.requerimento.entities.RequerimentoTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class RequerimentoSubmissionMapper {

    public RequerimentoSubmissionDTO toDTO(RequerimentoSubmission entity) {
        return toDTO(entity, null);
    }

    public RequerimentoSubmissionDTO toDTO(
            RequerimentoSubmission entity,
            RequerimentoTemplate template
    ) {
        RequerimentoSubmissionDTO dto = new RequerimentoSubmissionDTO();

        dto.id = entity.id;
        dto.templateId = entity.templateId;
        dto.submittedBy = entity.submittedBy;
        dto.status = entity.status;
        dto.createdAt = entity.createdAt;

        dto.data = entity.data;
        dto.answers = mapAnswers(entity.data, entity.answers, template);

        return dto;
    }

    private List<RequerimentoSubmissionAnswerDTO> mapAnswers(
            Map<String, Object> data,
            List<Map<String, Object>> answerSnapshot,
            RequerimentoTemplate template
    ) {
        if (answerSnapshot != null && !answerSnapshot.isEmpty()) {
            return answerSnapshot.stream()
                    .map(answer -> toAnswer(
                            stringValue(answer.get("fieldKey")),
                            stringValue(answer.get("label")),
                            answer.get("value")
                    ))
                    .toList();
        }

        if (data == null || data.isEmpty()) {
            return List.of();
        }

        if (template == null || template.fields == null) {
            return data.entrySet().stream()
                    .map(entry -> toAnswer(entry.getKey(), entry.getKey(), entry.getValue()))
                    .toList();
        }

        return template.fields.stream()
                .sorted(Comparator.comparingInt(this::position))
                .map(field -> {
                    String fieldKey = stringValue(field.get("fieldKey"));
                    if (fieldKey == null || !data.containsKey(fieldKey)) {
                        return null;
                    }
                    String label = stringValue(field.get("label"));
                    return toAnswer(
                            fieldKey,
                            label == null || label.isBlank() ? fieldKey : label,
                            data.get(fieldKey)
                    );
                })
                .filter(answer -> answer != null)
                .toList();
    }

    private RequerimentoSubmissionAnswerDTO toAnswer(
            String fieldKey,
            String label,
            Object value
    ) {
        RequerimentoSubmissionAnswerDTO answer = new RequerimentoSubmissionAnswerDTO();
        answer.fieldKey = fieldKey;
        answer.label = label;
        answer.value = value;
        return answer;
    }

    private int position(Map<String, Object> field) {
        Object value = field.get("position");
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.MAX_VALUE;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    public RequerimentoSubmission toEntity(RequerimentoSubmissionDTO dto) {

        RequerimentoSubmission entity = new RequerimentoSubmission();

        entity.id = dto.id;
        entity.templateId = dto.templateId;
        entity.submittedBy = dto.submittedBy;
        entity.status = dto.status;
        entity.createdAt = dto.createdAt;

        entity.data = dto.data;

        return entity;
    }
}
