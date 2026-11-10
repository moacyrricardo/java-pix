package com.iskeru.javapix.pix;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class PixCopyAndPasteTest {
    private static PixTarget.PixTargetBuilder target() {
        return PixTarget.builder().chavePix("minha-chave").cidade("SAO PAULO").nome("Person Person");
    }

    private static PixTransaction.PixTransactionBuilder tx() {
        return PixTransaction.builder().valorTransacao(new BigDecimal("12.34"));
    }

    private static PixCopyAndPaste pix(PixTarget.PixTargetBuilder to, PixTransaction.PixTransactionBuilder transfer) {
        return PixCopyAndPaste.builder().to(to.build()).transfer(transfer.build()).build();
    }

    @Test
    void testContent() {
        Assertions.assertEquals("00020126330014BR.GOV.BCB.PIX0111minha-chave520400005303986540512.345802BR5913Person Person6009SAO PAULO62110507myidis1",
                pix(target().pais(PixTarget.BRASIL), tx().idTransacao("myidis1")).generate(false));
    }

    @Test
    void testWithCRC16() {
        Assertions.assertEquals("00020126330014BR.GOV.BCB.PIX0111minha-chave520400005303986540512.345802BR5913Person Person6009SAO PAULO62110507myidis16304E3BF",
                pix(target().pais(PixTarget.BRASIL), tx().idTransacao("myidis1")).generate());
    }

    @Test
    void bcbManualReferencePayload() {
        // Reference static payload from the BCB BR Code manual
        Assertions.assertEquals("1D3D", PixCRC16.crcChecksum(
                "00020126580014br.gov.bcb.pix0136123e4567-e12b-12d1-a456-4266554400005204000053039865802BR5913Fulano de Tal6008BRASILIA62070503***6304"));
    }

    @Test
    void countryDefaultsToBR() {
        Assertions.assertEquals("00020126330014BR.GOV.BCB.PIX0111minha-chave520400005303986540512.345802BR5913Person Person6009SAO PAULO62110507myidis1",
                pix(target(), tx().idTransacao("myidis1")).generate(false));
    }

    @Test
    void missingTxidIsSentAsStars() {
        String code = pix(target(), tx()).generate();
        Assertions.assertEquals("00020126330014BR.GOV.BCB.PIX0111minha-chave520400005303986540512.345802BR5913Person Person6009SAO PAULO62070503***", code.substring(0, code.length() - 8));
        Assertions.assertNull(PixCopyAndPasteParser.parse(code).transfer().idTransacao());
    }

    @Test
    void nonAlphanumericTxidIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, pix(target(), tx().idTransacao("MEU_TXID_123"))::generate);
    }

    @Test
    void txidLongerThan25IsRejectedNotTruncated() {
        Assertions.assertThrows(IllegalArgumentException.class, pix(target(), tx().idTransacao("A".repeat(26)))::generate);
        Assertions.assertTrue(pix(target(), tx().idTransacao("A".repeat(25))).generate(false).endsWith("0525" + "A".repeat(25)));
    }

    @Test
    void cityIsCappedAndAccentsRemoved() {
        Assertions.assertTrue(pix(target().cidade("São José dos Campos").nome("João Conceição"), tx().idTransacao("myidis1"))
                .generate(false).contains("5914Joao Conceicao6015Sao Jose dos Ca62"));
    }

    @Test
    void nameAndCityAreRequired() {
        Assertions.assertThrows(IllegalArgumentException.class, pix(target().nome(null), tx())::generate);
        Assertions.assertThrows(IllegalArgumentException.class, pix(target().cidade(" "), tx())::generate);
    }

    @Test
    void nonAsciiIsDroppedFromNameAndCity() {
        Assertions.assertTrue(pix(target().nome("Café ☕ Bar").cidade(" RIO "), tx()).generate(false)
                .contains("5909Cafe  Bar6003RIO62"));
    }

    @Test
    void invalidAmountsAreRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, pix(target(), tx().valorTransacao(new BigDecimal("1.234")))::generate);
        Assertions.assertThrows(IllegalArgumentException.class, pix(target(), tx().valorTransacao(BigDecimal.ZERO))::generate);
        Assertions.assertThrows(IllegalArgumentException.class, pix(target(), tx().valorTransacao(new BigDecimal("-1")))::generate);
    }

    @Test
    void nonBrlCurrencyIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, pix(target(), tx().currency("USD"))::generate);
    }

    @Test
    void testItau() {
        PixCopyAndPaste pix = pix(
                PixTarget.builder().chavePix("my-email-key@email.com").pais(PixTarget.BRASIL).cidade("SAO PAULO").nome("MY super user name blabla"),
                PixTransaction.builder().idTransacao("XPTOLINGUICA123456789").valorTransacao(new BigDecimal("50.00")));
        Assertions.assertEquals("00020126440014BR.GOV.BCB.PIX0122my-email-key@email.com520400005303986540550.005802BR5925MY super user name blabla6009SAO PAULO62250521XPTOLINGUICA1234567896304B7FF", pix.generate());
    }
}
