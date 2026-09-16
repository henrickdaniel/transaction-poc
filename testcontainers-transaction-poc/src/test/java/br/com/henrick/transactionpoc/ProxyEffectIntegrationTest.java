package br.com.henrick.transactionpoc;

import br.com.henrick.transactionpoc.repository.AuditLogRepository;
import br.com.henrick.transactionpoc.service.InsecureAuditService;
import br.com.henrick.transactionpoc.service.SecureAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
class ProxyEffectIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private InsecureAuditService insecureAuditService;

    @Autowired
    private SecureAuditService secureAuditService;

    @Autowired
    private AuditLogRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Auto-invocacao: Deve IGNORAR a transacao e PERSISTIR os dados mesmo com RuntimeException")
    void devePersistirDadosQuandoOcorrerAutoInvocacao() {
        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            insecureAuditService.processarLogComAutoInvocacao("LOG_INSECURE");
        });

        // O Spring ignorou o @Transactional! O registro foi salvo via auto-commit do Hibernate
        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findAll().get(0).getAction()).isEqualTo("LOG_INSECURE");
    }

    @Test
    @DisplayName("Service Externo: Deve APLICAR a transacao e fazer ROLLBACK apos RuntimeException")
    void deveFazerRollbackQuandoChamadoViaServiceExterno() {
        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            secureAuditService.processarLogComServiceExterno("LOG_SECURE");
        });

        // O Proxy AOP funcionou e executou o Rollback do banco real no Testcontainers!
        assertThat(repository.findAll()).isEmpty();
    }
}