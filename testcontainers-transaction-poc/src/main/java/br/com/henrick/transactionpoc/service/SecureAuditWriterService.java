package br.com.henrick.transactionpoc.service;

import br.com.henrick.transactionpoc.model.AuditLog;
import br.com.henrick.transactionpoc.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SecureAuditWriterService {

    private final AuditLogRepository repository;

    @Transactional
    public void salvarLogTransacional(String acao) {
        repository.save(new AuditLog(acao));

        // Simula o mesmo erro de negócio
        throw new RuntimeException("Erro forçado após gravação no banco");
    }
}