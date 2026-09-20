package br.com.henrick.transactionpoc.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    private UUID id;

    private String aggregateType; // Ex: "PEDIDO"

    private String aggregateId;   // Ex: ID do pedido

    private String eventType;     // Ex: "PEDIDO_CRIADO"

    @Column(columnDefinition = "TEXT")
    private String payload;       // JSON do evento

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    public OutboxEvent(String aggregateType, String aggregateId, String eventType, String payload) {
        this.id = UUID.randomUUID();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public enum OutboxStatus {
        PENDING, PROCESSED, FAILED
    }
}