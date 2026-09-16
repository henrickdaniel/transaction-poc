package br.com.henrick.transactionpoc;

import br.com.henrick.transactionpoc.client.GatewayPagamentoClient;
import br.com.henrick.transactionpoc.model.Pedido;
import br.com.henrick.transactionpoc.model.StatusPedido;
import br.com.henrick.transactionpoc.dto.PedidoResumoDTO;
import br.com.henrick.transactionpoc.repository.PedidoRepository;
import br.com.henrick.transactionpoc.service.PedidoRelatorioService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class PedidoRelatorioServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private PedidoRelatorioService relatorioService;

    @Autowired
    private PedidoRepository repository;

    @MockitoBean
    private GatewayPagamentoClient pagamentoClient; // Evita falha na inicialização do ApplicationContext

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        Pedido p1 = new Pedido("Henrick", new BigDecimal("100.00"));
        p1.setStatus(StatusPedido.PAGO);

        Pedido p2 = new Pedido("Maria", new BigDecimal("250.00"));
        p2.setStatus(StatusPedido.PAGO);

        Pedido p3 = new Pedido("João", new BigDecimal("80.00"));
        p3.setStatus(StatusPedido.PENDENTE);

        repository.saveAll(List.of(p1, p2, p3));
    }

    @Test
    @DisplayName("Deve gerar o relatório trazendo apenas DTOs mapeados do banco sob transação readOnly")
    void deveGerarRelatorioComSucessoEmModoReadOnly() {
        // Act
        List<PedidoResumoDTO> relatorio = relatorioService.gerarRelatorio(StatusPedido.PAGO);

        // Assert
        assertThat(relatorio).hasSize(2);
        assertThat(relatorio)
                .extracting(PedidoResumoDTO::cliente)
                .containsExactlyInAnyOrder("Henrick", "Maria");

        assertThat(relatorio)
                .extracting(PedidoResumoDTO::valor)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactlyInAnyOrder(new BigDecimal("100.00"), new BigDecimal("250.00"));
    }
}