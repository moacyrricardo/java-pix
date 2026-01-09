package com.iskeru.javapix.pix;

import com.github.snksoft.crc.CRC;

/**
 * Utilities for computing CRC-16/CCITT checksums used in PIX QR payloads.
 * <p>
 * The Brazilian PIX "copia e cola"/QR payload includes a trailing field with a CRC-16/CCITT
 * checksum (polynomial 0x1021). This class provides a small helper to compute that checksum and
 * format it as an uppercase hexadecimal string as required by the standard.
 * </p>
 */
public final class PixCRC16 {
    private PixCRC16(){}

    /**
     * Computes the CRC-16/CCITT checksum for the given PIX payload prefix.
     * <p>
     * The caller is expected to pass the full payload up to the CRC field identifier and length
     * (e.g. appending {@code "63" + "04"} prior to calculation) so that the checksum matches the
     * specification. The result is returned as an uppercase hexadecimal string with left padding
     * when necessary.
     * </p>
     *
     * @param partialCode full PIX payload up to (but not including) the CRC value itself
     * @return the CRC-16/CCITT checksum encoded as 4 hex characters in uppercase
     */
    public static String crcChecksum(final String partialCode){
        final byte[] byteArray = partialCode.getBytes();

        long ccittCrc = CRC.calculateCRC(CRC.Parameters.CCITT, byteArray);
        return leftPad(Long.toHexString(ccittCrc), 4).toUpperCase();
    }
    /**
     * Inclui zero à esquerda de um código de um campo do QRCode PIX (se necessário),
     * pois todos os códigos devem ter 2 dígitos.
     * @param code código de um campo do QRCode PIX
     * @return o código com um possível zero à esquerda
     */
    private static String leftPad(final String code) {
        return leftPad(code, 2);
    }
    /**
     * Inclui uma determinada quantidade de zeros à esquerda de um valor.
     * @param code código pra incluir zeros à esquerda
     * @param len tamanho máximo da String retornada
     * @return o código com possíveis zeros à esquerda
     */
    private static String leftPad(final String code, final int len) {
        final var format = "%1$" + len + "s";
        return format.formatted(code).replace(' ', '0');
    }
}
