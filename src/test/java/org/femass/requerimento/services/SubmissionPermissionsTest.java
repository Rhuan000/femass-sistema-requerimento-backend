package org.femass.requerimento.services;

import org.femass.requerimento.entities.RequerimentoSubmission;
import org.femass.requerimento.entities.RequerimentoTemplate;
import org.femass.requerimento.entities.Usuario;
import org.femass.requerimento.exceptions.BusinessValidationException;
import org.femass.requerimento.validators.RequerimentoSubmissionValidator;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SubmissionPermissionsTest {
    RequerimentoSubmissionService service = new RequerimentoSubmissionService();

    private Usuario usuario() {
        var usuario = new Usuario();
        usuario.id = UUID.randomUUID();
        return usuario;
    }

    private RequerimentoSubmission submission(Usuario dono, String status) {
        var submission = new RequerimentoSubmission();
        submission.usuario = dono;
        submission.status = status;
        return submission;
    }

    @Test
    void onlyOwnerCanEditAndSubmitDraft() {
        var dono = usuario();
        var outro = usuario();
        var rascunho = submission(dono, RequerimentoSubmissionService.STATUS_RASCUNHO);

        assertTrue(service.canEdit(rascunho, dono));
        assertTrue(service.canSubmit(rascunho, dono));
        assertFalse(service.canEdit(rascunho, outro));
        assertFalse(service.canSubmit(rascunho, outro));
        assertFalse(service.canEdit(rascunho, null));
    }

    @Test
    void sentSubmissionCannotBeEditedOrResubmitted() {
        var dono = usuario();
        var enviado = submission(dono, RequerimentoSubmissionService.STATUS_ENVIADO);

        assertFalse(service.canEdit(enviado, dono));
        assertFalse(service.canSubmit(enviado, dono));
    }

    @Test
    void validatorReportsEveryInvalidFieldByFieldKey() {
        var template = new RequerimentoTemplate();
        template.fields = List.of(
                Map.of("fieldKey", "nome", "type", "text", "required", true),
                Map.of("fieldKey", "idade", "type", "number", "required", false),
                Map.of("fieldKey", "email", "type", "email", "required", false));
        var submission = new RequerimentoSubmission();
        submission.data = new HashMap<>(Map.of("idade", "abc", "email", "sem-arroba", "extra", "x"));

        var error = assertThrows(BusinessValidationException.class,
                () -> new RequerimentoSubmissionValidator().validate(template, submission));

        assertEquals(Set.of("nome", "idade", "email", "extra"), error.getFieldErrors().keySet());
        assertEquals("Campo obrigatório", error.getFieldErrors().get("nome"));
    }
}
