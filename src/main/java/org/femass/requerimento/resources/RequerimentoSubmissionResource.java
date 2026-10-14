package org.femass.requerimento.resources;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.femass.requerimento.dtos.DocumentoUploadDTO;
import org.femass.requerimento.dtos.RequerimentoSubmissionDTO;
import org.femass.requerimento.entities.RequerimentoSubmission;
import org.femass.requerimento.exceptions.BusinessValidationException;
import org.femass.requerimento.mappers.RequerimentoSubmissionMapper;
import org.femass.requerimento.services.DocumentoService;
import org.femass.requerimento.services.RequerimentoSubmissionService;
import org.femass.requerimento.services.RequerimentoTemplateService;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Path("/submissions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RequerimentoSubmissionResource {

    @Inject
    RequerimentoSubmissionService service;
    @Inject
    RequerimentoSubmissionMapper mapper;
    @Inject
    RequerimentoTemplateService templateService;
    @Inject
    DocumentoService documentoService;

    @POST
    //@RolesAllowed({"ALUNO", "PROFESSOR"})
    public Response createDraft(RequerimentoSubmissionDTO dto) {
        if (dto == null) {
            throw new BusinessValidationException("Os dados do rascunho são obrigatórios");
        }
        RequerimentoSubmission draft = service.createDraft(mapper.toEntity(dto));
        return Response.status(Response.Status.CREATED)
                .entity(toDTO(draft))
                .build();
    }

    @PUT
    @Path("/{id}")
    //@RolesAllowed({"ALUNO", "PROFESSOR"})
    public RequerimentoSubmissionDTO saveDraft(
            @PathParam("id") UUID id,
            RequerimentoSubmissionDTO dto
    ) {
        return toDTO(service.saveDraft(id, dto == null ? null : dto.data));
    }

    @POST
    @Path("/{id}/submit")
    //@RolesAllowed({"ALUNO", "PROFESSOR"})
    public RequerimentoSubmissionDTO submit(@PathParam("id") UUID id) {
        return toDTO(service.submit(id));
    }

    @POST
    @Path("/{id}/documents")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    //@RolesAllowed({"ALUNO", "PROFESSOR"})
    public Response uploadDocument(
            @PathParam("id") UUID id,
            @RestForm("file") FileUpload file
    ) {
        DocumentoUploadDTO document = documentoService.upload(id, file);
        return Response.status(Response.Status.CREATED).entity(document).build();
    }

    @GET
    @Path("/{id}/documents/{documentId}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Authenticated
    public Response downloadDocument(
            @PathParam("id") UUID id,
            @PathParam("documentId") UUID documentId
    ) {
        DocumentoService.DownloadedDocument document = documentoService.download(id, documentId);
        StreamingOutput body = output -> {
            try (var content = document.content()) {
                content.transferTo(output);
            }
        };

        return Response.ok(body, document.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(document.nomeOriginal()))
                .header(HttpHeaders.CONTENT_LENGTH, document.tamanho())
                .build();
    }

    @DELETE
    @Path("/{id}/documents/{documentId}")
    //@RolesAllowed({"ALUNO", "PROFESSOR"})
    public Response deleteDocument(
            @PathParam("id") UUID id,
            @PathParam("documentId") UUID documentId
    ) {
        documentoService.delete(id, documentId);
        return Response.noContent().build();
    }

    @GET
    @Path("/draft/template/{templateId}")
    @Authenticated
    public RequerimentoSubmissionDTO currentUserDraftByTemplate(
            @PathParam("templateId") UUID templateId
    ) {
        return toDTO(service.findCurrentUserDraftByTemplate(templateId));
    }

    @GET
    @Path("/template/{templateId}")
    @Authenticated
    public List<RequerimentoSubmissionDTO> byTemplate(@PathParam("templateId") UUID templateId) {
        return service.findByTemplate(templateId).stream().map(this::toDTO).toList();
    }

    @GET
    @Path("/{id}")
    @Authenticated
    public RequerimentoSubmissionDTO get(@PathParam("id") UUID id) {
        return toDTO(service.get(id));
    }

    private RequerimentoSubmissionDTO toDTO(RequerimentoSubmission submission) {
        return mapper.toDTO(submission, templateService.get(submission.templateId));
    }

    private String contentDisposition(String fileName) {
        String safeName = fileName.replace("\r", "").replace("\n", "").replace("\"", "'");
        String encodedName = URLEncoder.encode(safeName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "attachment; filename=\"" + safeName + "\"; filename*=UTF-8''" + encodedName;
    }
}
