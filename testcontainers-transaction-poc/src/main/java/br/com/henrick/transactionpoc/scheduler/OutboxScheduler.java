package br.com.henrick.transactionpoc.scheduler;

import br.com.henrick.transactionpoc.service.OutboxPublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxPublisherService publisherService;

    // Dispara a cada 5 segundos (5000ms)
    @Scheduled(fixedDelay = 5000)
    public void executarRelay() {
        publisherService.processarEventosPendentes();
    }
}