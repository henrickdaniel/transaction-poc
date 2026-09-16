package br.com.henrick.transactionpoc;

import br.com.henrick.transactionpoc.client.GatewayPagamentoClient;
import br.com.henrick.transactionpoc.model.Pedido;
import br.com.henrick.transactionpoc.repository.PedidoRepository;
import br.com.henrick.transactionpoc.service.PedidoCorretoService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
class TransactionScopeIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private PedidoCorretoService pedidoService;

    @Autowired
    private PedidoRepository repository;

    @MockitoBean
    private GatewayPagamentoClient pagamentoClient;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Deve executar o I/O fora da transacao e persistir usando TransactionTemplate")
    void deveProcessarPedidoComEscopoReduzido() {
        // Arrange
        when(pagamentoClient.autorizar(any())).thenReturn(true);
        Pedido novoPedido = new Pedido("Cliente X", new BigDecimal("150.00"));

        // Act
        pedidoService.finalizarCompra(novoPedido);

        // Assert
        assertThat(repository.findAll()).hasSize(1);
        verify(pagamentoClient, times(1)).autorizar(any());
    }

    @Test
    @DisplayName("Nao deve abrir transacao de banco se a API de I/O falhar previamente")
    void naoDevePersistirSeApiExternaFalhar() {
        // Arrange: Falha na API antes de qualquer bloco de banco de dados
        when(pagamentoClient.autorizar(any())).thenThrow(new RuntimeException("Gateway offline"));
        Pedido novoPedido = new Pedido("Cliente Y", new BigDecimal("200.00"));

        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            pedidoService.finalizarCompra(novoPedido);
        });

        // O banco continuou intocado e nenhuma conexão foi desperdiçada
        assertThat(repository.findAll()).isEmpty();
    }
}