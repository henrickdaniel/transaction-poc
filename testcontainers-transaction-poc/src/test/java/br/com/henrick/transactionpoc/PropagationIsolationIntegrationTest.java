package br.com.henrick.transactionpoc;

import br.com.henrick.transactionpoc.client.GatewayPagamentoClient;
import br.com.henrick.transactionpoc.repository.AuditLogRepository;
import br.com.henrick.transactionpoc.service.OperacaoFinanceiraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
class PropagationIsolationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private OperacaoFinanceiraService operacaoFinanceiraService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private GatewayPagamentoClient pagamentoClient;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
    }

    @Test
    @DisplayName("Garante que o log em REQUIRES_NEW seja gravado mesmo quando a transacao principal sofre Rollback")
    void deveSalvarLogDeAuditoriaMesmoComRollbackNaTransacaoPai() {
        // Act & Assert: A operação principal lança exceção e faz rollback
        assertThrows(RuntimeException.class, () -> {
            operacaoFinanceiraService.executarOperacaoComFalha(1L);
        });

        // Assert: A transação em REQUIRES_NEW comitou de forma independente!
        assertThat(auditLogRepository.count()).isEqualTo(1);
        assertThat(auditLogRepository.findAll().get(0).getAction())
                .contains("Tentativa de operacao financeira na conta: 1");
    }
}