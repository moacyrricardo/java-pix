package com.iskeru.javapix.pix;

import com.github.snksoft.crc.CRC;

public final class PixCRC16 {
    private PixCRC16(){}

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
