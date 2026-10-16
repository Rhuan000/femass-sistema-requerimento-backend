package org.femass.requerimento.validators;

import jakarta.enterprise.context.ApplicationScoped;
import org.femass.requerimento.entities.RequerimentoSubmission;
import org.femass.requerimento.entities.RequerimentoTemplate;
import org.femass.requerimento.exceptions.BusinessValidationException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class RequerimentoSubmissionValidator {

    /**
     * Valida as respostas e reúne todos os erros encontrados.
     * As chaves de fieldErrors são os fieldKey dos campos do template.
     */
    public void validate(RequerimentoTemplate template, RequerimentoSubmission submission) {
        Map<String, Object> data = submission.data == null ? Map.of() : submission.data;
        List<Map<String, Object>> fields = template.fields == null ? List.of() : template.fields;
        Map<String, String> errors = new LinkedHashMap<>();

        Set<String> templateFieldKeys = fields.stream()
                .map(field -> (String) field.get("fieldKey"))
                .collect(Collectors.toSet());

        for (String key : data.keySet()) {
            if (!templateFieldKeys.contains(key)) {
                errors.put(key, "Campo inválido: não existe no template");
            }
        }

        for (Map<String, Object> field : fields) {
            String key = (String) field.get("fieldKey");
            Object value = data.get(key);
            if (value == null || value.toString().isBlank()) {
                if (Boolean.TRUE.equals(field.get("required"))) {
                    errors.put(key, "Campo obrigatório");
                }
                continue;
            }
            String typeError = validateType((String) field.get("type"), value, field);
            if (typeError != null) {
                errors.put(key, typeError);
            }
        }

        if (!errors.isEmpty()) {
            throw new BusinessValidationException("Requerimento possui campos inválidos", errors);
        }
    }

    /** Retorna a mensagem de erro do campo, ou null quando o valor é válido. */
    private String validateType(String type, Object value, Map<String, Object> field) {
        if (type == null) return null;

        return switch (type) {
            case "number" -> {
                try {
                    Double.parseDouble(value.toString());
                    yield null;
                } catch (NumberFormatException exception) {
                    yield "Campo deve ser número";
                }
            }
            case "email" -> value.toString().contains("@") ? null : "E-mail inválido";
            case "date" -> {
                try {
                    Instant.parse(value.toString());
                    yield null;
                } catch (Exception exception) {
                    yield "Data inválida";
                }
            }
            case "select" -> isValidSelect(value, field) ? null : "Valor inválido para seleção";
            // Tipos textuais não exigem validação adicional neste momento.
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private boolean isValidSelect(Object value, Map<String, Object> field) {
        List<Map<String, Object>> options = (List<Map<String, Object>>) field.get("options");
        return options != null && options.stream()
                .map(option -> String.valueOf(option.get("value")))
                .anyMatch(option -> option.equals(value.toString()));
    }
}
