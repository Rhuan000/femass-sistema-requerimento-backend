package org.femass.requerimento.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.femass.requerimento.dtos.RequerimentoTemplateDTO;
import org.femass.requerimento.entities.RequerimentoTemplate;
import org.femass.requerimento.services.RequerimentoTemplateService;
import org.femass.requerimento.mappers.RequerimentoTemplateMapper;

import java.util.List;
import java.util.UUID;

@Path("/templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RequerimentoTemplateResource {

    @Inject
    RequerimentoTemplateService service;

    @Inject
    RequerimentoTemplateMapper mapper;

    @GET
    public List<RequerimentoTemplateDTO> listActive() {
        return service.listActive()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @GET
    @Path("/{id}")
    public RequerimentoTemplateDTO get(@PathParam("id") UUID id) {
        return mapper.toDTO(service.get(id));
    }

    @POST
    public Response create(RequerimentoTemplateDTO dto) {

        RequerimentoTemplate entity = mapper.toEntity(dto);
        service.create(entity);

        return Response.status(Response.Status.CREATED)
                .entity(mapper.toDTO(entity))
                .build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") UUID id, RequerimentoTemplateDTO dto) {

        RequerimentoTemplate entity = mapper.toEntity(dto);
        entity.id = id;

        return Response.ok(
                mapper.toDTO(service.update(entity))
        ).build();
    }
}