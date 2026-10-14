package org.femass.requerimento.dtos;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RequerimentoSubmissionDTO {
    public UUID id;
    public UUID templateId;
    public String usuarioId;
    public String status;
    public Instant createdAt;
    public Instant updatedAt;
    public Instant submittedAt;

    public Map<String, Object> data;
    public List<RequerimentoSubmissionAnswerDTO> answers;
    public List<DocumentoUploadDTO> documentos;
}
