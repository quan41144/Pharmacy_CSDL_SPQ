package com.pharmacy.medicare;

import com.pharmacy.medicare.entity.Category;
import com.pharmacy.medicare.entity.Invoice;
import com.pharmacy.medicare.entity.InvoiceItem;
import com.pharmacy.medicare.entity.Product;
import com.pharmacy.medicare.entity.User;
import com.pharmacy.medicare.entity.enums.PaymentMethod;
import com.pharmacy.medicare.entity.enums.UserStatus;
import com.pharmacy.medicare.service.PdfReceiptService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PdfReceiptServiceTest {

    @Test
    void testFormatReceiptDateTime() {
        LocalDateTime dt = LocalDateTime.of(2018, 6, 7, 0, 0, 0);
        String formatted = PdfReceiptService.formatReceiptDateTime(dt);
        assertEquals("07/06/2018 12:00:00 SA", formatted);

        LocalDateTime dtPm = LocalDateTime.of(2026, 10, 31, 14, 30, 45);
        String formattedPm = PdfReceiptService.formatReceiptDateTime(dtPm);
        assertEquals("31/10/2026 02:30:45 CH", formattedPm);
    }

    @Test
    void testGenerateThermalReceipt80mm() {
        PdfReceiptService service = new PdfReceiptService();

        User customer = new User("khachhang1", "pass", "Nguyễn Văn A", BigDecimal.ZERO, UserStatus.ACTIVE);
        User staff = new User("nhanvien1", "pass", "Trần Thị B", BigDecimal.ZERO, UserStatus.ACTIVE);

        Invoice invoice = new Invoice(customer, staff, new BigDecimal("20000.00"), PaymentMethod.CASH, 10);
        invoice.setId(1L);
        invoice.setCreatedAt(LocalDateTime.of(2018, 6, 7, 0, 0, 0));

        Category cat = new Category("Thuốc", "Mô tả");
        Product p1 = new Product("PARACETAMOL 500MG", cat, "", "", "", "", "Hộp",
                new BigDecimal("10000"), new BigDecimal("20000"), 100);

        InvoiceItem item1 = new InvoiceItem(p1, 1, new BigDecimal("20000.00"));
        invoice.addItem(item1);

        byte[] pdfBytes = service.generateThermalReceipt80mm(invoice);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);

        // Verify PDF Magic Bytes (%PDF-)
        String header = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertEquals("%PDF-", header);
    }
}
