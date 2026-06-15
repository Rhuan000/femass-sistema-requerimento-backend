package org.femass.requerimento.mappers;

import org.femass.requerimento.dtos.RequerimentoTemplateDTO;
import org.femass.requerimento.dtos.RequerimentoTemplateFieldDTO;
import org.femass.requerimento.dtos.SelectOptionDTO;
import org.femass.requerimento.entities.RequerimentoTemplate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequerimentoTemplateMapperTest {

    private final RequerimentoTemplateMapper mapper = new RequerimentoTemplateMapper();

    @Test
    void mapsJsonbNumbersAndOptionsToDto() {
        RequerimentoTemplate entity = new RequerimentoTemplate();
        entity.fields = List.of(Map.of(
                "id", "field-id",
                "fieldKey", "finalidade",
                "label", "Finalidade",
                "type", "select",
                "required", true,
                "position", new BigDecimal("2"),
                "options", List.of(Map.of("label", "Estágio", "value", "estagio"))
        ));

        RequerimentoTemplateFieldDTO field = mapper.toDTO(entity).fields.getFirst();

        assertEquals(2, field.position);
        assertEquals("field-id", field.id);
        assertEquals("Estágio", field.options.getFirst().label);
        assertEquals("estagio", field.options.getFirst().value);
    }

    @Test
    void preservesFieldIdAndSerializesOptionsAsMaps() {
        SelectOptionDTO option = new SelectOptionDTO();
        option.label = "Emprego";
        option.value = "emprego";

        RequerimentoTemplateFieldDTO field = new RequerimentoTemplateFieldDTO();
        field.id = "field-id";
        field.fieldKey = "finalidade";
        field.label = "Finalidade";
        field.type = "select";
        field.position = 1;
        field.options = List.of(option);

        RequerimentoTemplateDTO dto = new RequerimentoTemplateDTO();
        dto.fields = List.of(field);

        Map<String, Object> mappedField = mapper.toEntity(dto).fields.getFirst();
        Map<?, ?> mappedOption = (Map<?, ?>) ((List<?>) mappedField.get("options")).getFirst();

        assertEquals("field-id", mappedField.get("id"));
        assertEquals("Emprego", mappedOption.get("label"));
        assertEquals("emprego", mappedOption.get("value"));
    }
}
