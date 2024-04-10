package com.iskeru.javapix.pix;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PixTransaction(
        BigDecimal valorTransacao,
        String currency,
        String idTransacao
) {
}