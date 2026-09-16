package org.femass.requerimento.validators;

import jakarta.enterprise.context.ApplicationScoped;
import org.femass.requerimento.entities.RequerimentoSubmission;
import org.femass.requerimento.entities.RequerimentoTemplate;
import org.femass.requerimento.exceptions.BusinessValidationException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class RequerimentoSubmissionValidator {

    public void validate(RequerimentoTemplate template, RequerimentoSubmission submission) {
        Map<String, Object> data = submission.data == null ? Map.of() : submission.data;
        List<Map<String, Object>> fields = template.fields == null ? List.of() : template.fields;

        Set<String> templateFieldKeys = fields.stream()
                .map(field -> (String) field.get("fieldKey"))
                .collect(Collectors.toSet());

        for (String key : data.keySet()) {
            if (!templateFieldKeys.contains(key)) {
                throw new BusinessValidationException("Campo inválido: " + key);
            }
        }

        for (Map<String, Object> field : fields) {
            String key = (String) field.get("fieldKey");
            if (Boolean.TRUE.equals(field.get("required"))) {
                Object value = data.get(key);
                if (value == null || value.toString().isBlank()) {
                    throw new BusinessValidationException("Campo obrigatório: " + key);
                }
            }
        }

        for (Map<String, Object> field : fields) {
            String key = (String) field.get("fieldKey");
            Object value = data.get(key);
            if (value != null) {
                validateType(key, (String) field.get("type"), value, field);
            }
        }
    }

    private void validateType(String key, String type, Object value, Map<String, Object> field) {
        if (type == null) return;

        switch (type) {
            case "number" -> {
                try {
                    Double.parseDouble(value.toString());
                } catch (NumberFormatException exception) {
                    throw new BusinessValidationException("Campo deve ser número: " + key);
                }
            }
            case "email" -> {
                if (!value.toString().contains("@")) {
                    throw new BusinessValidationException("E-mail inválido: " + key);
                }
            }
            case "date" -> {
                try {
                    Instant.parse(value.toString());
                } catch (Exception exception) {
                    throw new BusinessValidationException("Data inválida: " + key);
                }
            }
            case "select" -> validateSelect(key, value, field);
            default -> {
                // Tipos textuais não exigem validação adicional neste momento.
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateSelect(String key, Object value, Map<String, Object> field) {
        List<Map<String, Object>> options = (List<Map<String, Object>>) field.get("options");
        boolean valid = options != null && options.stream()
                .map(option -> String.valueOf(option.get("value")))
                .anyMatch(option -> option.equals(value.toString()));
        if (!valid) {
            throw new BusinessValidationException("Valor inválido para seleção: " + key);
        }
    }
}
