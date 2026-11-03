package com.pharmacy.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.pharmacy.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final DecimalFormat currencyFormat = new DecimalFormat("#,##0");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private Font getUnicodeFont(float size, int style) {
        try {
            String fontPath = "C:/Windows/Fonts/times.ttf";
            if (style == Font.BOLD) {
                String boldPath = "C:/Windows/Fonts/timesbd.ttf";
                if (new File(boldPath).exists()) {
                    fontPath = boldPath;
                }
            } else if (style == Font.ITALIC) {
                String italicPath = "C:/Windows/Fonts/timesi.ttf";
                if (new File(italicPath).exists()) {
                    fontPath = italicPath;
                }
            }

            if (new File(fontPath).exists()) {
                BaseFont baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                return new Font(baseFont, size, style);
            }
        } catch (Exception ignored) {
        }
        return FontFactory.getFont(FontFactory.HELVETICA, size, style);
    }

    public byte[] exportInvoiceK80(InvoiceResponseDTO invoice) {
        // K80 standard width = 80mm ≈ 226.77 points
        float width = 226.77f;
        // Auto height estimation based on items count
        int itemCount = invoice.getItems() != null ? invoice.getItems().size() : 0;
        float height = Math.max(380f, 280f + itemCount * 30f);

        Rectangle pageSize = new Rectangle(width, height);
        Document document = new Document(pageSize, 8, 8, 10, 10);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = getUnicodeFont(13, Font.BOLD);
            Font subTitleFont = getUnicodeFont(9, Font.NORMAL);
            Font boldFont = getUnicodeFont(8.5f, Font.BOLD);
            Font regularFont = getUnicodeFont(8, Font.NORMAL);
            Font italicFont = getUnicodeFont(7.5f, Font.ITALIC);

            // Header Line 1: PHARMACY
            Paragraph pHeader = new Paragraph("PHARMACY", titleFont);
            pHeader.setAlignment(Element.ALIGN_CENTER);
            pHeader.setSpacingAfter(1);
            document.add(pHeader);

            // Header Line 2: ĐT: 02773766999
            Paragraph pPhone = new Paragraph("ĐT: 02773766999", subTitleFont);
            pPhone.setAlignment(Element.ALIGN_CENTER);
            pPhone.setSpacingAfter(2);
            document.add(pPhone);

            // Header Line 3: HÓA ĐƠN BÁN LẺ
            Paragraph pTitle = new Paragraph("HÓA ĐƠN BÁN LẺ", titleFont);
            pTitle.setAlignment(Element.ALIGN_CENTER);
            pTitle.setSpacingAfter(4);
            document.add(pTitle);

            // Divider 1
            LineSeparator separator = new LineSeparator(0.8f, 100, null, Element.ALIGN_CENTER, -2);
            document.add(separator);

            // Metadata
            Paragraph pMeta = new Paragraph();
            pMeta.setSpacingBefore(4);
            pMeta.setSpacingAfter(4);
            pMeta.setFont(regularFont);
            pMeta.add(new Chunk("Ngày mua: ", boldFont));
            pMeta.add(new Chunk(invoice.getCreatedAt() != null ? invoice.getCreatedAt().format(dateTimeFormatter) : "" + "\n", regularFont));
            pMeta.add(new Chunk("NV bán hàng: ", boldFont));
            pMeta.add(new Chunk(invoice.getSalesStaffName() + "\n", regularFont));
            pMeta.add(new Chunk("Người mua: ", boldFont));
            pMeta.add(new Chunk((invoice.getCustomerName() != null ? invoice.getCustomerName() : "KHVL") + "\n", regularFont));
            pMeta.add(new Chunk("Số HĐ: ", boldFont));
            pMeta.add(new Chunk(invoice.getInvoiceCode() + "\n", regularFont));
            document.add(pMeta);

            // Divider 2
            document.add(separator);

            // Product Items Table (6 cols: TT | Tên hàng | ĐVT | SL | ĐG | Thành tiền)
            PdfPTable table = new PdfPTable(new float[]{1.2f, 4.5f, 1.8f, 1.5f, 2.8f, 3.2f});
            table.setWidthPercentage(100);
            table.setSpacingBefore(4);
            table.setSpacingAfter(4);

            String[] headers = {"TT", "Tên hàng", "ĐVT", "SL", "ĐG", "Thành tiền"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, boldFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setBorder(Rectangle.BOTTOM);
                cell.setPadding(2);
                table.addCell(cell);
            }

            int seq = 1;
            int totalQty = 0;
            if (invoice.getItems() != null) {
                for (InvoiceItemResponseDTO item : invoice.getItems()) {
                    totalQty += item.getQuantity();

                    // TT
                    PdfPCell c1 = new PdfPCell(new Phrase(String.valueOf(seq++), regularFont));
                    c1.setHorizontalAlignment(Element.ALIGN_CENTER);
                    c1.setBorder(Rectangle.NO_BORDER);
                    c1.setPadding(2);
                    table.addCell(c1);

                    // Tên hàng (IN HOA)
                    PdfPCell c2 = new PdfPCell(new Phrase(item.getProductName().toUpperCase(), regularFont));
                    c2.setHorizontalAlignment(Element.ALIGN_LEFT);
                    c2.setBorder(Rectangle.NO_BORDER);
                    c2.setPadding(2);
                    table.addCell(c2);

                    // ĐVT
                    PdfPCell c3 = new PdfPCell(new Phrase(item.getUnit(), regularFont));
                    c3.setHorizontalAlignment(Element.ALIGN_CENTER);
                    c3.setBorder(Rectangle.NO_BORDER);
                    c3.setPadding(2);
                    table.addCell(c3);

                    // SL
                    PdfPCell c4 = new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), regularFont));
                    c4.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    c4.setBorder(Rectangle.NO_BORDER);
                    c4.setPadding(2);
                    table.addCell(c4);

                    // ĐG
                    PdfPCell c5 = new PdfPCell(new Phrase(currencyFormat.format(item.getUnitPrice()), regularFont));
                    c5.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    c5.setBorder(Rectangle.NO_BORDER);
                    c5.setPadding(2);
                    table.addCell(c5);

                    // Thành tiền
                    PdfPCell c6 = new PdfPCell(new Phrase(currencyFormat.format(item.getSubTotal()), regularFont));
                    c6.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    c6.setBorder(Rectangle.NO_BORDER);
                    c6.setPadding(2);
                    table.addCell(c6);
                }
            }
            document.add(table);

            // Divider 3
            document.add(separator);

            // Summary
            Paragraph pSum = new Paragraph();
            pSum.setSpacingBefore(4);
            pSum.add(new Chunk("Tổng SL: " + totalQty + "          ", boldFont));
            pSum.add(new Chunk("Tổng TT: " + currencyFormat.format(invoice.getTotalAmount()) + " đ\n", boldFont));
            pSum.add(new Chunk("Viết bằng chữ: ", italicFont));
            pSum.add(new Chunk(invoice.getTotalAmountInWords() + "\n", getUnicodeFont(7.5f, Font.BOLD | Font.ITALIC)));
            document.add(pSum);

            // Footer
            Paragraph pFoot = new Paragraph();
            pFoot.setAlignment(Element.ALIGN_CENTER);
            pFoot.setSpacingBefore(8);
            pFoot.add(new Chunk("Cảm ơn quý khách hàng đã mua hàng\n", italicFont));
            pFoot.add(new Chunk("Rất mong nhận được sự góp ý về phương thức phục vụ, giá cả, chất lượng của chúng tôi", italicFont));
            document.add(pFoot);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo hóa đơn PDF K80: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    public byte[] exportReportPdf(DashboardKPIsDTO kpi, List<InvoiceResponseDTO> invoices, List<InventoryReportItemDTO> inventories) {
        Document document = new Document(PageSize.A4, 25, 25, 25, 25);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font headerTitle = getUnicodeFont(16, Font.BOLD);
            Font sectionTitle = getUnicodeFont(12, Font.BOLD);
            Font bold = getUnicodeFont(9, Font.BOLD);
            Font normal = getUnicodeFont(8.5f, Font.NORMAL);
            Font italic = getUnicodeFont(8, Font.ITALIC);

            // Header
            Paragraph title = new Paragraph("HỆ THỐNG QUẢN LÝ HIỆU THUỐC PHARMACY", headerTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph sub = new Paragraph("BÁO CÁO DOANH THU & TỒN KHO - " + kpi.getFilterPeriod().toUpperCase(), sectionTitle);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(15);
            document.add(sub);

            // KPI Summary Table
            PdfPTable kpiTable = new PdfPTable(4);
            kpiTable.setWidthPercentage(100);
            kpiTable.setSpacingAfter(15);

            addCellHeader(kpiTable, "Tổng tiền nhập hàng", bold);
            addCellHeader(kpiTable, "Tổng tiền bán ra (Doanh thu)", bold);
            addCellHeader(kpiTable, "Số tiền lãi (Lợi nhuận gộp)", bold);
            addCellHeader(kpiTable, "Số dư Quỹ Chủ cửa hàng", bold);

            addCellData(kpiTable, currencyFormat.format(kpi.getTotalImportAmount()) + " đ", normal, Element.ALIGN_CENTER);
            addCellData(kpiTable, currencyFormat.format(kpi.getTotalRevenue()) + " đ", normal, Element.ALIGN_CENTER);
            addCellData(kpiTable, currencyFormat.format(kpi.getGrossProfit()) + " đ", normal, Element.ALIGN_CENTER);
            addCellData(kpiTable, currencyFormat.format(kpi.getAdminBalance()) + " đ", normal, Element.ALIGN_CENTER);
            document.add(kpiTable);

            // Section 1: Hóa đơn trong kỳ
            Paragraph s1 = new Paragraph("I. DANH SÁCH HÓA ĐƠN DOANH THU TRONG KỲ", sectionTitle);
            s1.setSpacingAfter(6);
            document.add(s1);

            PdfPTable invTable = new PdfPTable(new float[]{1.5f, 3.5f, 3.5f, 3f, 3f, 2f});
            invTable.setWidthPercentage(100);
            invTable.setSpacingAfter(15);

            addCellHeader(invTable, "Mã HĐ", bold);
            addCellHeader(invTable, "Khách hàng", bold);
            addCellHeader(invTable, "Nhân viên bán", bold);
            addCellHeader(invTable, "Ngày tạo", bold);
            addCellHeader(invTable, "Tổng tiền (VNĐ)", bold);
            addCellHeader(invTable, "Trạng thái", bold);

            for (InvoiceResponseDTO inv : invoices) {
                addCellData(invTable, inv.getInvoiceCode(), normal, Element.ALIGN_CENTER);
                addCellData(invTable, inv.getCustomerName(), normal, Element.ALIGN_LEFT);
                addCellData(invTable, inv.getSalesStaffName(), normal, Element.ALIGN_LEFT);
                addCellData(invTable, inv.getCreatedAt() != null ? inv.getCreatedAt().format(dateTimeFormatter) : "", normal, Element.ALIGN_CENTER);
                addCellData(invTable, currencyFormat.format(inv.getTotalAmount()) + " đ", normal, Element.ALIGN_RIGHT);
                addCellData(invTable, inv.getStatus(), normal, Element.ALIGN_CENTER);
            }
            document.add(invTable);

            // Section 2: Tồn kho
            Paragraph s2 = new Paragraph("II. BẢNG KÊ CÁC LÔ THUỐC TỒN KHO", sectionTitle);
            s2.setSpacingAfter(6);
            document.add(s2);

            PdfPTable stockTable = new PdfPTable(new float[]{1.5f, 4f, 2.5f, 2f, 2f, 1.5f, 2f, 2f});
            stockTable.setWidthPercentage(100);
            stockTable.setSpacingAfter(25);

            addCellHeader(stockTable, "Kho", bold);
            addCellHeader(stockTable, "Tên thuốc", bold);
            addCellHeader(stockTable, "Mã lô", bold);
            addCellHeader(stockTable, "NSX", bold);
            addCellHeader(stockTable, "HSD", bold);
            addCellHeader(stockTable, "Tồn kho", bold);
            addCellHeader(stockTable, "Giá bán", bold);
            addCellHeader(stockTable, "Trạng thái", bold);

            for (InventoryReportItemDTO it : inventories) {
                addCellData(stockTable, it.getWarehouseName(), normal, Element.ALIGN_LEFT);
                addCellData(stockTable, it.getProductName(), normal, Element.ALIGN_LEFT);
                addCellData(stockTable, it.getBatchCode(), normal, Element.ALIGN_CENTER);
                addCellData(stockTable, it.getMfgDate() != null ? it.getMfgDate().toString() : "", normal, Element.ALIGN_CENTER);
                addCellData(stockTable, it.getExpDate() != null ? it.getExpDate().toString() : "", normal, Element.ALIGN_CENTER);
                addCellData(stockTable, String.valueOf(it.getQuantity()) + " " + it.getUnit(), normal, Element.ALIGN_RIGHT);
                addCellData(stockTable, currencyFormat.format(it.getSellingPrice()) + " đ", normal, Element.ALIGN_RIGHT);
                addCellData(stockTable, it.getExpiryStatus(), normal, Element.ALIGN_CENTER);
            }
            document.add(stockTable);

            // Signatures block
            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);

            PdfPCell leftCell = new PdfPCell(new Phrase("Người lập báo cáo\n(Ký và ghi rõ họ tên)", italic));
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            signTable.addCell(leftCell);

            PdfPCell rightCell = new PdfPCell(new Phrase("Hà Nội, Ngày " + DateTimeFormatter.ofPattern("dd 'tháng' MM 'năm' yyyy").format(LocalDate.now()) + "\nChủ cửa hàng / Giám đốc\n(Ký tên và đóng dấu)", italic));
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            signTable.addCell(rightCell);

            document.add(signTable);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo báo cáo PDF A4: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    private void addCellHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setBackgroundColor(new java.awt.Color(235, 240, 245));
        table.addCell(cell);
    }

    private void addCellData(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        table.addCell(cell);
    }
}
