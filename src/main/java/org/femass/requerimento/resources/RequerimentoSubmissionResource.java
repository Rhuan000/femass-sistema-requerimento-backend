package org.femass.requerimento.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.femass.requerimento.dtos.RequerimentoSubmissionDTO;
import org.femass.requerimento.entities.RequerimentoSubmission;
import org.femass.requerimento.mappers.RequerimentoSubmissionMapper;
import org.femass.requerimento.services.RequerimentoSubmissionService;
import org.femass.requerimento.services.RequerimentoTemplateService;

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

    @POST
    public Response submit(RequerimentoSubmissionDTO dto) {

        RequerimentoSubmission entity = mapper.toEntity(dto);
        service.create(entity);

        return Response.status(Response.Status.CREATED)
                .entity(mapper.toDTO(entity, templateService.get(entity.templateId)))
                .build();
    }

    @GET
    @Path("/template/{templateId}")
    public List<RequerimentoSubmissionDTO> byTemplate(@PathParam("templateId") UUID templateId) {
        return service.findByTemplate(templateId)
                .stream()
                .map(submission -> mapper.toDTO(
                        submission,
                        templateService.get(submission.templateId)
                ))
                .toList();
    }

    @GET
    @Path("/{id}")
    public RequerimentoSubmissionDTO get(@PathParam("id") UUID id) {
        RequerimentoSubmission submission = service.get(id);
        return mapper.toDTO(submission, templateService.get(submission.templateId));
    }
}
