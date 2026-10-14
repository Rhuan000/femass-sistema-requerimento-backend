package org.femass.requerimento.services;

import org.femass.requerimento.entities.RequerimentoTemplate;
import org.femass.requerimento.entities.TemplateStatus;
import org.femass.requerimento.exceptions.BusinessValidationException;
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
        CurrentUserService currentUser = mock(CurrentUserService.class);
        var diretor = new org.femass.requerimento.entities.Usuario();
        diretor.ativo = true;
        var papel = new org.femass.requerimento.entities.Papel();
        papel.nome = "DIRETOR";
        diretor.adicionarPapel(papel);
        when(currentUser.get()).thenReturn(diretor);
        currentUserField.set(service, currentUser);
        template = new RequerimentoTemplate();
        template.id = UUID.randomUUID();
        template.status = TemplateStatus.PENDENTE;
        template.isActive = false;
        when(repository.findById(template.id)).thenReturn(template);
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
}
