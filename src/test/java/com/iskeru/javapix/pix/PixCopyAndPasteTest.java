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
