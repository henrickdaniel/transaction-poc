package br.com.henrick.transactionpoc.service;

import br.com.henrick.transactionpoc.client.GatewayPagamentoClient;
import br.com.henrick.transactionpoc.model.Pedido;
import br.com.henrick.transactionpoc.model.StatusPedido;
import br.com.henrick.transactionpoc.exception.PagamentoRecusadoException;
import br.com.henrick.transactionpoc.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class PedidoCorretoService {

    private final PedidoRepository repository;
    private final GatewayPagamentoClient pagamentoClient;
    private final EmailService emailService;
    private final TransactionTemplate transactionTemplate;

    public void finalizarCompra(Pedido pedido) {
        // 1. I/O Externa: Sem NENHUMA conexão de banco aberta
        boolean aprovado = pagamentoClient.autorizar(pedido.getValor());

        if (!aprovado) {
            throw new PagamentoRecusadoException("Pagamento não autorizado");
        }

        // 2. Transação CURTA: Abre a conexão, grava e commita imediatamente via TransactionTemplate
        Pedido pedidoSalvo = transactionTemplate.execute(status -> {
            pedido.setStatus(StatusPedido.PAGO);
            return repository.save(pedido);
        });

        // 3. I/O Externa pós-gravação: O banco já foi liberado!
        emailService.enviarConfirmacao(pedidoSalvo);
    }
}