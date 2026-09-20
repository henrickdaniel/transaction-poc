package br.com.henrick.transactionpoc.service;

import br.com.henrick.transactionpoc.model.OutboxEvent;
import br.com.henrick.transactionpoc.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxPublisherService {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processarEventosPendentes() {
        List<OutboxEvent> pendentes = outboxRepository.findByStatus(OutboxEvent.OutboxStatus.PENDING);

        for (OutboxEvent evento : pendentes) {
            try {
                // 1. Envio REAL para o Kafka no Testcontainers
                kafkaTemplate.send("pedidos-v1", evento.getAggregateId(), evento.getPayload()).get();

                // 2. Transição do status da Outbox
                evento.setStatus(OutboxEvent.OutboxStatus.PROCESSED);
                evento.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(evento);
            } catch (Exception e) {
                evento.setStatus(OutboxEvent.OutboxStatus.FAILED);
                outboxRepository.save(evento);
            }
        }
    }
}