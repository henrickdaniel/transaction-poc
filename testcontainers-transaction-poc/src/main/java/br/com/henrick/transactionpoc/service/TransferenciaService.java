package br.com.henrick.transactionpoc.service;

import br.com.henrick.transactionpoc.model.Conta;
import br.com.henrick.transactionpoc.repository.ContaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class TransferenciaService {

    private final ContaRepository contaRepository;

    public TransferenciaService(ContaRepository contaRepository) {
        this.contaRepository = contaRepository;
    }

    @Transactional
    public void transferirComErro(Long origenId, Long destinoId, BigDecimal valor) {
        Conta origem = contaRepository.findById(origenId).orElseThrow();
        Conta destino = contaRepository.findById(destinoId).orElseThrow();

        // Debita da origem
        origem.setSaldo(origem.getSaldo().subtract(valor));
        contaRepository.save(origem);

        // Simula uma falha inesperada no sistema antes de creditar no destino
        if (true) {
            throw new RuntimeException("Falha simulada no sistema após o débito!");
        }

        // Credita no destino (nunca será executado)
        destino.setSaldo(destino.getSaldo().add(valor));
        contaRepository.save(destino);
    }
}