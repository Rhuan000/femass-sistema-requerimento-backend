package org.femass.requerimento.resources;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.femass.requerimento.entities.*;
import org.femass.requerimento.services.*;
import java.util.*;

@Path("/approvals")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ApprovalResource {
    @Inject ApprovalService approval;
    @Inject RequerimentoSubmissionService submissions;
    @Inject CurrentUserService currentUser;
    @Inject EntityManager em;

    public record Person(UUID id, String name, List<String> roles) {}
    public record StepView(ApprovalStep step, String responsibleName, boolean canDecide, boolean canReassign) {}
    public record FlowView(List<StepView> steps, List<ApprovalEvent> history) {}
    public record Decision(String action, String reason, UUID targetUserId, Long version) {}

    @GET @Path("/people") @Transactional
    public List<Person> people() {
        currentUser.get();
        return em.createQuery("from Usuario where ativo = true order by nome", Usuario.class).getResultList().stream()
            .map(user -> new Person(user.id, user.nome, user.papeis.stream().map(up -> up.papel.nome).sorted().toList())).toList();
    }

    @GET @Path("/submissions/{id}") @Transactional
    public FlowView flow(@PathParam("id") UUID id) {
        approval.requireView(submissions.get(id));
        Usuario user = currentUser.get();
        List<StepView> steps = approval.steps(id).stream().map(step -> {
            Usuario responsible = step.userId == null ? null : em.find(Usuario.class, step.userId);
            return new StepView(step, responsible == null ? step.role : responsible.nome,
                approval.eligible(step, user), "DISPONIVEL".equals(step.status) && (approval.eligible(step, user) || user.possuiPapel("DIRETOR")));
        }).toList();
        return new FlowView(steps, em.createQuery("from ApprovalEvent where submissionId = :id order by createdAt, id", ApprovalEvent.class).setParameter("id", id).getResultList());
    }

    @POST @Path("/submissions/{id}/steps/{stepId}/decisions") @Transactional
    public FlowView decide(@PathParam("id") UUID id, @PathParam("stepId") UUID stepId, Decision input) {
        if (input == null || input.action() == null) throw new BadRequestException("Informe uma decisão");
        approval.decide(id, stepId, input.action(), input.reason(), input.targetUserId(), input.version());
        return flow(id);
    }

    @GET @Path("/notifications") @Transactional
    public List<ApprovalNotification> notifications() {
        return em.createQuery("from ApprovalNotification where userId = :id order by createdAt desc", ApprovalNotification.class)
                .setParameter("id", currentUser.get().id).setMaxResults(100).getResultList();
    }

    @POST @Path("/notifications/{id}/read") @Transactional
    public void read(@PathParam("id") UUID id) {
        ApprovalNotification notification = em.find(ApprovalNotification.class, id);
        if (notification == null || !currentUser.get().id.equals(notification.userId)) throw new NotFoundException();
        notification.readAt = java.time.Instant.now();
    }
}
