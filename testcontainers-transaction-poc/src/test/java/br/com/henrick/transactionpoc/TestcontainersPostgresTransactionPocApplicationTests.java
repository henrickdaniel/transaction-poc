package br.com.henrick.transactionpoc;

import br.com.henrick.transactionpoc.model.Conta;
import br.com.henrick.transactionpoc.repository.ContaRepository;
import br.com.henrick.transactionpoc.service.TransferenciaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
class TestcontainersPostgresTransactionPocApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TransferenciaService transferenciaService;

    @Autowired
    private ContaRepository contaRepository;

    @Test
    void deveFazerRollbackEmAmbienteIsoladoComTestcontainers() {
        Conta origem = contaRepository.save(new Conta(null, "Henrick", new BigDecimal("1000.00")));
        Conta destino = contaRepository.save(new Conta(null, "Maria", new BigDecimal("500.00")));

        assertThrows(RuntimeException.class, () -> {
            transferenciaService.transferirComErro(origem.getId(), destino.getId(), new BigDecimal("200.00"));
        });

        Conta origemPosErro = contaRepository.findById(origem.getId()).orElseThrow();
        assertThat(origemPosErro.getSaldo()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }
}