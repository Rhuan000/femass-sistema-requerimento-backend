package org.femass.requerimento.mappers;

import org.femass.requerimento.dtos.RequerimentoSubmissionDTO;
import org.femass.requerimento.entities.RequerimentoSubmission;
import org.femass.requerimento.entities.RequerimentoTemplate;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequerimentoSubmissionMapperTest {

    private final RequerimentoSubmissionMapper mapper =
            new RequerimentoSubmissionMapper();

    @Test
    void shouldIncludeQuestionLabelWithAnswerInTemplateOrder() {
        RequerimentoTemplate template = new RequerimentoTemplate();
        template.fields = List.of(
                Map.of(
                        "fieldKey", "justificativa",
                        "label", "Qual é a justificativa?",
                        "position", 2
                ),
                Map.of(
                        "fieldKey", "disciplina",
                        "label", "Qual é a disciplina?",
                        "position", 1
                )
        );

        RequerimentoSubmission submission = new RequerimentoSubmission();
        submission.data = Map.of(
                "justificativa", "Consulta médica",
                "disciplina", "Cálculo II"
        );

        RequerimentoSubmissionDTO dto = mapper.toDTO(submission, template);

        assertEquals(2, dto.answers.size());
        assertEquals("Qual é a disciplina?", dto.answers.getFirst().label);
        assertEquals("Cálculo II", dto.answers.getFirst().value);
        assertEquals("Qual é a justificativa?", dto.answers.get(1).label);
        assertEquals("Consulta médica", dto.answers.get(1).value);
    }

    @Test
    void shouldPreferStoredQuestionSnapshotOverCurrentTemplateLabel() {
        RequerimentoTemplate template = new RequerimentoTemplate();
        template.fields = List.of(Map.of(
                "fieldKey", "motivo",
                "label", "Pergunta editada",
                "position", 1
        ));

        RequerimentoSubmission submission = new RequerimentoSubmission();
        submission.data = Map.of("motivo", "Resposta");
        submission.answers = List.of(Map.of(
                "fieldKey", "motivo",
                "label", "Pergunta original",
                "value", "Resposta"
        ));

        RequerimentoSubmissionDTO dto = mapper.toDTO(submission, template);

        assertEquals("Pergunta original", dto.answers.getFirst().label);
        assertEquals("Resposta", dto.answers.getFirst().value);
    }
}
