package br.com.henrick.transactionpoc.repository;

import br.com.henrick.transactionpoc.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    Optional<Pedido> findByIdempotencyKey(String idempotencyKey);
}