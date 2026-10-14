package org.femass.requerimento.resources;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.femass.requerimento.dtos.CurrentUserDTO;
import org.femass.requerimento.entities.Usuario;
import org.femass.requerimento.services.CurrentUserService;
import org.femass.requerimento.services.RequerimentoSubmissionService;
import org.femass.requerimento.services.RequerimentoTemplateService;

import java.util.Objects;

@Path("/me")
@Produces(MediaType.APPLICATION_JSON)
public class MeResource {
    @Inject CurrentUserService currentUserService;
    @Inject RequerimentoTemplateService templateService;
    @Inject RequerimentoSubmissionService submissionService;

    @GET
    @Authenticated
    public CurrentUserDTO get() {
        Usuario usuario = currentUserService.get();

        CurrentUserDTO dto = new CurrentUserDTO();
        dto.id = usuario.id;
        dto.nome = usuario.nome;
        dto.email = usuario.email;
        dto.ativo = usuario.ativo;
        dto.papeis = usuario.papeis.stream()
                .map(up -> up.papel == null ? null : up.papel.nome)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        dto.permissions = new CurrentUserDTO.Permissions();
        // Criar template e ver a listagem de gestão seguem a mesma regra no serviço.
        dto.permissions.canCreateTemplate = templateService.podeSolicitar(usuario);
        dto.permissions.canManageTemplates = templateService.podeSolicitar(usuario);
        dto.permissions.canApproveTemplates = templateService.podeDecidir(usuario);
        dto.permissions.canCreateSubmission = submissionService.canCreate(usuario);
        return dto;
    }
}
