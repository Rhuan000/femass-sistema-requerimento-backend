package org.femass.requerimento.services;

import io.minio.ObjectWriteResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.femass.requerimento.clients.MinIOClient;
import org.femass.requerimento.exceptions.BusinessValidationException;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class DocumentoStorageService {

    private static final Map<String, Set<String>> ACCEPTED_TYPES = Map.of(
            "pdf", Set.of("application/pdf"),
            "doc", Set.of("application/msword", "application/octet-stream"),
            "docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/zip", "application/octet-stream"),
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "png", Set.of("image/png"),
            "txt", Set.of("text/plain", "application/octet-stream")
    );

    @Inject
    MinIOClient minIOClient;

    @ConfigProperty(name = "documentos.max-size", defaultValue = "10485760")
    long maxSize;

    public StoredDocument store(UUID submissionId, FileUpload file) {
        validate(file);

        String originalName = file.fileName();
        String safeName = sanitizeFileName(originalName);
        String objectName = submissionId + "/" + UUID.randomUUID() + "-" + safeName;

        try (InputStream content = Files.newInputStream(file.uploadedFile())) {
            ObjectWriteResponse response = minIOClient.upload(
                    objectName,
                    content,
                    file.size(),
                    file.contentType()
            );

            return new StoredDocument(
                    originalName,
                    file.contentType(),
                    file.size(),
                    minIOClient.bucket(),
                    objectName,
                    response.etag()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível ler o documento enviado", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível armazenar o documento no MinIO", exception);
        }
    }

    public void delete(String objectName) {
        try {
            minIOClient.delete(objectName);
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível excluir o documento do MinIO", exception);
        }
    }

    private void validate(FileUpload file) {
        if (file == null || file.fileName() == null || file.fileName().isBlank()) {
            throw new BusinessValidationException("O documento é obrigatório");
        }
        if (file.size() <= 0) {
            throw new BusinessValidationException("O documento está vazio");
        }
        if (file.size() > maxSize) {
            throw new BusinessValidationException("O documento excede o tamanho máximo de " + maxSize + " bytes");
        }

        String extension = extensionOf(file.fileName());
        Set<String> contentTypes = ACCEPTED_TYPES.get(extension);
        String contentType = file.contentType() == null
                ? ""
                : file.contentType().toLowerCase(Locale.ROOT);

        if (contentTypes == null || !contentTypes.contains(contentType)) {
            throw new BusinessValidationException(
                    "Tipo de documento não permitido. Formatos aceitos: "
                            + String.join(", ", ACCEPTED_TYPES.keySet())
            );
        }
    }

    private String extensionOf(String fileName) {
        int separator = fileName.lastIndexOf('.');
        if (separator < 0 || separator == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(separator + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitizeFileName(String fileName) {
        String baseName = fileName.replace('\\', '/');
        baseName = baseName.substring(baseName.lastIndexOf('/') + 1);
        return baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public record StoredDocument(
            String nomeOriginal,
            String contentType,
            long tamanho,
            String bucket,
            String objectName,
            String etag
    ) {
    }
}
