package br.com.henrick.transactionpoc.repository;

import br.com.henrick.transactionpoc.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}