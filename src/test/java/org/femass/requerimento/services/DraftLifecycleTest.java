package org.femass.requerimento.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.femass.requerimento.entities.*;
import org.femass.requerimento.exceptions.BusinessValidationException;
import org.femass.requerimento.repositories.*;
import org.femass.requerimento.validators.RequerimentoSubmissionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DraftLifecycleTest {
    RequerimentoSubmissionService service;
    Usuario owner;
    RequerimentoTemplate template;
    RequerimentoSubmission draft;

    @BeforeEach void setup() {
        service = new RequerimentoSubmissionService();
        service.repository = mock(RequerimentoSubmissionRepository.class);
        service.templateRepository = mock(RequerimentoTemplateRepository.class);
        service.currentUserService = mock(CurrentUserService.class);
        service.approvalService = mock(ApprovalService.class);
        service.validator = mock(RequerimentoSubmissionValidator.class);
        service.em = mock(EntityManager.class);
        owner = new Usuario(); owner.id = UUID.randomUUID();
        template = new RequerimentoTemplate(); template.id = UUID.randomUUID(); template.isActive = true;
        template.fields = List.of(Map.of("fieldKey", "motivo", "label", "Motivo", "position", 1));
        draft = new RequerimentoSubmission(); draft.id = UUID.randomUUID(); draft.templateId = template.id;
        draft.usuario = owner; draft.status = "RASCUNHO"; draft.data = Map.of("motivo", "Preenchimento existente");
        when(service.currentUserService.get()).thenReturn(owner);
        when(service.templateRepository.findById(template.id)).thenReturn(template);
        when(service.em.find(Usuario.class, owner.id, LockModeType.PESSIMISTIC_WRITE)).thenReturn(owner);
        when(service.em.find(RequerimentoSubmission.class, draft.id, LockModeType.PESSIMISTIC_WRITE)).thenReturn(draft);
    }

    @Test void retryReusesExistingDraftWithoutOverwritingAnswers() {
        when(service.repository.findDraftByTemplateAndUsuario(template.id, owner.id, "RASCUNHO")).thenReturn(draft);
        RequerimentoSubmission input = new RequerimentoSubmission(); input.templateId = template.id; input.data = Map.of();
        assertSame(draft, service.createDraft(input));
        assertEquals("Preenchimento existente", draft.data.get("motivo"));
        verify(service.repository, never()).persist(any(RequerimentoSubmission.class));
        verify(service.em).find(Usuario.class, owner.id, LockModeType.PESSIMISTIC_WRITE);
    }

    @Test void submitTransitionsSameEntityAndDoesNotCreateCopy() {
        UUID originalId = draft.id;
        assertSame(draft, service.submit(originalId));
        assertEquals(originalId, draft.id); assertEquals("ENVIADO", draft.status);
        assertNotNull(draft.submittedAt); assertEquals(1, draft.answers.size());
        verify(service.repository, never()).persist(any(RequerimentoSubmission.class));
        verify(service.approvalService).start(draft, template);
    }

    @Test void autosaveAndRepeatedSubmitCannotReopenSentSubmission() {
        service.submit(draft.id);
        assertThrows(BusinessValidationException.class, () -> service.saveDraft(draft.id, Map.of()));
        assertThrows(BusinessValidationException.class, () -> service.submit(draft.id));
        assertEquals("ENVIADO", draft.status);
        assertEquals("Preenchimento existente", draft.data.get("motivo"));
        verify(service.approvalService, times(1)).start(draft, template);
    }
}
