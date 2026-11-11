package com.pharmacy.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.pharmacy.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
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

    private final Color PRIMARY_GREEN = new Color(0, 135, 68);
    private final Color DARK_GREEN = new Color(0, 100, 50);
    private final Color LIGHT_GREEN = new Color(236, 247, 238);
    private final Color BORDER_GREEN = new Color(46, 125, 50);
    private final Color BORDER_LIGHT = new Color(165, 214, 167);
    private final Color TEXT_MUTED = new Color(100, 100, 100);

    private Font getUnicodeFont(float size, int style) {
        return getUnicodeFont(size, style, Color.BLACK);
    }

    private Font getUnicodeFont(float size, int style, Color color) {
        try {
            String fontPath = "C:/Windows/Fonts/arial.ttf";
            if (style == Font.BOLD) {
                String boldPath = "C:/Windows/Fonts/arialbd.ttf";
                if (new File(boldPath).exists()) fontPath = boldPath;
            } else if (style == Font.ITALIC) {
                String italicPath = "C:/Windows/Fonts/ariali.ttf";
                if (new File(italicPath).exists()) fontPath = italicPath;
            } else if (style == (Font.BOLD | Font.ITALIC)) {
                String biPath = "C:/Windows/Fonts/arialbi.ttf";
                if (new File(biPath).exists()) fontPath = biPath;
            }

            if (new File(fontPath).exists()) {
                BaseFont baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                return new Font(baseFont, size, style, color);
            }
        } catch (Exception ignored) {
        }
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
    }

    public byte[] exportInvoiceK80(InvoiceResponseDTO invoice) {
        // Form hóa đơn bán lẻ theo chuẩn 1 tờ A4 Portrait (210 x 297 mm)
        Document document = new Document(PageSize.A4, 28, 28, 24, 24);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // Viền bo trang và dải chân trang
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    PdfContentByte cb = writer.getDirectContent();
                    cb.saveState();

                    float width = document.getPageSize().getWidth();
                    float height = document.getPageSize().getHeight();

                    // Khung viền xanh bo góc toàn trang A4 (giống mẫu ảnh)
                    cb.setColorStroke(PRIMARY_GREEN);
                    cb.setLineWidth(1.5f);
                    cb.roundRectangle(20, 20, width - 40, height - 40, 10);
                    cb.stroke();

                    // Dải banner xanh đáy trang A4
                    cb.setColorFill(PRIMARY_GREEN);
                    cb.roundRectangle(22, 22, width - 44, 24, 6);
                    cb.fill();

                    try {
                        Font bannerFont = getUnicodeFont(9.5f, Font.BOLD, Color.WHITE);
                        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                                new Phrase("Quý khách được đổi trả trong vòng 5 - 7 ngày, xin cảm ơn Quý khách!", bannerFont),
                                width / 2, 29, 0);

                        // Dòng chữ dọc mép phải giấy như mẫu ảnh
                        Font sideFont = getUnicodeFont(7.5f, Font.NORMAL, new Color(130, 130, 130));
                        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                                new Phrase("Hệ thống Nhà thuốc PHARMACY - Hotline: 0962416952", sideFont),
                                width - 10, height / 2, 90);
                    } catch (Exception ignored) {}

                    cb.restoreState();
                }
            });

            document.open();

            Font brandTitleFont = getUnicodeFont(19f, Font.BOLD, PRIMARY_GREEN);
            Font infoFont = getUnicodeFont(10.5f, Font.NORMAL, Color.BLACK);
            Font phoneFont = getUnicodeFont(11f, Font.BOLD, PRIMARY_GREEN);
            Font sloganFont = getUnicodeFont(11f, Font.ITALIC, PRIMARY_GREEN);
            Font docTitleFont = getUnicodeFont(18f, Font.BOLD, PRIMARY_GREEN);
            Font boldFont = getUnicodeFont(10.5f, Font.BOLD, Color.BLACK);
            Font regularFont = getUnicodeFont(10.5f, Font.NORMAL, Color.BLACK);
            Font italicFont = getUnicodeFont(10f, Font.ITALIC, Color.BLACK);

            // ================= 1. HEADER (LOGO + THÔNG TIN NHÀ THUỐC) =================
            PdfPTable headerTable = new PdfPTable(new float[]{1.3f, 4.7f});
            headerTable.setWidthPercentage(100);
            headerTable.setSpacingAfter(8);

            // Cột 1: Logo
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            Paragraph logoP = new Paragraph();
            logoP.setAlignment(Element.ALIGN_CENTER);
            logoP.add(new Chunk("+ ✛ +\n", getUnicodeFont(20f, Font.BOLD, PRIMARY_GREEN)));
            logoP.add(new Chunk("PHARMACY", getUnicodeFont(10.5f, Font.BOLD, PRIMARY_GREEN)));
            logoCell.addElement(logoP);
            headerTable.addCell(logoCell);

            // Cột 2: Thông tin hiệu thuốc
            PdfPCell brandCell = new PdfPCell();
            brandCell.setBorder(Rectangle.NO_BORDER);
            brandCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            Paragraph pBrand = new Paragraph("NHÀ THUỐC PHARMACY", brandTitleFont);
            pBrand.setSpacingAfter(3);
            brandCell.addElement(pBrand);

            String whAddr = (invoice.getWarehouseAddress() != null && !invoice.getWarehouseAddress().isBlank())
                    ? invoice.getWarehouseAddress()
                    : (invoice.getWarehouseName() != null ? invoice.getWarehouseName() : "Kho Tổng - Số 123 Đường Giải Phóng, Hà Nội");

            Paragraph pAddr = new Paragraph();
            pAddr.add(new Chunk("Địa chỉ: ", boldFont));
            pAddr.add(new Chunk(whAddr + "\n", infoFont));
            pAddr.add(new Chunk("Điện thoại: ", boldFont));
            pAddr.add(new Chunk("0962416952\n", phoneFont));
            pAddr.setSpacingAfter(3);
            brandCell.addElement(pAddr);

            Paragraph pSlogan = new Paragraph("Chăm Sóc Sức Khỏe – Chia Sẻ Niềm Vui", sloganFont);
            pSlogan.setAlignment(Element.ALIGN_LEFT);
            brandCell.addElement(pSlogan);

            headerTable.addCell(brandCell);
            document.add(headerTable);

            // ================= 2. TIÊU ĐỀ HÓA ĐƠN & SỐ =================
            Paragraph pDocTitle = new Paragraph("HÓA ĐƠN BÁN HÀNG", docTitleFont);
            pDocTitle.setAlignment(Element.ALIGN_CENTER);
            pDocTitle.setSpacingAfter(2);
            document.add(pDocTitle);

            Paragraph pDocCode = new Paragraph("Số: " + (invoice.getInvoiceCode() != null ? invoice.getInvoiceCode() : "........"), regularFont);
            pDocCode.setAlignment(Element.ALIGN_CENTER);
            pDocCode.setSpacingAfter(8);
            document.add(pDocCode);

            // ================= 3. THÔNG TIN KHÁCH HÀNG =================
            PdfPTable custTable = new PdfPTable(new float[]{3.6f, 2.4f});
            custTable.setWidthPercentage(100);
            custTable.setSpacingAfter(8);

            String custName = invoice.getCustomerName() != null ? invoice.getCustomerName() : "Khách lẻ";
            String custPhone = (invoice.getCustomerPhone() != null && !invoice.getCustomerPhone().isBlank()) ? invoice.getCustomerPhone() : "....................";
            String custAddr = (invoice.getCustomerAddress() != null && !invoice.getCustomerAddress().isBlank()) ? invoice.getCustomerAddress() : "........................................................";

            PdfPCell cName = new PdfPCell(new Phrase("Tên khách hàng: " + custName, regularFont));
            cName.setBorder(Rectangle.NO_BORDER);
            cName.setPadding(2);
            custTable.addCell(cName);

            PdfPCell cPhone = new PdfPCell(new Phrase("Điện thoại: " + custPhone, regularFont));
            cPhone.setBorder(Rectangle.NO_BORDER);
            cPhone.setPadding(2);
            custTable.addCell(cPhone);

            PdfPCell cAddr = new PdfPCell(new Phrase("Địa chỉ: " + custAddr, regularFont));
            cAddr.setColspan(2);
            cAddr.setBorder(Rectangle.NO_BORDER);
            cAddr.setPadding(2);
            custTable.addCell(cAddr);

            document.add(custTable);

            // ================= 4. BẢNG HÀNG HÓA =================
            PdfPTable itemsTable = new PdfPTable(new float[]{0.8f, 5.0f, 1.4f, 2.0f, 2.2f});
            itemsTable.setWidthPercentage(100);
            itemsTable.setSpacingBefore(4);
            itemsTable.setSpacingAfter(6);

            // Header bảng
            String[] headers = {"TT", "Hàng hóa", "Số lượng", "Đơn giá", "Thành tiền"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, getUnicodeFont(10.5f, Font.BOLD, Color.WHITE)));
                cell.setBackgroundColor(PRIMARY_GREEN);
                cell.setBorderColor(PRIMARY_GREEN);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(6f);
                itemsTable.addCell(cell);
            }

            int seq = 1;
            int totalRows = 0;
            if (invoice.getItems() != null) {
                for (InvoiceItemResponseDTO item : invoice.getItems()) {
                    totalRows++;

                    // TT
                    PdfPCell c1 = new PdfPCell(new Phrase(String.valueOf(seq++), regularFont));
                    c1.setHorizontalAlignment(Element.ALIGN_CENTER);
                    c1.setBorderColor(BORDER_LIGHT);
                    c1.setPadding(5.5f);
                    itemsTable.addCell(c1);

                    // Hàng hóa (Tên thuốc + ĐVT)
                    String prodLabel = item.getProductName() + (item.getUnit() != null ? " (" + item.getUnit() + ")" : "");
                    PdfPCell c2 = new PdfPCell(new Phrase(prodLabel, regularFont));
                    c2.setHorizontalAlignment(Element.ALIGN_LEFT);
                    c2.setBorderColor(BORDER_LIGHT);
                    c2.setPadding(5.5f);
                    itemsTable.addCell(c2);

                    // Số lượng
                    PdfPCell c3 = new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), regularFont));
                    c3.setHorizontalAlignment(Element.ALIGN_CENTER);
                    c3.setBorderColor(BORDER_LIGHT);
                    c3.setPadding(5.5f);
                    itemsTable.addCell(c3);

                    // Đơn giá
                    PdfPCell c4 = new PdfPCell(new Phrase(currencyFormat.format(item.getUnitPrice()), regularFont));
                    c4.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    c4.setBorderColor(BORDER_LIGHT);
                    c4.setPadding(5.5f);
                    itemsTable.addCell(c4);

                    // Thành tiền
                    PdfPCell c5 = new PdfPCell(new Phrase(currencyFormat.format(item.getSubTotal()), regularFont));
                    c5.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    c5.setBorderColor(BORDER_LIGHT);
                    c5.setPadding(5.5f);
                    itemsTable.addCell(c5);
                }
            }

            // Tạo các dòng trống để bảng có đủ 12 dòng đúng chuẩn mẫu phôi hóa đơn bán lẻ A4
            int minRows = 12;
            for (int r = totalRows + 1; r <= minRows; r++) {
                PdfPCell c1 = new PdfPCell(new Phrase(String.valueOf(r), getUnicodeFont(9.5f, Font.NORMAL, new Color(180, 180, 180))));
                c1.setHorizontalAlignment(Element.ALIGN_CENTER);
                c1.setBorderColor(BORDER_LIGHT);
                c1.setPadding(5.5f);
                itemsTable.addCell(c1);

                for (int c = 2; c <= 5; c++) {
                    PdfPCell emptyC = new PdfPCell(new Phrase(" ", regularFont));
                    emptyC.setBorderColor(BORDER_LIGHT);
                    emptyC.setPadding(5.5f);
                    itemsTable.addCell(emptyC);
                }
            }

            // Dòng Cộng (Tổng tiền)
            PdfPCell totalLabelCell = new PdfPCell(new Phrase("Cộng", boldFont));
            totalLabelCell.setColspan(4);
            totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalLabelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            totalLabelCell.setBorderColor(BORDER_LIGHT);
            totalLabelCell.setPadding(6f);
            itemsTable.addCell(totalLabelCell);

            PdfPCell totalValCell = new PdfPCell(new Phrase(currencyFormat.format(invoice.getTotalAmount()) + " đ", getUnicodeFont(11.5f, Font.BOLD, PRIMARY_GREEN)));
            totalValCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalValCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            totalValCell.setBorderColor(BORDER_LIGHT);
            totalValCell.setPadding(6f);
            itemsTable.addCell(totalValCell);

            document.add(itemsTable);

            // ================= 5. TỔNG TIỀN BẰNG CHỮ =================
            Paragraph pWords = new Paragraph();
            pWords.add(new Chunk("Thành tiền bằng chữ: ", italicFont));
            pWords.add(new Chunk(invoice.getTotalAmountInWords() != null ? invoice.getTotalAmountInWords() : "", getUnicodeFont(10.5f, Font.BOLD | Font.ITALIC, Color.BLACK)));
            pWords.setSpacingAfter(10);
            document.add(pWords);

            // ================= 6. GHI CHÚ & CHỮ KÝ DƯỢC SỸ =================
            PdfPTable footerTable = new PdfPTable(new float[]{3.2f, 2.8f});
            footerTable.setWidthPercentage(100);
            footerTable.setSpacingAfter(10);

            // Cột trái: Ô ghi chú bo góc nhẹ nền xanh nhạt
            PdfPCell noteCell = new PdfPCell();
            noteCell.setBackgroundColor(LIGHT_GREEN);
            noteCell.setBorderColor(BORDER_LIGHT);
            noteCell.setPadding(8);

            Paragraph pNote = new Paragraph();
            pNote.add(new Chunk("Ghi chú:\n", boldFont));
            pNote.add(new Chunk(invoice.getNote() != null && !invoice.getNote().isBlank() ? invoice.getNote() : "Hàng mua theo đơn, bảo quản nơi khô ráo thoáng mát.", regularFont));
            noteCell.addElement(pNote);
            footerTable.addCell(noteCell);

            // Cột phải: Ngày tháng năm và Dược sỹ (Tên người bán)
            LocalDate d = invoice.getCreatedAt() != null ? invoice.getCreatedAt().toLocalDate() : LocalDate.now();
            String dateStr = String.format("Ngày %02d tháng %02d năm 20%02d", d.getDayOfMonth(), d.getMonthValue(), d.getYear() % 100);

            PdfPCell signCell = new PdfPCell();
            signCell.setBorder(Rectangle.NO_BORDER);
            signCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph pSign = new Paragraph();
            pSign.setAlignment(Element.ALIGN_CENTER);
            pSign.add(new Chunk(dateStr + "\n", italicFont));
            pSign.add(new Chunk("DƯỢC SỸ\n", getUnicodeFont(11f, Font.BOLD, PRIMARY_GREEN)));
            pSign.add(new Chunk("(Ký, ghi rõ họ tên)\n\n\n\n", italicFont));
            pSign.add(new Chunk(invoice.getSalesStaffName() != null ? invoice.getSalesStaffName() : "Dược sỹ bán hàng", boldFont));
            signCell.addElement(pSign);
            footerTable.addCell(signCell);

            document.add(footerTable);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo hóa đơn bán lẻ PDF: " + e.getMessage(), e);
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
