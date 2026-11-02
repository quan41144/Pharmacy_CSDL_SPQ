package com.pharmacy.medicare;

import com.pharmacy.medicare.util.VietnameseNumberToWords;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VietnameseNumberToWordsTest {

    @Test
    void testStandardValues() {
        assertEquals("Hai mươi nghìn đồng chẵn.", VietnameseNumberToWords.convert(new BigDecimal("20000")));
        assertEquals("Mười lăm nghìn đồng chẵn.", VietnameseNumberToWords.convert(new BigDecimal("15000")));
        assertEquals("Bốn mươi lăm nghìn đồng chẵn.", VietnameseNumberToWords.convert(new BigDecimal("45000")));
        assertEquals("Hai mươi mốt nghìn đồng chẵn.", VietnameseNumberToWords.convert(new BigDecimal("21000")));
        assertEquals("Một trăm nghìn đồng chẵn.", VietnameseNumberToWords.convert(new BigDecimal("100000")));
        assertEquals("Một trăm linh năm nghìn đồng chẵn.", VietnameseNumberToWords.convert(new BigDecimal("105000")));
        assertEquals("Một trăm hai mươi lăm nghìn đồng chẵn.", VietnameseNumberToWords.convert(new BigDecimal("125000")));
        assertEquals("Hai triệu đồng chẵn.", VietnameseNumberToWords.convert(new BigDecimal("2000000")));
        assertEquals("Không đồng chẵn.", VietnameseNumberToWords.convert(BigDecimal.ZERO));
    }
}
