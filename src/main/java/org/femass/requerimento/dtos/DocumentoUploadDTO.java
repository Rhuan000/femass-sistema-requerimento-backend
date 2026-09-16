package org.femass.requerimento.dtos;

import java.time.Instant;
import java.util.UUID;

public record DocumentoUploadDTO(
        UUID id,
        UUID submissionId,
        String nomeOriginal,
        String contentType,
        long tamanho,
        String bucket,
        String objectName,
        String etag,
        Instant createdAt
) {
}
