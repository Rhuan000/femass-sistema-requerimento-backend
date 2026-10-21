package org.femass.requerimento.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_event")
public class ApprovalEvent {
    @Id public UUID id;
    public UUID submissionId;
    public UUID stepId;
    public UUID actorId;
    public String actorName;
    public String action;
    @Column(length = 2000) public String description;
    public Instant createdAt;
}
