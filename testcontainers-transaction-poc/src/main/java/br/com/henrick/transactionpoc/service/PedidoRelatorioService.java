package br.com.henrick.transactionpoc.service;

import br.com.henrick.transactionpoc.dto.PedidoResumoDTO;
import br.com.henrick.transactionpoc.model.StatusPedido;
import br.com.henrick.transactionpoc.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // <--- Mantém a conexão otimizada e direcionada à réplica
public class PedidoRelatorioService {

    private final PedidoRepository repository;

    public List<PedidoResumoDTO> gerarRelatorio(StatusPedido status) {
        return repository.buscarResumoPorStatus(status);
    }
}