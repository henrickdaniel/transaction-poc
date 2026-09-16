package br.com.henrick.transactionpoc;

import br.com.henrick.transactionpoc.client.GatewayPagamentoClient;
import br.com.henrick.transactionpoc.model.Conta;
import br.com.henrick.transactionpoc.repository.ContaRepository;
import br.com.henrick.transactionpoc.service.TransferenciaCorretaService;
import br.com.henrick.transactionpoc.service.TransferenciaIncorretaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
class CheckedExceptionRollbackIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TransferenciaIncorretaService serviceIncorreto;

    @Autowired
    private TransferenciaCorretaService serviceCorreto;

    @Autowired
    private ContaRepository contaRepository;

    @MockitoBean
    private GatewayPagamentoClient pagamentoClient; // Evita falhas no ApplicationContext

    private Long contaId;

    @BeforeEach
    void setUp() {
        contaRepository.deleteAll();
        Conta conta = new Conta("Henrick", new BigDecimal("2000.00"));
        Conta salva = contaRepository.save(conta);
        this.contaId = salva.getId();
    }

    @Test
    @DisplayName("Incorreto: Deve efetuar COMMIT no banco mesmo quando uma Checked Exception for lançada")
    void deveFazerCommitComCheckedExceptionSemRollbackFor() {
        // Act & Assert
        assertThrows(Exception.class, () -> {
            serviceIncorreto.transferir(contaId, new BigDecimal("1500.00"));
        });

        // O saldo FOI alterado no banco de dados (o commit aconteceu inadvertidamente)
        Conta contaAtualizada = contaRepository.findById(contaId).orElseThrow();
        assertThat(contaAtualizada.getSaldo()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Correto: Deve efetuar ROLLBACK ao lançar Checked Exception quando rollbackFor = Exception.class estiver configurado")
    void deveFazerRollbackComCheckedExceptionComRollbackFor() {
        // Act & Assert
        assertThrows(Exception.class, () -> {
            serviceCorreto.transferir(contaId, new BigDecimal("1500.00"));
        });

        // O saldo PERMANECEU intacto devido ao rollback
        Conta contaAtualizada = contaRepository.findById(contaId).orElseThrow();
        assertThat(contaAtualizada.getSaldo()).isEqualByComparingTo("2000.00");
    }
}