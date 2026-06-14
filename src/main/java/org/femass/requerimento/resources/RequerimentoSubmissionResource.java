package org.femass.requerimento.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.femass.requerimento.dtos.RequerimentoSubmissionDTO;
import org.femass.requerimento.entities.RequerimentoSubmission;
import org.femass.requerimento.mappers.RequerimentoSubmissionMapper;
import org.femass.requerimento.services.RequerimentoSubmissionService;

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

    @POST
    public Response submit(RequerimentoSubmissionDTO dto) {

        RequerimentoSubmission entity = mapper.toEntity(dto);
        service.create(entity);

        return Response.status(Response.Status.CREATED)
                .entity(mapper.toDTO(entity))
                .build();
    }

    @GET
    @Path("/template/{templateId}")
    public List<RequerimentoSubmissionDTO> byTemplate(@PathParam("templateId") UUID templateId) {
        return service.findByTemplate(templateId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @GET
    @Path("/{id}")
    public RequerimentoSubmissionDTO get(@PathParam("id") UUID id) {
        return mapper.toDTO(service.get(id));
    }
}