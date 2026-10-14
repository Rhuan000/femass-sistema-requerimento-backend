package org.femass.requerimento.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "requerimento_submission")
public class RequerimentoSubmission {

    @Id
    public UUID id;

    public UUID templateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    public Usuario usuario;

    public String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    public Map<String, Object> data;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    public List<Map<String, Object>> answers;

    public Instant createdAt;

    public Instant updatedAt;

    public Instant submittedAt;

    @OneToMany(mappedBy = "submission", fetch = FetchType.EAGER)
    public List<Documento> documentos = new ArrayList<>();
}
