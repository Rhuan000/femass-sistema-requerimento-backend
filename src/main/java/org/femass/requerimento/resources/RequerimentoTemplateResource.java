package org.femass.requerimento.resources;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.femass.requerimento.dtos.RequerimentoTemplateDTO;
import org.femass.requerimento.entities.RequerimentoTemplate;
import org.femass.requerimento.entities.Usuario;
import org.femass.requerimento.mappers.RequerimentoTemplateMapper;
import org.femass.requerimento.services.CurrentUserService;
import org.femass.requerimento.services.RequerimentoTemplateService;
import java.util.List;
import java.util.UUID;

@Path("/templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RequerimentoTemplateResource {
    @Inject RequerimentoTemplateService service;
    @Inject RequerimentoTemplateMapper mapper;
    @Inject CurrentUserService currentUserService;

    @GET
    @Authenticated
    public List<RequerimentoTemplateDTO> listActive() {
        Usuario usuario = currentUserService.find().orElse(null);
        return service.listActive().stream().map(template -> toDTO(template, usuario)).toList();
    }

    @GET
    @Path("/management")
    @Authenticated
    public List<RequerimentoTemplateDTO> listAll() {
        Usuario usuario = currentUserService.find().orElse(null);
        return service.listAll().stream().map(template -> toDTO(template, usuario)).toList();
    }

    @GET
    @Path("/{id}")
    @Authenticated
    public RequerimentoTemplateDTO get(@PathParam("id") UUID id) {
        return toDTO(service.get(id));
    }

    @POST
    @Authenticated
    public Response create(RequerimentoTemplateDTO dto) {
        return Response.status(Response.Status.CREATED)
            .entity(toDTO(service.create(mapper.toEntity(dto)))).build();
    }

    @PUT
    @Path("/{id}")
    @Authenticated
    public RequerimentoTemplateDTO update(@PathParam("id") UUID id, RequerimentoTemplateDTO dto) {
        var entity = mapper.toEntity(dto);
        entity.id = id;
        return toDTO(service.update(entity));
    }

    @POST
    @Path("/{id}/submit-approval")
    @Authenticated
    public RequerimentoTemplateDTO submitApproval(@PathParam("id") UUID id) {
        return toDTO(service.submitForApproval(id));
    }

    @POST
    @Path("/{id}/approve")
    @Authenticated
    public RequerimentoTemplateDTO approve(@PathParam("id") UUID id) {
        return toDTO(service.approve(id));
    }

    @POST
    @Path("/{id}/return-for-editing")
    @Authenticated
    public RequerimentoTemplateDTO returnForEditing(@PathParam("id") UUID id) {
        return toDTO(service.returnForEditing(id));
    }

    @POST
    @Path("/{id}/activate")
    @Authenticated
    public RequerimentoTemplateDTO activate(@PathParam("id") UUID id) {
        return toDTO(service.activate(id));
    }

    @POST
    @Path("/{id}/deactivate")
    @Authenticated
    public RequerimentoTemplateDTO deactivate(@PathParam("id") UUID id) {
        return toDTO(service.deactivate(id));
    }

    private RequerimentoTemplateDTO toDTO(RequerimentoTemplate template) {
        return toDTO(template, currentUserService.find().orElse(null));
    }

    private RequerimentoTemplateDTO toDTO(RequerimentoTemplate template, Usuario usuario) {
        RequerimentoTemplateDTO dto = mapper.toDTO(template);
        dto.availableActions = service.availableActions(template, usuario);
        return dto;
    }
}
