package org.femass.requerimento.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_step", uniqueConstraints = @UniqueConstraint(columnNames = {"submissionId", "position"}))
public class ApprovalStep {
    @Id public UUID id;
    @Version public long version;
    @Column(nullable = false) public UUID submissionId;
    public int position;
    public String label;
    public String role;
    public UUID userId;
    public int priority;
    public String status;
    public UUID decidedBy;
    public Instant decidedAt;
    @Column(length = 2000) public String reason;
}
