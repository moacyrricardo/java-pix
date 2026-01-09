package com.iskeru.javapix.pix;

import lombok.Builder;

/**
 * Target (payee/merchant) information used to build a PIX payload.
 * <p>
 * Encapsulates the recipient's display name, country, city, and the mandatory PIX key
 * (phone, email, EVPP, or random key) that identifies the account to receive the funds.
 * </p>
 */
@Builder
public record PixTarget(
        String nome,
        String pais,
        String cidade,
        String chavePix
) {
    /**
     * Country code for Brazil (ISO 3166-1 alpha-2), commonly used in PIX payloads.
     */
    public static final String BRASIL = "BR";
}