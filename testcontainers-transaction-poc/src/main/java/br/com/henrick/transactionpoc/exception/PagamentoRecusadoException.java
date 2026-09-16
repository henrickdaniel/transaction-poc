package br.com.henrick.transactionpoc.exception;

public class PagamentoRecusadoException extends RuntimeException {
    public PagamentoRecusadoException(String message) {
        super(message);
    }
}