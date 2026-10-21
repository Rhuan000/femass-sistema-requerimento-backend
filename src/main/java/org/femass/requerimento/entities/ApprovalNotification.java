package org.femass.requerimento.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_notification", indexes = @Index(columnList = "userId, createdAt"))
public class ApprovalNotification {
    @Id public UUID id;
    public UUID userId;
    public UUID submissionId;
    public UUID templateId;
    public String message;
    public Instant createdAt;
    public Instant readAt;
}
