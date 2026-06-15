package org.femass.requerimento.dtos;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RequerimentoSubmissionDTO {
    public UUID id;
    public UUID templateId;
    public String submittedBy;
    public String status;
    public Instant createdAt;

    public Map<String, Object> data;
    public List<RequerimentoSubmissionAnswerDTO> answers;
}
