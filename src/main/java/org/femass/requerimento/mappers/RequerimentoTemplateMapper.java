package org.femass.requerimento.mappers;

import jakarta.enterprise.context.ApplicationScoped;
import org.femass.requerimento.dtos.RequerimentoTemplateDTO;
import org.femass.requerimento.dtos.RequerimentoTemplateFieldDTO;
import org.femass.requerimento.dtos.SelectOptionDTO;
import org.femass.requerimento.entities.RequerimentoTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class RequerimentoTemplateMapper {

    public RequerimentoTemplateDTO toDTO(RequerimentoTemplate entity) {
        if (entity == null) return null;
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
        if (dto == null) return null;
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

        dto.id = (String) map.get("id");
        dto.fieldKey = (String) map.get("fieldKey");
        dto.label = (String) map.get("label");
        dto.type = (String) map.get("type");
        dto.required = (Boolean) map.get("required");
        dto.placeholder = (String) map.get("placeholder");
        dto.description = (String) map.get("description");

        Object position = map.get("position");
        dto.position = toInteger(position);

        dto.options = toOptions(map.get("options"));

        return dto;
    }

    private Map<String, Object> mapToMap(RequerimentoTemplateFieldDTO dto) {

        Map<String, Object> map = new HashMap<>();

        if (dto.id != null) {
            map.put("id", dto.id);
        }
        map.put("fieldKey", dto.fieldKey);
        map.put("label", dto.label);
        map.put("type", dto.type);
        map.put("required", dto.required);
        map.put("placeholder", dto.placeholder);
        map.put("description", dto.description);
        map.put("position", dto.position);
        map.put("options", dto.options == null
                ? List.of()
                : dto.options.stream().map(this::optionToMap).toList());

        return map;
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.intValueExact();
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
    }

    private List<SelectOptionDTO> toOptions(Object value) {
        if (!(value instanceof List<?> options)) {
            return List.of();
        }

        return options.stream()
                .map(this::toOptionDTO)
                .toList();
    }

    private SelectOptionDTO toOptionDTO(Object value) {
        if (value instanceof SelectOptionDTO option) {
            return option;
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Opção de campo inválida");
        }

        SelectOptionDTO option = new SelectOptionDTO();
        option.label = stringValue(map.get("label"));
        option.value = stringValue(map.get("value"));
        return option;
    }

    private Map<String, Object> optionToMap(SelectOptionDTO option) {
        Map<String, Object> map = new HashMap<>();
        map.put("label", option.label);
        map.put("value", option.value);
        return map;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
