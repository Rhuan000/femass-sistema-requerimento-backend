package org.femass.requerimento.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "requerimento_template")
public class RequerimentoTemplate {
    @Id
    public UUID id;
    public String name;
    public String description;
    public String category;
    public Integer version;
    public Boolean isActive;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public TemplateStatus status = TemplateStatus.PENDENTE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    public List<Map<String, Object>> fields;

    public Instant createdAt;
    public Instant updatedAt;
}
