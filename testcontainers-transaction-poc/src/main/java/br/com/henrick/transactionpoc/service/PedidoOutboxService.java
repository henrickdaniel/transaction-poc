package br.com.henrick.transactionpoc.service;

import br.com.henrick.transactionpoc.model.OutboxEvent;
import br.com.henrick.transactionpoc.model.Pedido;
import br.com.henrick.transactionpoc.model.StatusPedido;
import br.com.henrick.transactionpoc.repository.OutboxEventRepository;
import br.com.henrick.transactionpoc.repository.PedidoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PedidoOutboxService {

    private final PedidoRepository pedidoRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    @SneakyThrows
    public Pedido criarPedidoComOutbox(String cliente, BigDecimal valor) {
        // 1. Salva a regra de negócio
        Pedido pedido = new Pedido(cliente, valor);
        pedido.setStatus(StatusPedido.PENDENTE);
        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        // 2. Prepara o payload em JSON
        String payloadJson = objectMapper.writeValueAsString(pedidoSalvo);

        // 3. Persiste o evento na mesma transação ACID
        OutboxEvent outboxEvent = new OutboxEvent(
                "PEDIDO",
                pedidoSalvo.getId().toString(),
                "PEDIDO_CRIADO",
                payloadJson
        );
        outboxRepository.save(outboxEvent);

        return pedidoSalvo;
    }
}