package br.com.henrick.transactionpoc.service;

import br.com.henrick.transactionpoc.model.AuditLog;
import br.com.henrick.transactionpoc.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarLog(String acao) {
        AuditLog log = new AuditLog(acao);
        auditLogRepository.save(log);
    }
}