package org.femass.requerimento.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.femass.requerimento.dtos.DocumentoUploadDTO;
import org.femass.requerimento.entities.Documento;
import org.femass.requerimento.entities.RequerimentoSubmission;
import org.femass.requerimento.exceptions.ResourceNotFoundException;
import org.femass.requerimento.mappers.RequerimentoSubmissionMapper;
import org.femass.requerimento.repositories.DocumentoRepository;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class DocumentoService {

    @Inject DocumentoStorageService storageService;
    @Inject RequerimentoSubmissionService submissionService;
    @Inject DocumentoRepository repository;
    @Inject RequerimentoSubmissionMapper mapper;

    @Transactional
    public DocumentoUploadDTO upload(UUID submissionId, FileUpload file) {
        RequerimentoSubmission draft = submissionService.getOwnedDraft(submissionId);
        DocumentoStorageService.StoredDocument stored = storageService.store(submissionId, file);

        Documento documento = new Documento();
        documento.id = UUID.randomUUID();
        documento.submission = draft;
        documento.nomeOriginal = stored.nomeOriginal();
        documento.contentType = stored.contentType();
        documento.tamanho = stored.tamanho();
        documento.bucket = stored.bucket();
        documento.objectName = stored.objectName();
        documento.etag = stored.etag();
        documento.createdAt = Instant.now();

        repository.persist(documento);
        draft.documentos.add(documento);
        draft.updatedAt = Instant.now();
        return mapper.toDocumentoDTO(documento);
    }

    public DownloadedDocument download(UUID submissionId, UUID documentId) {
        submissionService.getOwnedSubmission(submissionId);
        Documento documento = repository.findByIdAndSubmissionId(documentId, submissionId);
        if (documento == null) {
            throw new ResourceNotFoundException("Documento não encontrado");
        }

        return new DownloadedDocument(
                storageService.download(documento.objectName),
                documento.nomeOriginal,
                documento.contentType,
                documento.tamanho
        );
    }

    @Transactional
    public void delete(UUID submissionId, UUID documentId) {
        RequerimentoSubmission draft = submissionService.getOwnedDraft(submissionId);
        Documento documento = repository.findByIdAndSubmissionId(documentId, submissionId);
        if (documento == null) {
            throw new ResourceNotFoundException("Documento não encontrado");
        }

        storageService.delete(documento.objectName);
        draft.documentos.remove(documento);
        repository.delete(documento);
        draft.updatedAt = Instant.now();
    }

    public record DownloadedDocument(
            InputStream content,
            String nomeOriginal,
            String contentType,
            long tamanho
    ) {
    }
}
