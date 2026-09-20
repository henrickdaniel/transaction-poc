package br.com.henrick.transactionpoc;

import br.com.henrick.transactionpoc.model.OutboxEvent;
import br.com.henrick.transactionpoc.model.Pedido;
import br.com.henrick.transactionpoc.repository.OutboxEventRepository;
import br.com.henrick.transactionpoc.repository.PedidoRepository;
import br.com.henrick.transactionpoc.service.OutboxPublisherService;
import br.com.henrick.transactionpoc.service.PedidoOutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class TransactionalOutboxIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1")
    );

    @DynamicPropertySource
    static void overrideKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private PedidoOutboxService pedidoOutboxService;

    @Autowired
    private OutboxPublisherService publisherService;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private OutboxEventRepository outboxRepository;

    // Fila em memória para capturar eventos consumidos pelo Listener no teste
    private static final BlockingQueue<String> mensagensRecebidas = new LinkedBlockingQueue<>();

    @KafkaListener(topics = "pedidos-v1", groupId = "test-group")
    public void escutarTopicoPedidos(String payload) {
        mensagensRecebidas.add(payload);
    }

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        pedidoRepository.deleteAll();
        mensagensRecebidas.clear();
    }

    @Test
    @DisplayName("Deve salvar Pedido e OutboxEvent no PG, publicar no Kafka e alterar status para PROCESSED")
    void deveExecutarFluxoCompletoDeOutboxEEnvioAoKafka() throws InterruptedException {
        // 1. ARRANGE & ACT: Cria o pedido + outbox_event (PENDING)
        Pedido pedido = pedidoOutboxService.criarPedidoComOutbox("Henrick", new BigDecimal("1200.00"));

        // Asserção intermediária: O evento foi criado como PENDING no banco
        assertThat(outboxRepository.findAll()).hasSize(1);
        assertThat(outboxRepository.findAll().get(0).getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PENDING);

        // 2. ACT: Dispara o publisher (Relay) para publicar no Kafka real do Testcontainers
        publisherService.processarEventosPendentes();

        // 3. ASSERT 1: Valida se o status na Outbox mudou para PROCESSED no PostgreSQL
        assertThat(outboxRepository.findAll().get(0).getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PROCESSED);

        // 4. ASSERT 2: Valida se a mensagem REALMENTE trafegou pelo Kafka e foi recebida pelo Consumer
        await().atMost(Duration.ofSeconds(200)).untilAsserted(() -> {
            String payloadConsumido = mensagensRecebidas.poll(1, TimeUnit.SECONDS);
            assertThat(payloadConsumido).isNotNull();
            assertThat(payloadConsumido).contains(pedido.getId().toString());
            assertThat(payloadConsumido).contains("Henrick");
        });
    }
}