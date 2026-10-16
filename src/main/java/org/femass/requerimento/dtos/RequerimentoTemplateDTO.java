package org.femass.requerimento.dtos;

import org.femass.requerimento.entities.TemplateStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RequerimentoTemplateDTO {
    public UUID id;
    public String name;
    public String description;
    public String category;
    public Integer version;
    public Boolean isActive;
    public TemplateStatus status;
    public List<RequerimentoTemplateFieldDTO> fields;
    public Instant createdAt;
    public Instant updatedAt;

    /** Ações que o usuário atual pode executar (ex.: approve, edit). Somente leitura. */
    public List<String> availableActions;
}
