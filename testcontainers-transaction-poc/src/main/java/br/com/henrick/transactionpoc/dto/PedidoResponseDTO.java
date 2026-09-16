package br.com.henrick.transactionpoc.dto;

import br.com.henrick.transactionpoc.model.StatusPedido;

import java.math.BigDecimal;

public record PedidoResponseDTO (Long id, String cliente, BigDecimal valor, StatusPedido status){}
