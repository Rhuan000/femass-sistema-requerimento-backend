package org.femass.requerimento.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documento")
public class Documento {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    public RequerimentoSubmission submission;

    @Column(nullable = false)
    public String nomeOriginal;

    @Column(nullable = false)
    public String contentType;

    @Column(nullable = false)
    public long tamanho;

    @Column(nullable = false)
    public String bucket;

    @Column(nullable = false, unique = true)
    public String objectName;

    public String etag;

    @Column(nullable = false)
    public Instant createdAt;
}
