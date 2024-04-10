package com.iskeru.javapix.pix;

import lombok.Builder;

@Builder
public record PixTarget(
        String nome,
        String pais,
        String cidade,
        String chavePix
) {
    public static final String BRASIL = "BR";
}