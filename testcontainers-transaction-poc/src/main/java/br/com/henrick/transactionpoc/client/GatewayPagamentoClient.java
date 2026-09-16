package br.com.henrick.transactionpoc.client;

import java.math.BigDecimal;

public interface GatewayPagamentoClient {
    boolean autorizar(BigDecimal valor);

    String autorizarComIdempotencia(BigDecimal valor, String idempotencyKey);

    void estornar(String transacaoId);
}