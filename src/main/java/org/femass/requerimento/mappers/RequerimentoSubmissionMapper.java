package org.femass.requerimento.mappers;

import jakarta.enterprise.context.ApplicationScoped;
import org.femass.requerimento.dtos.RequerimentoSubmissionDTO;
import org.femass.requerimento.entities.RequerimentoSubmission;

import java.util.UUID;

@ApplicationScoped
public class RequerimentoSubmissionMapper {

    public RequerimentoSubmissionDTO toDTO(RequerimentoSubmission entity) {

        RequerimentoSubmissionDTO dto = new RequerimentoSubmissionDTO();

        dto.id = entity.id;
        dto.templateId = entity.templateId;
        dto.submittedBy = entity.submittedBy;
        dto.status = entity.status;
        dto.createdAt = entity.createdAt;

        dto.data = entity.data;

        return dto;
    }

    public RequerimentoSubmission toEntity(RequerimentoSubmissionDTO dto) {

        RequerimentoSubmission entity = new RequerimentoSubmission();

        entity.id = dto.id;
        entity.templateId = dto.templateId;
        entity.submittedBy = dto.submittedBy;
        entity.status = dto.status;
        entity.createdAt = dto.createdAt;

        entity.data = dto.data;

        return entity;
    }
}