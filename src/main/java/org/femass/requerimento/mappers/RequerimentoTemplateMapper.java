package org.femass.requerimento.mappers;

import jakarta.enterprise.context.ApplicationScoped;
import org.femass.requerimento.dtos.RequerimentoTemplateDTO;
import org.femass.requerimento.dtos.RequerimentoTemplateFieldDTO;
import org.femass.requerimento.dtos.SelectOptionDTO;
import org.femass.requerimento.entities.RequerimentoTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class RequerimentoTemplateMapper {

    public RequerimentoTemplateDTO toDTO(RequerimentoTemplate entity) {

        RequerimentoTemplateDTO dto = new RequerimentoTemplateDTO();

        dto.id = entity.id;
        dto.name = entity.name;
        dto.description = entity.description;
        dto.category = entity.category;
        dto.version = entity.version;
        dto.isActive = entity.isActive;
        dto.createdAt = entity.createdAt;
        dto.updatedAt = entity.updatedAt;

        dto.fields = entity.fields == null
                ? List.of()
                : entity.fields.stream()
                .map(this::mapToFieldDTO)
                .toList();

        return dto;
    }

    public RequerimentoTemplate toEntity(RequerimentoTemplateDTO dto) {

        RequerimentoTemplate entity = new RequerimentoTemplate();

        entity.id = dto.id;
        entity.name = dto.name;
        entity.description = dto.description;
        entity.category = dto.category;
        entity.version = dto.version;
        entity.isActive = dto.isActive;
        entity.createdAt = dto.createdAt;
        entity.updatedAt = dto.updatedAt;

        entity.fields = dto.fields == null
                ? List.of()
                : dto.fields.stream()
                .map(this::mapToMap)
                .toList();

        return entity;
    }


    private RequerimentoTemplateFieldDTO mapToFieldDTO(Map<String, Object> map) {

        RequerimentoTemplateFieldDTO dto = new RequerimentoTemplateFieldDTO();

        dto.fieldKey = (String) map.get("fieldKey");
        dto.label = (String) map.get("label");
        dto.type = (String) map.get("type");
        dto.required = (Boolean) map.get("required");
        dto.placeholder = (String) map.get("placeholder");
        dto.description = (String) map.get("description");

        Object position = map.get("position");
        dto.position = position != null ? (Integer) position : null;

        dto.options = (List<SelectOptionDTO>) map.get("options");

        return dto;
    }

    // -------------------------
    // DTO -> ENTITY (JSONB MAP)
    // -------------------------
    private Map<String, Object> mapToMap(RequerimentoTemplateFieldDTO dto) {

        Map<String, Object> map = new HashMap<>();

        map.put("fieldKey", dto.fieldKey);
        map.put("label", dto.label);
        map.put("type", dto.type);
        map.put("required", dto.required);
        map.put("placeholder", dto.placeholder);
        map.put("description", dto.description);
        map.put("position", dto.position);
        map.put("options", dto.options);

        return map;
    }
}