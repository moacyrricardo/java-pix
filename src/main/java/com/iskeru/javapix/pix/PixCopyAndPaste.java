package com.iskeru.javapix.pix;

import lombok.Builder;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Optional;
import java.util.regex.Pattern;

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

        if (to == null || to.chavePix() == null || to.chavePix().isEmpty()) {
            throw new IllegalArgumentException("Chave Pix inválida");
        }

        // 59 and 60 are mandatory
        String nome = toAscii(to.nome());
        if (nome.isEmpty()) {
            throw new IllegalArgumentException("Nome do recebedor é obrigatório");
        }
        String cidade = toAscii(to.cidade());
        if (cidade.isEmpty()) {
            throw new IllegalArgumentException("Cidade do recebedor é obrigatória");
        }

        StringBuilder codeBuilder = new StringBuilder();
        // Cria o código Pix
        add(codeBuilder, Fields.PAYLOAD_FORMAT_INDICATOR, "01");

        StringBuilder merAccInfo = new StringBuilder();
        add(merAccInfo, Fields.MERCHANT_ACCOUNT_INFO_GUI, Fields.MERCHANT_ACCOUNT_INFO_GUI_VALUE);
        add(merAccInfo, Fields.MERCHANT_ACCOUNT_INFO_PIX_KEY, to.chavePix());

        add(codeBuilder, Fields.MERCHANT_ACCOUNT_INFO, merAccInfo.toString());

        add(codeBuilder, Fields.MERCHANT_CATEGORY, Fields.EMPTY_MERCHANT);

        if (transfer.currency() == null || "BRL".equals(transfer.currency())) {
            add(codeBuilder, Fields.TRANSACTION_CURRENCY, Fields.TRANSACTION_CURRENCY_BRL);
        }
        add(codeBuilder, Fields.TRANSACTION_VALUE, transfer.valorTransacao().setScale(2).toString());

        // 58 is mandatory: default to BR
        String pais = to.pais() != null && !to.pais().isEmpty() ? to.pais() : PixTarget.BRASIL;
        add(codeBuilder, Fields.MERCHANT_COUNTRY, pais);
        add(codeBuilder, Fields.MERCHANT_NAME, nome, Fields.MERCHANT_NAME_MAXLENGTH);
        add(codeBuilder, Fields.MERCHANT_CITY, cidade, Fields.MERCHANT_CITY_MAXLENGTH);

        // 62 is mandatory: txid "***" means no txid informed
        String txid = transfer.idTransacao();
        if (txid == null || txid.isEmpty()) {
            txid = Fields.ADDITIONAL_DATA_TXID_NONE;
        } else if (!Fields.ADDITIONAL_DATA_TXID_CHARSET.matcher(txid).matches()) {
            throw new IllegalArgumentException("txid deve conter apenas caracteres alfanuméricos (A-Z, a-z, 0-9): " + txid);
        }
        StringBuilder addData = new StringBuilder();
        add(addData, Fields.ADDITIONAL_DATA_TXID, txid, Fields.ADDITIONAL_DATA_TXID_MAXLENGTH);
        add(codeBuilder, Fields.ADDITIONAL_DATA, addData.toString());

        if (withCrc) {
            // Formata o CRC16 como uma string hexadecimal
            String partialCode = codeBuilder.toString();
            String crcHex = PixCRC16.crcChecksum(partialCode + Fields.CRC16 + "04");
            add(codeBuilder, Fields.CRC16, crcHex);
        }

        return codeBuilder.toString();
    }

    /**
     * Removes accents (e.g. "São" becomes "Sao"), drops any remaining non-printable-ASCII character
     * and trims. Non-ASCII characters take more than one byte and break the declared field length.
     */
    static String toAscii(String value) {
        if (value == null) {
            return "";
        }
        String noAccents = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.replaceAll("[^\\x20-\\x7E]", "").trim();
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
        String v = maxLength.isPresent() && value.length() > maxLength.get()
                ? value.substring(0, maxLength.get())
                : value;
        sb.append(field).append(String.format("%02d", v.length())).append(v);
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
        /** Value used in field 62.05 when no txid is informed (static BR Code). */
        public static String ADDITIONAL_DATA_TXID_NONE = "***";
        static Pattern ADDITIONAL_DATA_TXID_CHARSET = Pattern.compile("[A-Za-z0-9]+");

        public static String MERCHANT_CATEGORY = "52";
        public static String EMPTY_MERCHANT = "0000";

        public static String TRANSACTION_CURRENCY = "53";
        public static String TRANSACTION_CURRENCY_BRL = "986";
        public static String TRANSACTION_VALUE = "54";

        public static String MERCHANT_COUNTRY = "58";
        public static String MERCHANT_NAME = "59";
        static Optional<Integer> MERCHANT_NAME_MAXLENGTH = Optional.of(25);
        public static String MERCHANT_CITY = "60";
        static Optional<Integer> MERCHANT_CITY_MAXLENGTH = Optional.of(15);

        /** Field identifier for the CRC-16 result. */
        public static String CRC16 = "63";
    }
}
