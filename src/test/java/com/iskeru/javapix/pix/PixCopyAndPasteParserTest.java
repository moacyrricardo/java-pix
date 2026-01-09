package com.iskeru.javapix.pix;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class PixCopyAndPasteParserTest {

    @Test
    void parseGeneratedSample() {
        String payload = "00020126330014BR.GOV.BCB.PIX0111minha-chave520400005303986540512.345802BR5913Person Person6009SAO PAULO62110507myidis16304E3BF";
        PixCopyAndPaste parsed = PixCopyAndPasteParser.parse(payload);
        Assertions.assertEquals("minha-chave", parsed.to().chavePix());
        Assertions.assertEquals("Person Person", parsed.to().nome());
        Assertions.assertEquals("BR", parsed.to().pais());
        Assertions.assertEquals("SAO PAULO", parsed.to().cidade());
        Assertions.assertEquals(new BigDecimal("12.34"), parsed.transfer().valorTransacao());
        Assertions.assertEquals("BRL", parsed.transfer().currency());
        Assertions.assertEquals("myidis1", parsed.transfer().idTransacao());
    }

    @Test
    void crcMismatchThrows() {
        String bad = "00020126330014BR.GOV.BCB.PIX0111minha-chave520400005303986540512.345802BR5913Person Person6009SAO PAULO62110507myidis16304FFFF"; // wrong CRC
        Assertions.assertThrows(IllegalArgumentException.class, () -> PixCopyAndPasteParser.parse(bad));
    }

    @Test
    void parseWithoutCrcStillWorks() {
        String payloadNoCrc = "00020126330014BR.GOV.BCB.PIX0111minha-chave520400005303986540512.345802BR5913Person Person6009SAO PAULO62110507myidis1";
        PixCopyAndPaste parsed = PixCopyAndPasteParser.parse(payloadNoCrc);
        Assertions.assertEquals("minha-chave", parsed.to().chavePix());
        Assertions.assertEquals(new BigDecimal("12.34"), parsed.transfer().valorTransacao());
    }
}
