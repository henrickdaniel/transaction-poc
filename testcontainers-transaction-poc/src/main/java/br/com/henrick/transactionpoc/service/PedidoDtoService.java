package br.com.henrick.transactionpoc.service;

import br.com.henrick.transactionpoc.dto.PedidoResponseDTO;
import br.com.henrick.transactionpoc.exception.PedidoNaoEncontradoException;
import br.com.henrick.transactionpoc.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PedidoDtoService {

    private final PedidoRepository repository;

    public PedidoResponseDTO buscarPedidoOtimizado(Long id) {
        return repository.findProjectedById(id)
                .orElseThrow(() -> new PedidoNaoEncontradoException("Pedido não encontrado: " + id));
    }
}