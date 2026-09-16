package br.com.henrick.transactionpoc.exception;

public class PedidoNaoEncontradoException extends RuntimeException {
    public PedidoNaoEncontradoException(String message) {
        super(message);
    }
}