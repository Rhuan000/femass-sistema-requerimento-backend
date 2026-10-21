package org.femass.requerimento.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.femass.requerimento.entities.*;
import org.femass.requerimento.exceptions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApprovalServiceTest {
    ApprovalService service;
    Usuario owner, reviewer;
    RequerimentoSubmission submission;
    ApprovalStep first, second;

    Usuario user(String role) {
        Usuario user = new Usuario(); user.id = UUID.randomUUID(); user.nome = role;
        Papel papel = new Papel(); papel.nome = role; user.adicionarPapel(papel); return user;
    }

    @BeforeEach void setup() {
        service = spy(new ApprovalService()); service.em = mock(EntityManager.class);
        service.currentUser = mock(CurrentUserService.class);
        owner = user("ALUNO"); reviewer = user("SECRETARIA");
        when(service.currentUser.get()).thenReturn(reviewer);
        submission = new RequerimentoSubmission(); submission.id = UUID.randomUUID();
        submission.templateId = UUID.randomUUID(); submission.usuario = owner; submission.status = "EM_APROVACAO";
        first = step(0, "SECRETARIA", "DISPONIVEL"); second = step(1, "COORDENADOR", "PENDENTE");
        when(service.em.find(RequerimentoSubmission.class, submission.id, LockModeType.PESSIMISTIC_WRITE)).thenReturn(submission);
        when(service.em.find(ApprovalStep.class, first.id)).thenReturn(first);
        doReturn(List.of(first, second)).when(service).steps(submission.id);
        doReturn(List.of(user("COORDENADOR"))).when(service).candidates("COORDENADOR", null);
    }

    ApprovalStep step(int position, String role, String status) {
        ApprovalStep step = new ApprovalStep(); step.id = UUID.randomUUID(); step.submissionId = submission.id;
        step.label = role; step.role = role; step.position = position; step.status = status; return step;
    }

    @Test void approvingActivatesOnlyNextStageAndNotifies() {
        service.decide(submission.id, first.id, "APPROVE", "Conferido", null, 0L);
        assertEquals("APROVADA", first.status); assertEquals(reviewer.id, first.decidedBy);
        assertEquals("DISPONIVEL", second.status); assertEquals("EM_APROVACAO", submission.status);
        ArgumentCaptor<Object> saved = ArgumentCaptor.forClass(Object.class); verify(service.em, atLeastOnce()).persist(saved.capture());
        assertTrue(saved.getAllValues().stream().anyMatch(o -> o instanceof ApprovalNotification n && n.userId.equals(owner.id)));
    }

    @Test void finalApprovalCompletesSubmission() {
        doReturn(List.of(first)).when(service).steps(submission.id);
        service.decide(submission.id, first.id, "APPROVE", null, null, 0L);
        assertEquals("APROVADO", submission.status);
    }

    @Test void rejectionRequiresReasonAndCancelsFollowingStages() {
        assertThrows(BusinessValidationException.class, () -> service.decide(submission.id, first.id, "REJECT", "", null, 0L));
        assertEquals("DISPONIVEL", first.status);
        service.decide(submission.id, first.id, "REJECT", "Documento inválido", null, 0L);
        assertEquals("REJEITADO", submission.status); assertEquals("CANCELADA", second.status);
    }

    @Test void unrelatedUserCannotDecide() {
        when(service.currentUser.get()).thenReturn(owner);
        assertThrows(AuthorizationException.class, () -> service.decide(submission.id, first.id, "APPROVE", "", null, 0L));
        assertEquals("DISPONIVEL", first.status);
    }

    @Test void staleAndRepeatedDecisionsAreRejected() {
        first.version = 2;
        assertThrows(jakarta.ws.rs.ClientErrorException.class, () -> service.decide(submission.id, first.id, "APPROVE", "", null, 1L));
        first.status = "APROVADA";
        assertThrows(BusinessValidationException.class, () -> service.decide(submission.id, first.id, "APPROVE", "", null, 2L));
    }

    @Test void directorCanReassignWithoutSkippingStage() {
        when(service.currentUser.get()).thenReturn(user("DIRETOR"));
        Usuario target = user("PROFESSOR"); doReturn(List.of(target)).when(service).candidates(null, target.id);
        service.decide(submission.id, first.id, "REASSIGN", "Análise especializada", target.id, 0L);
        assertEquals(target.id, first.userId); assertNull(first.role);
        assertEquals("DISPONIVEL", first.status); assertEquals("PENDENTE", second.status);
        assertFalse(service.eligible(first, reviewer)); assertTrue(service.eligible(first, target));
    }

    @Test void inactiveUserAndPendingStepCannotApprove() {
        reviewer.ativo = false; assertFalse(service.eligible(first, reviewer));
        reviewer.ativo = true; assertFalse(service.eligible(second, reviewer));
    }

    @Test void validatesExclusiveTargetAndRejectsMissingRecipients() {
        ApprovalRule rule = new ApprovalRule(); rule.label = "Conferência";
        assertThrows(BusinessValidationException.class, () -> service.validateRules(List.of(rule)));
        rule.role = "SECRETARIA"; rule.userId = reviewer.id;
        assertThrows(BusinessValidationException.class, () -> service.validateRules(List.of(rule)));
        rule.userId = null; doReturn(List.of()).when(service).candidates("SECRETARIA", null);
        assertThrows(BusinessValidationException.class, () -> service.validateRules(List.of(rule)));
    }

    @Test void startCopiesDefinitionAndKeepsLaterTemplateEditsOutOfSubmission() {
        doReturn(List.of()).when(service).steps(submission.id);
        doReturn(List.of(reviewer)).when(service).candidates("SECRETARIA", null);
        ApprovalRule rule = new ApprovalRule(); rule.label = "Conferir"; rule.role = "SECRETARIA";
        RequerimentoTemplate template = new RequerimentoTemplate(); template.approvalFlow = List.of(rule);
        service.start(submission, template);
        rule.label = "Alterada depois";
        ArgumentCaptor<Object> saved = ArgumentCaptor.forClass(Object.class); verify(service.em, atLeastOnce()).persist(saved.capture());
        ApprovalStep snapshot = (ApprovalStep) saved.getAllValues().stream().filter(ApprovalStep.class::isInstance).findFirst().orElseThrow();
        assertEquals("Conferir", snapshot.label); assertEquals("DISPONIVEL", snapshot.status);
        assertEquals("EM_APROVACAO", submission.status);
    }
}
