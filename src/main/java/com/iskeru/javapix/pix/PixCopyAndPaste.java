package com.iskeru.javapix.pix;

import lombok.Builder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Builder for PIX "copia e cola" (copy and paste) payload strings.
 * <p>
 * This class assembles a static PIX QR payload according to the Banco Central do Brasil
 * specification (QR Code Estático) described in the
 * <a href="https://www.bcb.gov.br/content/estabilidadefinanceira/pix/Regulamento_Pix/II_ManualdePadroesparaIniciacaodoPix.pdf">Manual PIX</a>.
 * Provide the target (merchant/payee) information via {@link PixTarget}, transaction details via
 * {@link PixTransaction}, and an optional free-form message. Then call {@link #generate()} to
 * produce the final EMV-like payload, including a CRC-16/CCITT checksum.
 * </p>
 */
@Builder
public class PixCopyAndPaste {
    /**
     * Convenience constant to convert between BRL with 2 decimals and integer cents representation.
     */
    public static final BigDecimal HUNDRED = new BigDecimal("100");
    private PixTarget to;
    private PixTransaction transfer;
    private String message;

    /** @return returns the PixTarget **/
    public PixTarget to() { return to; }
    /** @return returns the pix transfer **/
    public PixTransaction transfer() { return transfer; }
    /** @return returns the pix message **/
    public String message() { return message; }

    /**
     * Generates a complete PIX payload, including the CRC-16 field.
     *
     * @return the EMV-formatted PIX payload ready to be embedded in a QR code or copied as text
     * @throws IllegalArgumentException if required data is missing or violates basic constraints
     */
    public String generate() {
        return generate(true);
    }

    //visible for test
    /**
     * Generates the PIX payload, optionally skipping the CRC field (useful for tests).
     *
     * @param withCrc whether the CRC-16 field should be appended
     * @return the EMV-formatted PIX payload
     * @throws IllegalArgumentException if required data is missing or violates basic constraints
     */
    protected String generate(boolean withCrc) {
        // Validações (opcional)

        if (transfer == null || transfer.valorTransacao() == null) {
            throw new IllegalArgumentException("Valor da transação inválido");
        }

        if (to == null || to.cidade() != null && to.cidade().length() > 50) {
            throw new IllegalArgumentException("Cidade inválida");
        }

        if (to == null || to.chavePix() == null || to.chavePix().isEmpty()) {
            throw new IllegalArgumentException("Chave Pix inválida");
        }

        // Converte o valor da transação para String
        String valorTransacaoStr = transfer.valorTransacao().multiply(HUNDRED).setScale(0, RoundingMode.HALF_EVEN).toString();

        StringBuilder codeBuilder = new StringBuilder();
        // Cria o código Pix
        add(codeBuilder, Fields.PAYLOAD_FORMAT_INDICATOR, "01");

        StringBuilder merAccInfo = new StringBuilder();
        add(merAccInfo, Fields.MERCHANT_ACCOUNT_INFO_GUI, Fields.MERCHANT_ACCOUNT_INFO_GUI_VALUE);
        add(merAccInfo, Fields.MERCHANT_ACCOUNT_INFO_PIX_KEY, to.chavePix());

        add(codeBuilder, Fields.MERCHANT_ACCOUNT_INFO, merAccInfo.toString());

        add(codeBuilder, Fields.MERCHANT_CATEGORY, Fields.EMPTY_MERCHANT);

        if (transfer != null && (transfer.currency() == null || "BRL".equals(transfer.currency()))) {
            add(codeBuilder, Fields.TRANSACTION_CURRENCY, Fields.TRANSACTION_CURRENCY_BRL);
        }
        add(codeBuilder, Fields.TRANSACTION_VALUE, transfer.valorTransacao().setScale(2).toString());

        if (to != null && to.pais() != null && !to.pais().isEmpty()) {
            add(codeBuilder, Fields.MERCHANT_COUNTRY, to.pais());
        }
        if (to != null && to.nome() != null && !to.nome().isEmpty()) {
            add(codeBuilder, Fields.MERCHANT_NAME, to.nome(), Fields.MERCHANT_NAME_MAXLENGTH);
        }
        if (to != null && to.cidade() != null && !to.cidade().isEmpty()) {
            add(codeBuilder, Fields.MERCHANT_CITY, to.cidade());
        }

        if (transfer.idTransacao() != null && !transfer.idTransacao().isEmpty()) {
            StringBuilder addData = new StringBuilder();
            add(addData, Fields.ADDITIONAL_DATA_TXID, transfer.idTransacao(), Fields.ADDITIONAL_DATA_TXID_MAXLENGTH);
            add(codeBuilder, Fields.ADDITIONAL_DATA, addData.toString());
        }

        if (withCrc) {
            // Formata o CRC16 como uma string hexadecimal
            String partialCode = codeBuilder.toString();
            String crcHex = PixCRC16.crcChecksum(partialCode + Fields.CRC16 + "04");
            add(codeBuilder, Fields.CRC16, crcHex);
        }

        return codeBuilder.toString();
    }
    /**
     * Appends a TLV (tag-length-value) field to the given buffer.
     *
     * @param sb destination buffer
     * @param field 2-character field identifier
     * @param value raw value to be encoded
     */
    public void add(StringBuilder sb, String field, String value) {
        add(sb, field, value, Optional.empty());
    }

    /**
     * Appends a TLV (tag-length-value) field to the given buffer, capping the value length
     * if a maximum is provided by the specification.
     *
     * @param sb destination buffer
     * @param field 2-character field identifier
     * @param value raw value to be encoded
     * @param maxLength optional maximum value length; if value exceeds it, it will be truncated
     */
    public void add(StringBuilder sb, String field, String value, Optional<Integer> maxLength) {
        sb.append(field);
        int valLen = value.length();
        if (valLen < 10) {
            sb.append("0");
        }
        if (maxLength.isEmpty() || maxLength.get() >= valLen) {
            sb.append(valLen);
            sb.append(value);
        } else {
            sb.append(maxLength.get());
            sb.append(value, 0, maxLength.get());
        }
    }

    /**
     * EMV/PIX field identifiers and constants used to build the payload.
     */
    static class Fields {
        public static String PAYLOAD_FORMAT_INDICATOR = "00";
        public static String MERCHANT_ACCOUNT_INFO = "26";

        public static String MERCHANT_ACCOUNT_INFO_GUI = "00";
        public static String MERCHANT_ACCOUNT_INFO_GUI_VALUE = "BR.GOV.BCB.PIX";
        //public static String MERCHANT_ACCOUNT_INFO_GUI_VALUE = "br.gov.bcb.pix";
        public static String MERCHANT_ACCOUNT_INFO_PIX_KEY = "01";

        public static String ADDITIONAL_DATA = "62";

        private static Optional<Integer> ADDITIONAL_DATA_TXID_MAXLENGTH = Optional.of(25);
        public static String ADDITIONAL_DATA_TXID = "05";

        public static String MERCHANT_CATEGORY = "52";
        public static String EMPTY_MERCHANT = "0000";

        public static String TRANSACTION_CURRENCY = "53";
        public static String TRANSACTION_CURRENCY_BRL = "986";
        public static String TRANSACTION_VALUE = "54";

        public static String MERCHANT_COUNTRY = "58";
        public static String MERCHANT_NAME = "59";
        static Optional<Integer> MERCHANT_NAME_MAXLENGTH = Optional.of(25);
        public static String MERCHANT_CITY = "60";

        /** Field identifier for the CRC-16 result. */
        public static String CRC16 = "63";
    }
}
