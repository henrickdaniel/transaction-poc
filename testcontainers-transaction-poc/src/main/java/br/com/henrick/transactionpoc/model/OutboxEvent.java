package br.com.henrick.transactionpoc.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_event")
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "outbox_seq")
    @SequenceGenerator(name = "outbox_seq", sequenceName = "seq_outbox_event", allocationSize = 50)
    private Long id;

    private String aggregateType; // Ex: "PEDIDO"

    private String aggregateId;   // Ex: ID do pedido

    private String eventType;     // Ex: "PEDIDO_CRIADO"

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    private String payload;       // JSON do evento

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    public enum OutboxStatus {
        PENDING, PROCESSED, FAILED
    }
}