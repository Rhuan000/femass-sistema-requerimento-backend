package org.femass.requerimento.resources;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.femass.requerimento.dtos.RequerimentoTemplateDTO;
import org.femass.requerimento.mappers.RequerimentoTemplateMapper;
import org.femass.requerimento.services.RequerimentoTemplateService;
import java.util.List;
import java.util.UUID;

@Path("/templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RequerimentoTemplateResource {
    @Inject RequerimentoTemplateService service;
    @Inject RequerimentoTemplateMapper mapper;

    @GET
    @Authenticated
    public List<RequerimentoTemplateDTO> listActive() {
        return service.listActive().stream().map(mapper::toDTO).toList();
    }

    @GET
    @Path("/management")
    @RolesAllowed({"DIRETOR", "COORDENADOR", "COODERNADOR", "SECRETARIA"})
    public List<RequerimentoTemplateDTO> listAll() {
        return service.listAll().stream().map(mapper::toDTO).toList();
    }

    @GET
    @Path("/{id}")
    @Authenticated
    public RequerimentoTemplateDTO get(@PathParam("id") UUID id) {
        return mapper.toDTO(service.get(id));
    }

    @POST
    @RolesAllowed({"DIRETOR", "COORDENADOR", "COODERNADOR", "SECRETARIA"})
    public Response create(RequerimentoTemplateDTO dto) {
        return Response.status(Response.Status.CREATED)
            .entity(mapper.toDTO(service.create(mapper.toEntity(dto)))).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"DIRETOR", "COORDENADOR", "COODERNADOR", "SECRETARIA"})
    public RequerimentoTemplateDTO update(@PathParam("id") UUID id, RequerimentoTemplateDTO dto) {
        var entity = mapper.toEntity(dto);
        entity.id = id;
        return mapper.toDTO(service.update(entity));
    }

    @POST
    @Path("/{id}/submit-approval")
    @RolesAllowed({"DIRETOR", "COORDENADOR", "COODERNADOR", "SECRETARIA"})
    public RequerimentoTemplateDTO submitApproval(@PathParam("id") UUID id) {
        return mapper.toDTO(service.submitForApproval(id));
    }

    @POST
    @Path("/{id}/approve")
    @RolesAllowed("DIRETOR")
    public RequerimentoTemplateDTO approve(@PathParam("id") UUID id) {
        return mapper.toDTO(service.approve(id));
    }

    @POST
    @Path("/{id}/return-for-editing")
    @RolesAllowed("DIRETOR")
    public RequerimentoTemplateDTO returnForEditing(@PathParam("id") UUID id) {
        return mapper.toDTO(service.returnForEditing(id));
    }

    @POST
    @Path("/{id}/activate")
    @RolesAllowed("DIRETOR")
    public RequerimentoTemplateDTO activate(@PathParam("id") UUID id) {
        return mapper.toDTO(service.activate(id));
    }

    @POST
    @Path("/{id}/deactivate")
    @RolesAllowed("DIRETOR")
    public RequerimentoTemplateDTO deactivate(@PathParam("id") UUID id) {
        return mapper.toDTO(service.deactivate(id));
    }
}
