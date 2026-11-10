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

    @Test
    void bcbManualReferencePayload() {
        // Reference static payload from the BCB BR Code manual
        Assertions.assertEquals("1D3D", PixCRC16.crcChecksum(
                "00020126580014br.gov.bcb.pix0136123e4567-e12b-12d1-a456-4266554400005204000053039865802BR5913Fulano de Tal6008BRASILIA62070503***6304"));
    }

    @Test
    void testContent() {
        PixCopyAndPaste pix = PixCopyAndPaste.builder()
                .to(target().pais(PixTarget.BRASIL).build())
                .transfer(tx().idTransacao("myidis1").build()).build();
        Assertions.assertEquals("00020126330014BR.GOV.BCB.PIX0111minha-chave520400005303986540512.345802BR5913Person Person6009SAO PAULO62110507myidis1", pix.generate(false));
    }

    @Test
    void testWithCRC16() {
        PixCopyAndPaste pix = PixCopyAndPaste.builder()
                .to(target().pais(PixTarget.BRASIL).build())
                .transfer(tx().idTransacao("myidis1").build()).build();
        Assertions.assertEquals("00020126330014BR.GOV.BCB.PIX0111minha-chave520400005303986540512.345802BR5913Person Person6009SAO PAULO62110507myidis16304E3BF", pix.generate());
    }

    @Test
    void countryDefaultsToBR() {
        String code = PixCopyAndPaste.builder().to(target().build()).transfer(tx().idTransacao("myidis1").build()).build().generate(false);
        Assertions.assertTrue(code.contains("5802BR5913Person Person"), code);
    }

    @Test
    void missingTxidUsesStars() {
        String code = PixCopyAndPaste.builder().to(target().build()).transfer(tx().build()).build().generate(false);
        Assertions.assertTrue(code.endsWith("6009SAO PAULO62070503***"), code);
    }

    @Test
    void invalidTxidIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> PixCopyAndPaste.builder()
                .to(target().build()).transfer(tx().idTransacao("MEU_TXID_123").build()).build().generate());
        Assertions.assertThrows(IllegalArgumentException.class, () -> PixCopyAndPaste.builder()
                .to(target().build()).transfer(tx().idTransacao("A".repeat(26)).build()).build().generate());
    }

    @Test
    void cityIsTruncatedAndAccentsRemoved() {
        String code = PixCopyAndPaste.builder()
                .to(target().nome("João Conceição").cidade("São José dos Campos").build())
                .transfer(tx().build()).build().generate(false);
        Assertions.assertTrue(code.contains("5914Joao Conceicao6015Sao Jose dos Ca62"), code);
    }

    @Test
    void nameAndCityAreRequired() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> PixCopyAndPaste.builder()
                .to(target().nome(null).build()).transfer(tx().build()).build().generate());
        Assertions.assertThrows(IllegalArgumentException.class, () -> PixCopyAndPaste.builder()
                .to(target().cidade(" ").build()).transfer(tx().build()).build().generate());
    }

    @Test
    void amountWithMoreThanTwoDecimalsIsRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> PixCopyAndPaste.builder()
                .to(target().build()).transfer(PixTransaction.builder().valorTransacao(new BigDecimal("1.234")).build()).build().generate());
    }

    @Test
    void generatedCodeRoundTripsThroughParser() {
        String code = PixCopyAndPaste.builder().to(target().build()).transfer(tx().build()).build().generate();
        PixCopyAndPaste parsed = PixCopyAndPasteParser.parse(code);
        Assertions.assertEquals("BR", parsed.to().pais());
        Assertions.assertNull(parsed.transfer().idTransacao());
    }

    @Test
    void testItau() {
        PixCopyAndPaste pix = PixCopyAndPaste.builder()
                .to(
                        PixTarget.builder()
                                .chavePix("my-email-key@email.com").pais(PixTarget.BRASIL).cidade("SAO PAULO")
                                .nome("MY super user name blabla").build()
                ).transfer(
                        PixTransaction.builder()
                                .idTransacao("XPTOLINGUICA123456789")
                                .valorTransacao(new BigDecimal("50.00"))
                                .build()
                ).build();
        Assertions.assertEquals("00020126440014BR.GOV.BCB.PIX0122my-email-key@email.com520400005303986540550.005802BR5925MY super user name blabla6009SAO PAULO62250521XPTOLINGUICA1234567896304B7FF", pix.generate());
    }
}
