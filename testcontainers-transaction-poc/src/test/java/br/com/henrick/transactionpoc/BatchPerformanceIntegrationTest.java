package br.com.henrick.transactionpoc;

import br.com.henrick.transactionpoc.model  .OutboxEvent;
import br.com.henrick.transactionpoc.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Slf4j
class BatchPerformanceIntegrationTest {

    public static final int EVENTOS_PARA_PROCESSAR = 2000;
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Aumenta o pool para suportar o teste concorrente
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "20");
    }

    @Autowired
    private OutboxEventRepository outboxRepository;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Test
    @DisplayName("Carga Massiva: Deve inserir 1.000 eventos via Batch Insert em tempo reduzido")
    void deveExecutarBatchInsertComAltaPerformance() {
        int totalRegistros = 1000;
        List<OutboxEvent> eventos = new ArrayList<>();

        for (int i = 0; i < totalRegistros; i++) {
            eventos.add(OutboxEvent.builder()
                    .aggregateType("PEDIDO")
                    .aggregateId(String.valueOf(i))
                    .eventType("PEDIDO_CRIADO")
                    .payload("{\"pedidoId\":" + i + "}")
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        Instant inicio = Instant.now();

        // Salva tudo em uma única transação
        outboxRepository.saveAll(eventos);

        Duration tempoDecorrido = Duration.between(inicio, Instant.now());

        assertThat(outboxRepository.count()).isEqualTo(totalRegistros);
        System.out.println("Tempo total para salvar " + totalRegistros + " registros: " + tempoDecorrido.toMillis() + " ms");

        // Validação de performance: Inserção de 1000 registros via batch deve levar menos de 2 segundos no container
        assertThat(tempoDecorrido.toMillis()).isLessThan(2000);
    }

    @Autowired
    private TransactionTemplate transactionTemplate; // Injete o TransactionTemplate para controlar a transação do Worker

    @Test
    @DisplayName("Locks Concorrentes: Deve falhar sem SKIP LOCKED e passar com SKIP LOCKED")
    void deveGarantirQueThreadsConcorrentesNaoProcessemMesmoEvento2() throws InterruptedException {
        int totalEventos = 100; // Reduzido para o teste rodar rápido
        List<OutboxEvent> eventos = new ArrayList<>();
        for (int i = 0; i < totalEventos; i++) {
            eventos.add(OutboxEvent.builder()
                    .aggregateType("PEDIDO")
                    .aggregateId("REG-" + i)
                    .eventType("AUDIT")
                    .payload("{\"id\":" + i + "}")
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        outboxRepository.saveAll(eventos);

        int numThreads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        Callable<Integer> worker = () -> {
            int processadosPelaThread = 0;

            while (true) {
                // EXECUTANDO DENTRO DE UMA TRANSAÇÃO ATIVA
                Boolean processouLote = transactionTemplate.execute(status -> {
                    // 1. SELECT (Se não tiver SKIP LOCKED, as 5 threads vão ler OS MESMOS 10 REGISTROS ao mesmo tempo!)
                    List<OutboxEvent> pendentes = outboxRepository.findTop10ByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus.PENDING);

                    if (pendentes.isEmpty()) {
                        return false;
                    }

                    for (OutboxEvent e : pendentes) {
                        try {
                            Thread.sleep(50); // Simula I/O prendendo a transação
                        } catch (InterruptedException ignored) {}

                        e.setStatus(OutboxEvent.OutboxStatus.PROCESSED);
                        e.setProcessedAt(LocalDateTime.now());
                        outboxRepository.save(e);
                    }
                    return true;
                });

                if (Boolean.FALSE.equals(processouLote)) {
                    break;
                }
                processadosPelaThread += 10;
            }

            return processadosPelaThread;
        };

        List<Future<Integer>> resultados = new ArrayList<>();
        for (int i = 0; i < numThreads; i++) {
            resultados.add(executor.submit(worker));
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS); // Tempo suficiente para processar 100 itens

        long processadosNoBanco = outboxRepository.findAll().stream()
                .filter(e -> e.getStatus() == OutboxEvent.OutboxStatus.PROCESSED)
                .count();

        // Contagem da soma dos retornos das threads (se houver duplicidade de leitura, a soma dará mais que 100!)
        int somaProcessadaPelasThreads = 0;
        for (Future<Integer> f : resultados) {
            try {
                somaProcessadaPelasThreads += f.get();
            } catch (ExecutionException ignored) {}
        }

        log.info("Soma processada pelas Threads: {}", somaProcessadaPelasThreads);
        log.info("Total no Banco: {}", processadosNoBanco);

        // Valida se as threads NÃO leram registros duplicados
        assertThat(somaProcessadaPelasThreads).isEqualTo(totalEventos);
    }
}