package com.iskeru.javapix.pix;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PixCRC16Test {
    @Test
    void testSimpleCRC() {
        Assertions.assertEquals("245B", PixCRC16.crcChecksum("aaa"));
    }

    @Test
    void testKnownPixCRC() {
        Assertions.assertEquals("E3BF", PixCRC16.crcChecksum("00020126330014BR.GOV.BCB.PIX0111minha-chave520400005303986540512.345802BR5913Person Person6009SAO PAULO62110507myidis16304"));
    }

}
