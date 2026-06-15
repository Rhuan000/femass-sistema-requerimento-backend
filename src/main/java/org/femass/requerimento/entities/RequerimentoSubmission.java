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
@Table(name = "form_submission")
public class RequerimentoSubmission {

    @Id
    public UUID id;

    public UUID templateId;

    public String submittedBy;

    public String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    public Map<String, Object> data;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    public List<Map<String, Object>> answers;

    public Instant createdAt;
}
