package br.com.henrick.transactionpoc.client;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class DummyGatewayPagamentoClient implements GatewayPagamentoClient {

    @Override
    public boolean autorizar(BigDecimal valor) {
        // Implementação mock/dummy padrão para o contexto do Spring subir
        return true;
    }

    @Override
    public String autorizarComIdempotencia(BigDecimal valor, String idempotencyKey) {
        return "";
    }

    @Override
    public void estornar(String transacaoId) {

    }
}