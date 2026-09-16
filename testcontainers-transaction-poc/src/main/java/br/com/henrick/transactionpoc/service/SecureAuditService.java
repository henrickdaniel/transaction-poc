package br.com.henrick.transactionpoc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecureAuditService {

    private final SecureAuditWriterService writerService;

    public void processarLogComServiceExterno(String acao) {
        // Chama através da dependência injetada -> Spring AOP intercepta!
        writerService.salvarLogTransacional(acao);
    }
}