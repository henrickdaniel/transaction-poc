package br.com.henrick.transactionpoc.exception;

public class ProcessamentoPedidoException extends RuntimeException {

    public ProcessamentoPedidoException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessamentoPedidoException(String message) {
        super(message);
    }
}