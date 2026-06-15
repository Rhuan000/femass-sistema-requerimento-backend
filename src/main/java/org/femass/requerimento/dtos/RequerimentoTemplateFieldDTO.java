package org.femass.requerimento.dtos;

import java.util.List;
import java.util.UUID;

public class RequerimentoTemplateFieldDTO {
    public String id;
    public String fieldKey;
    public String label;
    public String type;
    public Boolean required;
    public String placeholder;
    public String description;
    public List<SelectOptionDTO> options;
    public Integer position;
}