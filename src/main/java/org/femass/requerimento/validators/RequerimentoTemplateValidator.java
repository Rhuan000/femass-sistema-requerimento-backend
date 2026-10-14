package org.femass.requerimento.validators;

import jakarta.enterprise.context.ApplicationScoped;
import org.femass.requerimento.exceptions.BusinessValidationException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class RequerimentoTemplateValidator {

    public void validateFields(List<Map<String, Object>> fields) {

        if (fields == null || fields.isEmpty()) {
            throw new BusinessValidationException("Template deve ter pelo menos um campo");
        }

        Set<String> keys = new HashSet<>();

        for (Map<String, Object> field : fields) {

            String key = (String) field.get("fieldKey");

            // 1. null / vazio primeiro (OBRIGATÓRIO)
            if (key == null || key.isBlank()) {
                throw new BusinessValidationException("fieldKey não pode ser vazio");
            }

            // 2. formato válido depois (seguro)
            if (!key.matches("^[a-z0-9_]+$")) {
                throw new BusinessValidationException("fieldKey inválido: " + key);
            }

            // 3. unicidade
            if (!keys.add(key)) {
                throw new BusinessValidationException("fieldKey duplicado: " + key);
            }

            // 4. type obrigatório
            String type = (String) field.get("type");
            if (type == null || type.isBlank()) {
                throw new BusinessValidationException("type obrigatório para fieldKey: " + key);
            }

            // 5. label obrigatório
            String label = (String) field.get("label");
            if (label == null || label.isBlank()) {
                throw new BusinessValidationException("label obrigatório para fieldKey: " + key);
            }
        }
    }
}