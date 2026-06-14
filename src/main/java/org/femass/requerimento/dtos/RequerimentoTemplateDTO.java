package org.femass.requerimento.dtos;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RequerimentoTemplateDTO {
    public UUID id;
    public String name;
    public String description;
    public String category;
    public Integer version;
    public Boolean isActive;
    public List<RequerimentoTemplateFieldDTO> fields;
    public Instant createdAt;
    public Instant updatedAt;
}