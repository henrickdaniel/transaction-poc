package br.com.henrick.transactionpoc.service;

import br.com.henrick.transactionpoc.model.AuditLog;
import br.com.henrick.transactionpoc.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InsecureAuditService {

    private final AuditLogRepository repository;

    // Método NÃO transacional chamando método transacional interno
    public void processarLogComAutoInvocacao(String acao) {
        // A chamada abaixo ignora o Proxy do Spring AOP!
        this.salvarLogTransacional(acao);
    }

    @Transactional
    public void salvarLogTransacional(String acao) {
        repository.save(new AuditLog(acao));

        // Simula um erro de negócio
        throw new RuntimeException("Erro forçado após gravação no banco");
    }
}