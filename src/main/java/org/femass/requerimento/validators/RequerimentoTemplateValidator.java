package org.femass.requerimento.validators;

import jakarta.enterprise.context.ApplicationScoped;
import org.femass.requerimento.exceptions.BusinessValidationException;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class RequerimentoTemplateValidator {

    /**
     * Valida os campos do template e reúne todos os erros encontrados.
     * As chaves de fieldErrors seguem o JSON enviado: "fields", "fields[0].fieldKey", ...
     */
    public void validateFields(List<Map<String, Object>> fields) {

        if (fields == null || fields.isEmpty()) {
            throw new BusinessValidationException(
                    "Template deve ter pelo menos um campo",
                    Map.of("fields", "Template deve ter pelo menos um campo"));
        }

        Map<String, String> errors = new LinkedHashMap<>();
        Set<String> keys = new HashSet<>();

        for (int i = 0; i < fields.size(); i++) {
            Map<String, Object> field = fields.get(i);
            String path = "fields[" + i + "].";

            String key = (String) field.get("fieldKey");

            // 1. null / vazio primeiro (OBRIGATÓRIO)
            if (key == null || key.isBlank()) {
                errors.put(path + "fieldKey", "fieldKey não pode ser vazio");
            }
            // 2. formato válido depois (seguro)
            else if (!key.matches("^[a-z0-9_]+$")) {
                errors.put(path + "fieldKey", "fieldKey inválido: " + key);
            }
            // 3. unicidade
            else if (!keys.add(key)) {
                errors.put(path + "fieldKey", "fieldKey duplicado: " + key);
            }

            // 4. type obrigatório
            String type = (String) field.get("type");
            if (type == null || type.isBlank()) {
                errors.put(path + "type", "type obrigatório");
            }

            // 5. label obrigatório
            String label = (String) field.get("label");
            if (label == null || label.isBlank()) {
                errors.put(path + "label", "label obrigatório");
            }
        }

        if (!errors.isEmpty()) {
            throw new BusinessValidationException("Template possui campos inválidos", errors);
        }
    }
}
