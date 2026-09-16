package br.com.henrick.transactionpoc.service;

import br.com.henrick.transactionpoc.repository.ContaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OperacaoFinanceiraService {

    private final ContaRepository contaRepository;
    private final AuditLogService auditLogService;

    @Transactional(
            isolation = Isolation.REPEATABLE_READ,
            rollbackFor = Exception.class
    )
    public void executarOperacaoComFalha(Long contaId) {
        // 1. Registra auditoria em uma TRANSAÇÃO NOVA (REQUIRES_NEW)
        auditLogService.registrarLog("Tentativa de operacao financeira na conta: " + contaId);

        // 2. Simula uma falha de negócio/sistema na transação principal
        throw new RuntimeException("Falha simulada no processamento financeiro");
    }
}