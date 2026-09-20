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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class ChaosOutboxIntegrationTest {

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
        // Timeout reduzido para o teste de chaos não demorar minutos esperando resposta do broker morto
        registry.add("spring.kafka.producer.properties.max.block.ms", () -> "3000");
    }

    @Autowired
    private PedidoOutboxService pedidoOutboxService;

    @Autowired
    private OutboxPublisherService publisherService;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        pedidoRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve manter o pedido seguro e recuperar o envio após o restabelecimento do Kafka")
    void deveRecuperarEnvioDeEventosAposQuedaERetornoDoKafka() {
        // 1. ARRANGE: Criar o pedido (salva no PG com sucesso)
        Pedido pedido = pedidoOutboxService.criarPedidoComOutbox("Chaos Test", new BigDecimal("500.00"));

        assertThat(pedidoRepository.count()).isEqualTo(1);
        assertThat(outboxRepository.findAll().get(0).getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PENDING);

        // 2. CHAOS INJECTION: Pausar o container do Kafka via Docker API
        String containerId = kafka.getContainerId();
        kafka.getDockerClient().pauseContainerCmd(containerId).exec();

        try {
            // 3. ACT: Tenta publicar com o Kafka fora do ar
            publisherService.processarEventosPendentes();

            // 4. ASSERT: O evento deve ter ido para FAILED (ou mantido PENDING), mas o Pedido permanece no Banco!
            OutboxEvent eventoAposFalha = outboxRepository.findAll().get(0);
            assertThat(eventoAposFalha.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.FAILED);
            assertThat(pedidoRepository.count()).isEqualTo(1); // O banco relacional manteve o pedido seguro!

        } finally {
            // 5. RECOVERY: Despausar o container do Kafka para restabelecer a infraestrutura
            kafka.getDockerClient().unpauseContainerCmd(containerId).exec();
        }

        // 6. RETRY: Reprocessar os eventos com falha após o Kafka voltar
        // Reiniciamos o status para PENDING (simulando um job de retry)
        OutboxEvent eventoParaRetry = outboxRepository.findAll().get(0);
        eventoParaRetry.setStatus(OutboxEvent.OutboxStatus.PENDING);
        outboxRepository.save(eventoParaRetry);

        publisherService.processarEventosPendentes();

        // 7. FINAL ASSERT: Evento agora deve ser PROCESSED com o broker restabelecido
        OutboxEvent eventoProcessado = outboxRepository.findAll().get(0);
        assertThat(eventoProcessado.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PROCESSED);
    }
}