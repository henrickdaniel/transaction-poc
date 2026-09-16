package br.com.henrick.transactionpoc.dto;

import java.math.BigDecimal;

public record PedidoResumoDTO(
    Long id,
    String cliente,
    BigDecimal valor
) {}
