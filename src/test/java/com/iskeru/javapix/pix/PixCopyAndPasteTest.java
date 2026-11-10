package com.iskeru.javapix.pix;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class PixCopyAndPasteTest {
    @Test
    void testContent() {
        PixCopyAndPaste pix = PixCopyAndPaste.builder()
                .to(
                        PixTarget.builder()
                                .chavePix("minha-chave").pais(PixTarget.BRASIL).cidade("SAO PAULO")
                                .nome("Person Person").build()
                ).transfer(
                        PixTransaction.builder()
                                .idTransacao("myidis1")
                                .valorTransacao(new BigDecimal("12.34"))
                                .build()
                ).build();
        Assertions.assertEquals("00020126330014BR.GOV.BCB.PIX0111minha-chave520400005303986540512.345802BR5913Person Person6009SAO PAULO62110507myidis1", pix.generate(false));
    }

    @Test
    void testWithCRC16() {
        PixCopyAndPaste pix = PixCopyAndPaste.builder()
                .to(
                        PixTarget.builder()
                                .chavePix("minha-chave").pais(PixTarget.BRASIL).cidade("SAO PAULO")
                                .nome("Person Person").build()
                ).transfer(
                        PixTransaction.builder()
                                .idTransacao("myidis1")
                                .valorTransacao(new BigDecimal("12.34"))
                                .build()
                ).build();
        Assertions.assertEquals("00020126330014BR.GOV.BCB.PIX0111minha-chave520400005303986540512.345802BR5913Person Person6009SAO PAULO62110507myidis16304E3BF", pix.generate());
    }

    @Test
    void bcbManualReferencePayload() {
        // Reference static payload from the BCB BR Code manual
        Assertions.assertEquals("1D3D", PixCRC16.crcChecksum(
                "00020126580014br.gov.bcb.pix0136123e4567-e12b-12d1-a456-4266554400005204000053039865802BR5913Fulano de Tal6008BRASILIA62070503***6304"));
    }

    @Test
    void countryDefaultsToBR() {
        PixCopyAndPaste pix = PixCopyAndPaste.builder()
                .to(PixTarget.builder().chavePix("minha-chave").cidade("SAO PAULO").nome("Person Person").build())
                .transfer(PixTransaction.builder().idTransacao("myidis1").valorTransacao(new BigDecimal("12.34")).build())
                .build();
        Assertions.assertEquals("00020126330014BR.GOV.BCB.PIX0111minha-chave520400005303986540512.345802BR5913Person Person6009SAO PAULO62110507myidis1", pix.generate(false));
    }

    @Test
    void missingTxidIsSentAsStars() {
        PixCopyAndPaste pix = PixCopyAndPaste.builder()
                .to(PixTarget.builder().chavePix("minha-chave").cidade("SAO PAULO").nome("Person Person").build())
                .transfer(PixTransaction.builder().valorTransacao(new BigDecimal("12.34")).build())
                .build();
        String code = pix.generate();
        Assertions.assertEquals("00020126330014BR.GOV.BCB.PIX0111minha-chave520400005303986540512.345802BR5913Person Person6009SAO PAULO62070503***", code.substring(0, code.length() - 8));
        Assertions.assertNull(PixCopyAndPasteParser.parse(code).transfer().idTransacao());
    }

    @Test
    void nonAlphanumericTxidIsRejected() {
        PixCopyAndPaste pix = PixCopyAndPaste.builder()
                .to(PixTarget.builder().chavePix("minha-chave").cidade("SAO PAULO").nome("Person Person").build())
                .transfer(PixTransaction.builder().idTransacao("MEU_TXID_123").valorTransacao(new BigDecimal("12.34")).build())
                .build();
        Assertions.assertThrows(IllegalArgumentException.class, pix::generate);
    }

    @Test
    void cityIsCappedAndAccentsRemoved() {
        PixCopyAndPaste pix = PixCopyAndPaste.builder()
                .to(PixTarget.builder().chavePix("minha-chave").cidade("São José dos Campos").nome("João Conceição").build())
                .transfer(PixTransaction.builder().idTransacao("myidis1").valorTransacao(new BigDecimal("12.34")).build())
                .build();
        Assertions.assertTrue(pix.generate(false).contains("5914Joao Conceicao6015Sao Jose dos Ca62"));
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
//                                .idTransacao("143gpH1AQYR5qxw2Dli7Ys")
                                .idTransacao("XPTOLINGUICA123456789")
                                .valorTransacao(new BigDecimal("50.00"))
                                .build()
                ).build();
        Assertions.assertEquals("00020126440014BR.GOV.BCB.PIX0122my-email-key@email.com520400005303986540550.005802BR5925MY super user name blabla6009SAO PAULO62250521XPTOLINGUICA1234567896304B7FF", pix.generate());
    }
}
