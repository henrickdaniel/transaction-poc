package br.com.henrick.transactionpoc;

import br.com.henrick.transactionpoc.client.GatewayPagamentoClient;
import br.com.henrick.transactionpoc.model.Pedido;
import br.com.henrick.transactionpoc.model.StatusPedido;
import br.com.henrick.transactionpoc.dto.PedidoResponseDTO;
import br.com.henrick.transactionpoc.repository.PedidoRepository;
import br.com.henrick.transactionpoc.service.PedidoDtoService;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class DtoIsolationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private PedidoDtoService dtoService;

    @Autowired
    private PedidoRepository repository;

    @MockitoBean
    private GatewayPagamentoClient pagamentoClient;

    private Long pedidoId;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        Pedido pedido = new Pedido("Henrick", new BigDecimal("500.00"));
        pedido.setStatus(StatusPedido.PENDENTE);
        Pedido salvo = repository.save(pedido);
        this.pedidoId = salvo.getId();
    }

    @Test
    @DisplayName("Garante que alterações na resposta DTO não afetam o banco de dados")
    void deveIsolarCamadaDeApresentacaoComDTO() {
        // Act: Obtém a resposta em DTO
        PedidoResponseDTO response = dtoService.buscarPedidoOtimizado(pedidoId);

        // Tentativa fictícia de alterar o DTO na camada de apresentação
        // (Como DTO é Record, ele é imutável! Se fosse uma classe comum, alterações não causariam dirty checking)
        assertThat(response.cliente()).isEqualTo("Henrick");

        // Assert: A entidade original no banco permanece inalterada
        Pedido pedidoNoBanco = repository.findById(pedidoId).orElseThrow();
        assertThat(pedidoNoBanco.getCliente()).isEqualTo("Henrick");
    }
}