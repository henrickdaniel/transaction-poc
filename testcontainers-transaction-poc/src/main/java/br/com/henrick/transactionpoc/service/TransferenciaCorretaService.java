package br.com.henrick.transactionpoc.service;

import br.com.henrick.transactionpoc.model.Conta;
import br.com.henrick.transactionpoc.repository.ContaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferenciaCorretaService {

    private final ContaRepository repository;

    // ✅ Garante rollback para Checked Exceptions, Unchecked Exceptions e Errors
    @Transactional(rollbackFor = Exception.class)
    public void transferir(Long contaId, BigDecimal valor) throws Exception {
        Conta conta = repository.findById(contaId).orElseThrow();
        conta.setSaldo(conta.getSaldo().subtract(valor));
        repository.save(conta);

        if (valor.compareTo(new BigDecimal("1000")) > 0) {
            throw new Exception("Falha ao notificar sistema de auditoria legado");
        }
    }
}