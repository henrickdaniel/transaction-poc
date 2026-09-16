package br.com.henrick.transactionpoc.repository;

import br.com.henrick.transactionpoc.dto.PedidoResponseDTO;
import br.com.henrick.transactionpoc.dto.PedidoResumoDTO;
import br.com.henrick.transactionpoc.model.Pedido;
import br.com.henrick.transactionpoc.model.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    Optional<Pedido> findByIdempotencyKey(String idempotencyKey);


    @Query("""
        SELECT new br.com.henrick.transactionpoc.dto.PedidoResumoDTO(p.id, p.cliente, p.valor)
        FROM Pedido p
        WHERE p.status = :status
    """)
    List<PedidoResumoDTO> buscarResumoPorStatus(StatusPedido status);

    // O Spring Data faz a projeção direta para o Record sem precisar de "new ..."!
    Optional<PedidoResponseDTO> findProjectedById(Long id);

    // Se preferir JPQL explícito para consultas mais complexas/joins:
    @Query("""
        SELECT p.id as id, p.cliente as cliente, p.valor as valor, p.status as status
        FROM Pedido p
        WHERE p.id = :id
    """)
    Optional<PedidoResponseDTO> buscarDTOExplicitamente(Long id);
}