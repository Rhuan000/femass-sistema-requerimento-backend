package org.femass.requerimento.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.femass.requerimento.entities.*;
import org.femass.requerimento.exceptions.*;
import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class ApprovalService {
    static final Set<String> ROLES = Set.of("SECRETARIA", "PROFESSOR", "COORDENADOR", "DIRETOR", "ORIENTADOR");
    @Inject EntityManager em;
    @Inject CurrentUserService currentUser;

    public List<ApprovalRule> validateRules(List<ApprovalRule> rules) {
        if (rules == null) return new ArrayList<>();
        if (rules.size() > 30) throw invalid("O fluxo permite no máximo 30 etapas");
        List<ApprovalRule> copy = new ArrayList<>();
        for (ApprovalRule rule : rules) {
            if (rule == null || rule.label == null || rule.label.isBlank() || rule.label.length() > 150)
                throw invalid("Informe um nome de até 150 caracteres para cada etapa");
            if ((rule.userId == null) == (rule.role == null || rule.role.isBlank()))
                throw invalid("Cada etapa deve selecionar um papel OU uma pessoa");
            if (rule.userId == null && !ROLES.contains(rule.role)) throw invalid("Papel de aprovação inválido");
            if (rule.priority != null && (rule.priority < 1 || rule.priority > 3)) throw invalid("Prioridade deve estar entre 1 e 3");
            if (candidates(rule.role, rule.userId).isEmpty()) throw invalid("Etapa sem responsável ativo: " + rule.label);
            ApprovalRule value = new ApprovalRule();
            value.label = rule.label.trim(); value.role = rule.userId == null ? rule.role : null;
            value.userId = rule.userId; value.priority = rule.priority == null ? 2 : rule.priority;
            copy.add(value);
        }
        return copy;
    }

    public List<Usuario> candidates(String role, UUID userId) {
        if (userId != null) {
            Usuario user = em.find(Usuario.class, userId);
            return user != null && Boolean.TRUE.equals(user.ativo) ? List.of(user) : List.of();
        }
        if (role == null) return List.of();
        return em.createQuery("select distinct u from Usuario u join u.papeis up where u.ativo = true and up.papel.nome = :role", Usuario.class)
                .setParameter("role", role).getResultList();
    }

    public List<ApprovalStep> steps(UUID submissionId) {
        return em.createQuery("from ApprovalStep where submissionId = :id order by position", ApprovalStep.class)
                .setParameter("id", submissionId).getResultList();
    }

    /** Executado na mesma transação do envio: o template nunca é consultado nas decisões seguintes. */
    public void start(RequerimentoSubmission submission, RequerimentoTemplate template) {
        List<ApprovalRule> rules = validateRules(template.approvalFlow);
        if (rules.isEmpty()) return; // Compatibilidade com templates anteriores, sem fluxo configurado.
        if (!steps(submission.id).isEmpty()) throw invalid("O requerimento já possui fluxo de aprovação");
        int position = 0;
        for (ApprovalRule rule : rules) {
            ApprovalStep step = new ApprovalStep();
            step.id = UUID.randomUUID(); step.submissionId = submission.id; step.position = position++;
            step.label = rule.label; step.role = rule.role; step.userId = rule.userId; step.priority = rule.priority;
            step.status = step.position == 0 ? "DISPONIVEL" : "PENDENTE";
            em.persist(step);
            if (step.position == 0) notifyStep(submission, step);
        }
        submission.status = "EM_APROVACAO";
        event(submission.id, null, "ENVIADO", "Fluxo de aprovação iniciado", currentUser.get());
        notify(submission, submission.usuario.id, "Requerimento enviado para aprovação");
    }

    public boolean eligible(ApprovalStep step, Usuario user) {
        return user != null && Boolean.TRUE.equals(user.ativo) && "DISPONIVEL".equals(step.status)
                && (step.userId != null ? step.userId.equals(user.id) : user.possuiPapel(step.role));
    }

    public boolean canView(RequerimentoSubmission submission, Usuario user) {
        if (user == null || !Boolean.TRUE.equals(user.ativo)) return false;
        if (user.possuiPapel("DIRETOR") || user.possuiPapel("ADMIN")) return true;
        if (submission.usuario != null && user.id.equals(submission.usuario.id)) return true;
        if (!em.createQuery("from ApprovalEvent where submissionId = :id and actorId = :actor", ApprovalEvent.class)
                .setParameter("id", submission.id).setParameter("actor", user.id).setMaxResults(1).getResultList().isEmpty()) return true;
        return steps(submission.id).stream().anyMatch(step ->
                user.id.equals(step.decidedBy) || (step.userId != null ? user.id.equals(step.userId) : user.possuiPapel(step.role)));
    }

    public void requireView(RequerimentoSubmission submission) {
        if (!canView(submission, currentUser.get())) throw new AuthorizationException("Você não pode consultar este requerimento");
    }

    @Transactional
    public void decide(UUID submissionId, UUID stepId, String action, String reason, UUID targetUserId, Long expectedVersion) {
        // Serializa decisões concorrentes e encaminhamentos da mesma submissão.
        RequerimentoSubmission submission = em.find(RequerimentoSubmission.class, submissionId, LockModeType.PESSIMISTIC_WRITE);
        if (submission == null) throw new ResourceNotFoundException("Requerimento não encontrado");
        ApprovalStep step = em.find(ApprovalStep.class, stepId);
        if (step == null || !submissionId.equals(step.submissionId)) throw new ResourceNotFoundException("Etapa não encontrada");
        Usuario actor = currentUser.get();
        if (!"EM_APROVACAO".equals(submission.status) || !"DISPONIVEL".equals(step.status)) throw invalid("Esta etapa não está disponível");
        if (expectedVersion == null || expectedVersion != step.version) throw new jakarta.ws.rs.ClientErrorException("A etapa foi atualizada. Recarregue o requerimento", 409);
        if ("REASSIGN".equals(action)) {
            if (!eligible(step, actor) && !actor.possuiPapel("DIRETOR")) throw new AuthorizationException("Somente o responsável atual ou o diretor pode encaminhar");
            requireReason(reason);
            if (targetUserId == null || candidates(null, targetUserId).isEmpty()) throw invalid("Selecione uma pessoa ativa");
            String previous = step.userId == null ? step.role : step.userId.toString();
            step.role = null; step.userId = targetUserId;
            // Altera a versão mesmo quando o destino informado for igual ao anterior.
            step.reason = reason.trim();
            event(submissionId, step.id, "ENCAMINHADO", previous + " → " + targetUserId + ": " + reason.trim(), actor);
            notifyStep(submission, step);
            notify(submission, submission.usuario.id, "Etapa encaminhada: " + step.label);
        } else {
            if (!eligible(step, actor)) throw new AuthorizationException("Você não é responsável por esta etapa");
            if (!Set.of("APPROVE", "REJECT").contains(action)) throw invalid("Decisão inválida");
            if ("REJECT".equals(action)) requireReason(reason);
            if (reason != null && reason.length() > 1500) throw invalid("Observação deve ter até 1500 caracteres");
            step.status = "APPROVE".equals(action) ? "APROVADA" : "REJEITADA";
            step.decidedBy = actor.id; step.decidedAt = Instant.now(); step.reason = reason;
            event(submissionId, step.id, step.status, step.label + (reason == null || reason.isBlank() ? "" : ": " + reason), actor);
            List<ApprovalStep> pending = steps(submissionId).stream().filter(s -> "PENDENTE".equals(s.status)).toList();
            if ("REJECT".equals(action)) {
                pending.forEach(s -> s.status = "CANCELADA"); submission.status = "REJEITADO";
            } else if (pending.isEmpty()) submission.status = "APROVADO";
            else { ApprovalStep next = pending.getFirst(); next.status = "DISPONIVEL"; notifyStep(submission, next); }
            notify(submission, submission.usuario.id, step.label + ": " + step.status);
        }
        submission.updatedAt = Instant.now();
    }

    private void requireReason(String reason) {
        if (reason == null || reason.isBlank() || reason.length() > 1500) throw invalid("Informe um motivo de até 1500 caracteres");
    }

    private void notifyStep(RequerimentoSubmission submission, ApprovalStep step) {
        for (Usuario recipient : candidates(step.role, step.userId)) notify(submission, recipient.id, "Aprovação pendente: " + step.label);
    }

    private void notify(RequerimentoSubmission submission, UUID userId, String message) {
        ApprovalNotification notification = new ApprovalNotification();
        notification.id = UUID.randomUUID(); notification.userId = userId; notification.submissionId = submission.id;
        notification.templateId = submission.templateId; notification.message = message; notification.createdAt = Instant.now();
        em.persist(notification);
    }

    private void event(UUID submissionId, UUID stepId, String action, String description, Usuario actor) {
        ApprovalEvent event = new ApprovalEvent();
        event.id = UUID.randomUUID(); event.submissionId = submissionId; event.stepId = stepId;
        event.actorId = actor.id; event.actorName = actor.nome; event.action = action;
        event.description = description; event.createdAt = Instant.now(); em.persist(event);
    }

    private BusinessValidationException invalid(String message) { return new BusinessValidationException(message); }
}
