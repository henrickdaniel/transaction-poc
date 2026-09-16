package br.com.henrick.transactionpoc.service;

import br.com.henrick.transactionpoc.client.GatewayPagamentoClient;
import br.com.henrick.transactionpoc.dto.PedidoRequest;
import br.com.henrick.transactionpoc.exception.ProcessamentoPedidoException;
import br.com.henrick.transactionpoc.model.Pedido;
import br.com.henrick.transactionpoc.model.StatusPedido;
import br.com.henrick.transactionpoc.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PedidoIdempotenteService {

    private final PedidoRepository repository;
    private final GatewayPagamentoClient pagamentoClient;
    private final TransactionTemplate transactionTemplate;

    public Pedido processarPedidoIdempotente(String idempotencyKey, PedidoRequest request) {

        // 1. Checagem inicial: Já foi processado anteriormente?
        Optional<Pedido> pedidoExistente = repository.findByIdempotencyKey(idempotencyKey);
        if (pedidoExistente.isPresent()) {
            // Retorna o resultado salvo anteriormente sem reexecutar I/O nem banco
            return pedidoExistente.get();
        }

        // 2. I/O Externa: Envia a chave para o Gateway para idempotência do lado deles também
        String transacaoId = pagamentoClient.autorizarComIdempotencia(
                request.getValor(),
                idempotencyKey
        );

        try {
            // 3. Persistência Curta com Banco: Salva o pedido E a chave de idempotência
            return transactionTemplate.execute(status -> {
                Pedido novoPedido = new Pedido();
                novoPedido.setCliente(request.getCliente());
                novoPedido.setValor(request.getValor());
                novoPedido.setStatus(StatusPedido.PAGO);
                novoPedido.setTransacaoId(transacaoId);
                novoPedido.setIdempotencyKey(idempotencyKey); // Garante a constraint no banco

                return repository.save(novoPedido);
            });
        } catch (DataIntegrityViolationException e) {
            // Trata corrida de concorrência se 2 requisições paralelas passaram pela checagem 1
            return repository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("Erro de concorrência na chave de idempotência"));
        } catch (Exception e) {
            // Ação Compensatória (Saga): Se o banco falhar, faz o estorno
            pagamentoClient.estornar(transacaoId);
            throw new ProcessamentoPedidoException("Falha ao salvar. Pagamento estornado.", e);
        }
    }
}