package com.iskeru.javapix.pix;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Transaction details for a PIX payment.
 * <p>
 * Holds the monetary amount, optional ISO-4217 currency code (defaults to BRL in the builder logic
 * when omitted or set to {@code "BRL"}), and an optional transaction identifier (TXID) that can be
 * echoed back to aid reconciliation.
 * </p>
 */
@Builder
public record PixTransaction(
        BigDecimal valorTransacao,
        String currency,
        String idTransacao
) {
}