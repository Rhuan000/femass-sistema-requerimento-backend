package org.femass.requerimento.services;

import org.femass.requerimento.entities.RequerimentoTemplate;
import org.femass.requerimento.entities.TemplateStatus;
import org.femass.requerimento.exceptions.BusinessValidationException;
import org.femass.requerimento.exceptions.AuthorizationException;
import org.femass.requerimento.entities.Usuario;
import org.femass.requerimento.entities.Papel;
import org.femass.requerimento.repositories.RequerimentoTemplateRepository;
import org.femass.requerimento.validators.RequerimentoTemplateValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TemplateStatusWorkflowTest {
    RequerimentoTemplateService service;
    RequerimentoTemplateRepository repository;
    RequerimentoTemplate template;
    CurrentUserService currentUser;

    @BeforeEach
    void setup() throws Exception {
        service = new RequerimentoTemplateService();
        repository = mock(RequerimentoTemplateRepository.class);
        var field = RequerimentoTemplateService.class.getDeclaredField("repository");
        field.setAccessible(true);
        field.set(service, repository);
        var validatorField = RequerimentoTemplateService.class.getDeclaredField("validator");
        validatorField.setAccessible(true);
        validatorField.set(service, mock(RequerimentoTemplateValidator.class));
        var currentUserField = RequerimentoTemplateService.class.getDeclaredField("currentUserService");
        currentUserField.setAccessible(true);
        currentUser = mock(CurrentUserService.class);
        var diretor = usuarioComPapel("DIRETOR");
        when(currentUser.get()).thenReturn(diretor);
        currentUserField.set(service, currentUser);
        template = new RequerimentoTemplate();
        template.id = UUID.randomUUID();
        template.status = TemplateStatus.PENDENTE;
        template.isActive = false;
        when(repository.findById(template.id)).thenReturn(template);
    }

    private Usuario usuarioComPapel(String nome) {
        var usuario = new Usuario();
        usuario.ativo = true;
        var papel = new Papel();
        papel.nome = nome;
        usuario.adicionarPapel(papel);
        return usuario;
    }

    @Test
    void onlyDirectorCanApproveAndReturnForEditing() {
        for (String role : new String[]{"COORDENADOR", "SECRETARIA", "PROFESSOR"}) {
            when(currentUser.get()).thenReturn(usuarioComPapel(role));
            assertThrows(AuthorizationException.class, () -> service.approve(template.id));
            assertThrows(AuthorizationException.class, () -> service.returnForEditing(template.id));
            assertThrows(AuthorizationException.class, () -> service.activate(template.id));
            assertEquals(TemplateStatus.PENDENTE, template.status);
        }
    }

    @Test
    void coordinatorAndSecretariaCanResubmitButProfessorCannot() {
        template.status = TemplateStatus.EDITANDO;
        for (String role : new String[]{"COORDENADOR", "SECRETARIA"}) {
            when(currentUser.get()).thenReturn(usuarioComPapel(role));
            service.submitForApproval(template.id);
            assertEquals(TemplateStatus.PENDENTE, template.status);
            template.status = TemplateStatus.EDITANDO;
        }
        when(currentUser.get()).thenReturn(usuarioComPapel("PROFESSOR"));
        assertThrows(AuthorizationException.class, () -> service.submitForApproval(template.id));
        assertEquals(TemplateStatus.EDITANDO, template.status);
    }

    @Test
    void inactiveDirectorCannotApprove() {
        var diretor = usuarioComPapel("DIRETOR");
        diretor.ativo = false;
        when(currentUser.get()).thenReturn(diretor);
        assertThrows(AuthorizationException.class, () -> service.approve(template.id));
    }

    @Test
    void approvalAndActivationRequireSeparateSteps() {
        service.approve(template.id);
        assertEquals(TemplateStatus.APROVADO, template.status);
        assertFalse(template.isActive);
        service.activate(template.id);
        assertEquals(TemplateStatus.ATIVO, template.status);
        assertTrue(template.isActive);
        assertEquals(1, template.version);
        service.deactivate(template.id);
        assertEquals(TemplateStatus.DESATIVADO, template.status);
        assertFalse(template.isActive);
    }

    @Test
    void returningToEditingBlocksApprovalUntilResubmission() {
        service.returnForEditing(template.id);
        assertEquals(TemplateStatus.EDITANDO, template.status);
        assertThrows(BusinessValidationException.class, () -> service.approve(template.id));
        service.submitForApproval(template.id);
        assertEquals(TemplateStatus.PENDENTE, template.status);
        service.approve(template.id);
        assertEquals(TemplateStatus.APROVADO, template.status);
    }

    @Test
    void invalidTransitionsAreRejected() {
        assertThrows(BusinessValidationException.class, () -> service.activate(template.id));
        assertThrows(BusinessValidationException.class, () -> service.deactivate(template.id));
        service.approve(template.id);
        assertThrows(BusinessValidationException.class, () -> service.approve(template.id));
    }

    @Test
    void onlyRequestersCanListAllTemplates() {
        for (String role : new String[]{"DIRETOR", "COORDENADOR", "SECRETARIA"}) {
            when(currentUser.get()).thenReturn(usuarioComPapel(role));
            assertDoesNotThrow(() -> service.listAll());
        }
        for (String role : new String[]{"ALUNO", "PROFESSOR"}) {
            when(currentUser.get()).thenReturn(usuarioComPapel(role));
            assertThrows(AuthorizationException.class, () -> service.listAll());
        }
    }

    @Test
    void invalidFieldsAreRejectedAsBusinessValidation() {
        var validator = new RequerimentoTemplateValidator();
        assertThrows(BusinessValidationException.class, () -> validator.validateFields(java.util.List.of()));
        assertThrows(BusinessValidationException.class, () -> validator.validateFields(java.util.List.of(
                java.util.Map.of("fieldKey", "nome", "type", "text", "label", "Nome"),
                java.util.Map.of("fieldKey", "nome", "type", "text", "label", "Nome"))));
    }

    @Test
    void invalidFieldsReportEveryErrorByField() {
        var validator = new RequerimentoTemplateValidator();
        var error = assertThrows(BusinessValidationException.class, () -> validator.validateFields(java.util.List.of(
                java.util.Map.of("fieldKey", "nome", "type", "text", "label", "Nome"),
                java.util.Map.of("fieldKey", "nome", "type", "", "label", ""))));
        assertEquals(java.util.Set.of("fields[1].fieldKey", "fields[1].type", "fields[1].label"),
                error.getFieldErrors().keySet());
    }

    @Test
    void availableActionsFollowRoleAndStatus() {
        var diretor = usuarioComPapel("DIRETOR");
        var secretaria = usuarioComPapel("SECRETARIA");
        var aluno = usuarioComPapel("ALUNO");

        template.status = TemplateStatus.PENDENTE;
        assertEquals(java.util.List.of("approve", "return-for-editing"), service.availableActions(template, diretor));
        assertEquals(java.util.List.of(), service.availableActions(template, secretaria));

        template.status = TemplateStatus.EDITANDO;
        assertEquals(java.util.List.of("edit", "submit-approval"), service.availableActions(template, secretaria));
        assertEquals(java.util.List.of("edit", "submit-approval"), service.availableActions(template, diretor));

        template.status = TemplateStatus.APROVADO;
        assertEquals(java.util.List.of("activate"), service.availableActions(template, diretor));
        template.status = TemplateStatus.DESATIVADO;
        assertEquals(java.util.List.of("activate"), service.availableActions(template, diretor));
        template.status = TemplateStatus.ATIVO;
        assertEquals(java.util.List.of("deactivate"), service.availableActions(template, diretor));

        for (TemplateStatus status : TemplateStatus.values()) {
            template.status = status;
            assertEquals(java.util.List.of(), service.availableActions(template, aluno));
            assertEquals(java.util.List.of(), service.availableActions(template, null));
        }
    }

    @Test
    void everyAvailableActionIsAcceptedByTheService() {
        // Garante que availableActions não oferece nada que o serviço recusaria.
        for (String role : new String[]{"DIRETOR", "COORDENADOR", "SECRETARIA", "PROFESSOR"}) {
            var usuario = usuarioComPapel(role);
            when(currentUser.get()).thenReturn(usuario);
            for (TemplateStatus status : TemplateStatus.values()) {
                template.status = status;
                for (String action : service.availableActions(template, usuario)) {
                    template.status = status;
                    switch (action) {
                        case "edit" -> {
                            var changes = new RequerimentoTemplate();
                            changes.id = template.id;
                            changes.fields = java.util.List.of();
                            assertDoesNotThrow(() -> service.update(changes), role + " " + status + " " + action);
                        }
                        case "submit-approval" -> assertDoesNotThrow(() -> service.submitForApproval(template.id));
                        case "approve" -> assertDoesNotThrow(() -> service.approve(template.id));
                        case "return-for-editing" -> assertDoesNotThrow(() -> service.returnForEditing(template.id));
                        case "activate" -> assertDoesNotThrow(() -> service.activate(template.id));
                        case "deactivate" -> assertDoesNotThrow(() -> service.deactivate(template.id));
                        default -> fail("Ação desconhecida: " + action);
                    }
                }
            }
        }
    }
}
