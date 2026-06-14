package org.femass.requerimento.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    public List<Map<String, Object>> fields;

    public Instant createdAt;
    public Instant updatedAt;
}

