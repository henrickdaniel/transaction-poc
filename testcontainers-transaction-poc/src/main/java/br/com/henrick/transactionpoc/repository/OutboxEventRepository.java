package br.com.henrick.transactionpoc.repository;

import br.com.henrick.transactionpoc.model.OutboxEvent;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.hibernate.Timeouts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByStatus(OutboxEvent.OutboxStatus status);

    // SKIP LOCKED permite que workers paralelos pulem registros bloqueados por outras threads
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = Timeouts.SKIP_LOCKED_MILLI + "")// -2 = SKIP LOCKED no Hibernate
    })
    List<OutboxEvent> findTop10ByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus status);
}