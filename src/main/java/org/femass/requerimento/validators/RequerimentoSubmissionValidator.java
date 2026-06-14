package org.femass.requerimento.validators;

import jakarta.enterprise.context.ApplicationScoped;
import org.femass.requerimento.entities.RequerimentoTemplate;
import org.femass.requerimento.entities.RequerimentoSubmission;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class RequerimentoSubmissionValidator {

    public void validate(RequerimentoTemplate template, RequerimentoSubmission submission) {

        Map<String, Object> data = submission.data;

        List<Map<String, Object>> fields = template.fields;

        Set<String> templateFieldKeys = fields.stream()
                .map(f -> (String) f.get("fieldKey"))
                .collect(Collectors.toSet());

        // 1. campos desconhecidos
        for (String key : data.keySet()) {
            if (!templateFieldKeys.contains(key)) {
                throw new RuntimeException("Campo inválido: " + key);
            }
        }

        // 2. obrigatórios
        for (Map<String, Object> field : fields) {

            String key = (String) field.get("fieldKey");
            Boolean required = (Boolean) field.get("required");

            if (Boolean.TRUE.equals(required)) {

                Object value = data.get(key);

                if (value == null || value.toString().isBlank()) {
                    throw new RuntimeException("Campo obrigatório: " + key);
                }
            }
        }

        // 3. validação por tipo
        for (Map<String, Object> field : fields) {

            String key = (String) field.get("fieldKey");
            String type = (String) field.get("type");

            Object value = data.get(key);
            if (value == null) continue;

            validateType(key, type, value, field);
        }
    }

    private void validateType(String key, String type, Object value, Map<String, Object> field) {

        switch (type) {

            case "number" -> {
                try {
                    Double.parseDouble(value.toString());
                } catch (Exception e) {
                    throw new RuntimeException("Campo deve ser número: " + key);
                }
            }

            case "email" -> {
                if (!value.toString().contains("@")) {
                    throw new RuntimeException("Email inválido: " + key);
                }
            }

            case "date" -> {
                try {
                    java.time.Instant.parse(value.toString());
                } catch (Exception e) {
                    throw new RuntimeException("Data inválida: " + key);
                }
            }

            case "select" -> {

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> options =
                        (List<Map<String, Object>>) field.get("options");

                boolean valid = options != null &&
                        options.stream()
                                .map(opt -> String.valueOf(opt.get("value")))
                                .anyMatch(v -> v.equals(value.toString()));

                if (!valid) {
                    throw new RuntimeException("Valor inválido para select: " + key);
                }
            }
        }
    }
}