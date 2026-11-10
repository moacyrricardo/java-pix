package com.iskeru.javapix.pix;

import lombok.Builder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
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
        if (transfer == null || transfer.valorTransacao() == null) {
            throw new IllegalArgumentException("Valor da transação inválido");
        }
        if (transfer.valorTransacao().signum() <= 0) {
            throw new IllegalArgumentException("Valor da transação deve ser positivo");
        }
        if (to == null || to.chavePix() == null || to.chavePix().isBlank()) {
            throw new IllegalArgumentException("Chave Pix inválida");
        }
        String nome = normalize(to.nome());
        if (nome.isEmpty()) {
            throw new IllegalArgumentException("Nome do recebedor é obrigatório");
        }
        String cidade = normalize(to.cidade());
        if (cidade.isEmpty()) {
            throw new IllegalArgumentException("Cidade do recebedor é obrigatória");
        }
        String pais = to.pais() == null || to.pais().isBlank() ? PixTarget.BRASIL : to.pais();

        String txid = transfer.idTransacao();
        if (txid == null || txid.isEmpty()) {
            txid = Fields.ADDITIONAL_DATA_TXID_NONE;
        } else if (!Fields.ADDITIONAL_DATA_TXID_PATTERN.matcher(txid).matches()) {
            throw new IllegalArgumentException("txid deve ter de 1 a 25 caracteres alfanuméricos (A-Z, a-z, 0-9): " + txid);
        }

        String valor;
        try {
            valor = transfer.valorTransacao().setScale(2, RoundingMode.UNNECESSARY).toPlainString();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Valor da transação deve ter no máximo 2 casas decimais");
        }

        StringBuilder codeBuilder = new StringBuilder();
        add(codeBuilder, Fields.PAYLOAD_FORMAT_INDICATOR, "01");

        StringBuilder merAccInfo = new StringBuilder();
        add(merAccInfo, Fields.MERCHANT_ACCOUNT_INFO_GUI, Fields.MERCHANT_ACCOUNT_INFO_GUI_VALUE);
        add(merAccInfo, Fields.MERCHANT_ACCOUNT_INFO_PIX_KEY, to.chavePix().trim());
        add(codeBuilder, Fields.MERCHANT_ACCOUNT_INFO, merAccInfo.toString());

        add(codeBuilder, Fields.MERCHANT_CATEGORY, Fields.EMPTY_MERCHANT);

        if (transfer.currency() != null && !"BRL".equals(transfer.currency())) {
            throw new IllegalArgumentException("Apenas BRL é suportado: " + transfer.currency());
        }
        add(codeBuilder, Fields.TRANSACTION_CURRENCY, Fields.TRANSACTION_CURRENCY_BRL);
        add(codeBuilder, Fields.TRANSACTION_VALUE, valor);

        add(codeBuilder, Fields.MERCHANT_COUNTRY, pais);
        add(codeBuilder, Fields.MERCHANT_NAME, nome, Fields.MERCHANT_NAME_MAXLENGTH);
        add(codeBuilder, Fields.MERCHANT_CITY, cidade, Fields.MERCHANT_CITY_MAXLENGTH);

        StringBuilder addData = new StringBuilder();
        add(addData, Fields.ADDITIONAL_DATA_TXID, txid);
        add(codeBuilder, Fields.ADDITIONAL_DATA, addData.toString());

        if (withCrc) {
            String partialCode = codeBuilder.toString();
            String crcHex = PixCRC16.crcChecksum(partialCode + Fields.CRC16 + "04");
            add(codeBuilder, Fields.CRC16, crcHex);
        }

        return codeBuilder.toString();
    }

    /**
     * Removes accents and trims the value. Name and city must be plain ASCII so that
     * every banking app accepts them and the declared field length matches the encoded bytes.
     */
    static String normalize(String value) {
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
     * @throws IllegalArgumentException if the encoded value is longer than 99 bytes
     */
    public void add(StringBuilder sb, String field, String value, Optional<Integer> maxLength) {
        String v = maxLength.isPresent() && value.length() > maxLength.get()
                ? value.substring(0, maxLength.get()).trim()
                : value;
        int byteLen = v.getBytes(StandardCharsets.UTF_8).length;
        if (byteLen > 99) {
            throw new IllegalArgumentException("Campo " + field + " excede 99 bytes");
        }
        sb.append(field).append(String.format("%02d", byteLen)).append(v);
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

        public static String ADDITIONAL_DATA_TXID = "05";
        /** Value used in field 62.05 when no txid is informed (static BR Code). */
        public static String ADDITIONAL_DATA_TXID_NONE = "***";
        static Pattern ADDITIONAL_DATA_TXID_PATTERN = Pattern.compile("[A-Za-z0-9]{1,25}");

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
