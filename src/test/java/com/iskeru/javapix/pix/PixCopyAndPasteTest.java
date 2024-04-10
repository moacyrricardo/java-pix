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
}
