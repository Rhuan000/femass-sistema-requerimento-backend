package org.femass.requerimento.entities;

import jakarta.persistence.Embeddable;
import java.util.UUID;

/** Uma aprovação por etapa; a posição na lista determina a sequência. */
@Embeddable
public class ApprovalRule {
    public String label;
    public String role;
    public UUID userId;
    public Integer priority;
}
