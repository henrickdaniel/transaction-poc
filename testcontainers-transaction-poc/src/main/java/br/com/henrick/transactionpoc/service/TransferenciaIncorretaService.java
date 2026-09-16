package br.com.henrick.transactionpoc.service;

import br.com.henrick.transactionpoc.model.Conta;
import br.com.henrick.transactionpoc.repository.ContaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferenciaIncorretaService {

    private final ContaRepository repository;

    @Transactional // ⚠️ PERIGO: Não faz rollback para Checked Exceptions por padrão!
    public void transferir(Long contaId, BigDecimal valor) throws Exception {
        Conta conta = repository.findById(contaId).orElseThrow();
        conta.setSaldo(conta.getSaldo().subtract(valor));
        repository.save(conta);

        // Simulando I/O legado que lança uma Checked Exception
        if (valor.compareTo(new BigDecimal("1000")) > 0) {
            throw new Exception("Falha ao notificar sistema de auditoria legado");
        }
        // O Spring executará o COMMIT da alteração do saldo mesmo após a Exception!
    }
}