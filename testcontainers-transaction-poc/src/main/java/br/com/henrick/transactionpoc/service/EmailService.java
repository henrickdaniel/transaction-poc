package br.com.henrick.transactionpoc.service;

import br.com.henrick.transactionpoc.model.Pedido;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    public void enviarConfirmacao(Pedido pedido) {
        // Simulação de envio de e-mail I/O
        System.out.println("E-mail enviado para o pedido: " + pedido.getId());
    }
}