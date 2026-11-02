package com.pharmacy.medicare.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.pharmacy.medicare.entity.Invoice;
import com.pharmacy.medicare.entity.InvoiceItem;
import com.pharmacy.medicare.util.VietnameseNumberToWords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class PdfReceiptService {

    private static final Logger log = LoggerFactory.getLogger(PdfReceiptService.class);

    private static final DecimalFormat CURRENCY_FORMAT;
    private static final DecimalFormat QUANTITY_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMAN);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        CURRENCY_FORMAT = new DecimalFormat("#,##0", symbols);
        QUANTITY_FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    /**
     * Xuất hóa đơn nhiệt khổ K80 (80mm = 226.77 points) chuẩn mẫu 100%.
     * Sử dụng font Times New Roman hỗ trợ tiếng Việt Unicode đầy đủ.
     */
    public byte[] generateThermalReceipt80mm(Invoice invoice) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int itemCount = invoice.getItems() != null ? invoice.getItems().size() : 0;
            // Chiều rộng 80mm = ~226.77 points, chiều cao tự động co giãn theo số lượng thuốc
            float baseHeight = 320f;
            float pageHeight = Math.max(380f, baseHeight + (itemCount * 30f));
            Rectangle pageSize = new Rectangle(226.77f, pageHeight);

            // Căn lề hẹp cho giấy in nhiệt (margin: trái/phải 8pt, trên/dưới 10pt)
            Document document = new Document(pageSize, 8f, 8f, 10f, 10f);
            PdfWriter.getInstance(document, out);
            document.open();

            // Khởi tạo các font Times New Roman có chân chuẩn Unicode tiếng Việt
            Font fontPharmacy = createFont(14.5f, Font.BOLD);
            Font fontStorePhone = createFont(8.5f, Font.NORMAL);
            Font fontInvoiceTitle = createFont(11.5f, Font.BOLD);
            Font fontMeta = createFont(8.0f, Font.NORMAL);
            Font fontTableHeader = createFont(7.5f, Font.BOLD);
            Font fontTableBody = createFont(7.5f, Font.NORMAL);
            Font fontTotalSummary = createFont(8.2f, Font.BOLD);
            Font fontWordsLabel = createFont(7.8f, Font.ITALIC);
            Font fontWordsValue = createFont(7.8f, Font.BOLD | Font.ITALIC);
            Font fontFooter = createFont(7.5f, Font.ITALIC);

            // =========================================================================
            // 1. PHẦN TIÊU ĐỀ (HEADER - CĂN GIỮA)
            // =========================================================================
            // Dòng 1: Chữ PHARMACY (In hoa, in đậm, cỡ chữ lớn nổi bật trên cùng)
            Paragraph pPharmacy = new Paragraph("PHARMACY", fontPharmacy);
            pPharmacy.setAlignment(Element.ALIGN_CENTER);
            pPharmacy.setSpacingBefore(0f);
            pPharmacy.setSpacingAfter(1f);
            document.add(pPharmacy);

            // Dòng 2: ĐT: 02773766999 (hoặc số điện thoại cửa hàng)
            Paragraph pPhone = new Paragraph("ĐT: 02773766999", fontStorePhone);
            pPhone.setAlignment(Element.ALIGN_CENTER);
            pPhone.setSpacingAfter(2f);
            document.add(pPhone);

            // Dòng 3: HÓA ĐƠN BÁN LẺ (In hoa, in đậm, cỡ chữ lớn)
            Paragraph pInvoiceTitle = new Paragraph("HÓA ĐƠN BÁN LẺ", fontInvoiceTitle);
            pInvoiceTitle.setAlignment(Element.ALIGN_CENTER);
            pInvoiceTitle.setSpacingAfter(5f);
            document.add(pInvoiceTitle);

            // Kẻ một đường gạch ngang liền nét (border-bottom: 1px solid #000) ngăn cách
            document.add(createSolidLine());

            // =========================================================================
            // 2. PHẦN THÔNG TIN CHUNG (METADATA - CĂN TRÁI, NHÃN VÀ GIÁ TRỊ THẲNG CỘT)
            // =========================================================================
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100f);
            metaTable.setWidths(new float[]{32f, 68f});
            metaTable.setSpacingBefore(3f);
            metaTable.setSpacingAfter(3f);

            // Ngày mua: dd/MM/yyyy hh:mm:ss SA/CH
            String formattedDate = formatReceiptDateTime(invoice.getCreatedAt());
            addMetaRow(metaTable, "Ngày mua:", formattedDate, fontMeta);

            // NV bán hàng: Tên hoặc Mã nhân viên bán hàng phụ trách đơn
            String staffInfo = "NV01";
            if (invoice.getSalesStaff() != null) {
                if (invoice.getSalesStaff().getFullName() != null && !invoice.getSalesStaff().getFullName().isBlank()) {
                    staffInfo = invoice.getSalesStaff().getFullName();
                } else if (invoice.getSalesStaff().getUsername() != null) {
                    staffInfo = invoice.getSalesStaff().getUsername();
                } else if (invoice.getSalesStaff().getId() != null) {
                    staffInfo = "NV#" + invoice.getSalesStaff().getId();
                }
            }
            addMetaRow(metaTable, "NV bán hàng:", staffInfo, fontMeta);

            // Người mua: Tên tài khoản hoặc họ tên khách hàng, nếu không có thì hiện KHVL
            String customerInfo = "KHVL";
            if (invoice.getCustomer() != null) {
                if (invoice.getCustomer().getUsername() != null && !invoice.getCustomer().getUsername().isBlank()) {
                    customerInfo = invoice.getCustomer().getUsername();
                } else if (invoice.getCustomer().getFullName() != null && !invoice.getCustomer().getFullName().isBlank()) {
                    customerInfo = invoice.getCustomer().getFullName();
                } else if (invoice.getCustomer().getId() != null) {
                    customerInfo = "KH#" + invoice.getCustomer().getId();
                }
            }
            addMetaRow(metaTable, "Người mua:", customerInfo, fontMeta);

            // Số HĐ: Mã hóa đơn hệ thống sinh ra (ví dụ: PT00001 hoặc mã HĐ hiện tại)
            String invoiceIdStr = String.valueOf(invoice.getId() != null ? invoice.getId() : 1);
            addMetaRow(metaTable, "ID HĐ:", invoiceIdStr, fontMeta);

            document.add(metaTable);

            // Kẻ một đường gạch ngang liền nét kéo dài hết chiều ngang hóa đơn
            document.add(createSolidLine());

            // =========================================================================
            // 3. BẢNG CHI TIẾT SẢN PHẨM (TABLE 6 CỘT)
            // TT | Tên hàng | ĐVT | SL | ĐG | Thành tiền
            // =========================================================================
            PdfPTable itemsTable = new PdfPTable(6);
            itemsTable.setWidthPercentage(100f);
            itemsTable.setSpacingBefore(3f);
            itemsTable.setSpacingAfter(3f);
            // Tỷ lệ độ rộng cột phù hợp giấy 80mm
            itemsTable.setWidths(new float[]{1.6f, 6.8f, 2.3f, 2.3f, 3.8f, 4.2f});

            // Table Header
            addTableCell(itemsTable, "TT", fontTableHeader, Element.ALIGN_CENTER, true);
            addTableCell(itemsTable, "Tên hàng", fontTableHeader, Element.ALIGN_LEFT, true);
            addTableCell(itemsTable, "ĐVT", fontTableHeader, Element.ALIGN_CENTER, true);
            addTableCell(itemsTable, "SL", fontTableHeader, Element.ALIGN_RIGHT, true);
            addTableCell(itemsTable, "ĐG", fontTableHeader, Element.ALIGN_RIGHT, true);
            addTableCell(itemsTable, "Thành tiền", fontTableHeader, Element.ALIGN_RIGHT, true);

            // Data rows
            double totalQuantitySum = 0;
            if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
                int stt = 1;
                for (InvoiceItem item : invoice.getItems()) {
                    // TT
                    addTableCell(itemsTable, String.valueOf(stt++), fontTableBody, Element.ALIGN_CENTER, false);

                    // Tên hàng: Viết IN HOA
                    String prodName = (item.getProduct() != null && item.getProduct().getName() != null)
                            ? item.getProduct().getName().toUpperCase()
                            : "THUỐC";
                    addTableCell(itemsTable, prodName, fontTableBody, Element.ALIGN_LEFT, false);

                    // ĐVT: Đơn vị tính (Hộp, Vỉ, Viên, Lọ, Chai, Tuýp)
                    String unit = (item.getProduct() != null && item.getProduct().getUnit() != null && !item.getProduct().getUnit().isBlank())
                            ? item.getProduct().getUnit()
                            : "Hộp";
                    addTableCell(itemsTable, unit, fontTableBody, Element.ALIGN_CENTER, false);

                    // SL: Số lượng định dạng 2 số thập phân (ví dụ: 1,00)
                    int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                    totalQuantitySum += qty;
                    addTableCell(itemsTable, QUANTITY_FORMAT.format(qty), fontTableBody, Element.ALIGN_RIGHT, false);

                    // ĐG: Đơn giá bán phân cách hàng nghìn bằng dấu chấm (ví dụ: 20.000)
                    BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                    addTableCell(itemsTable, CURRENCY_FORMAT.format(unitPrice), fontTableBody, Element.ALIGN_RIGHT, false);

                    // Thành tiền: SL * ĐG kèm ký hiệu 'đ' ở cuối (ví dụ: 20.000 đ)
                    BigDecimal lineSubTotal = item.getSubTotal() != null ? item.getSubTotal() : unitPrice.multiply(BigDecimal.valueOf(qty));
                    addTableCell(itemsTable, CURRENCY_FORMAT.format(lineSubTotal) + " đ", fontTableBody, Element.ALIGN_RIGHT, false);
                }
            }

            document.add(itemsTable);

            // Kẻ một đường gạch ngang liền nét ngay dưới danh sách sản phẩm
            document.add(createSolidLine());

            // =========================================================================
            // 4. PHẦN TỔNG KẾT THANH TOÁN
            // =========================================================================
            // Một dòng in đậm chia làm 2 cụm: Bên trái "Tổng SL: [Tổng]", bên phải "Tổng TT: [Tổng tiền] đ"
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100f);
            summaryTable.setWidths(new float[]{50f, 50f});
            summaryTable.setSpacingBefore(3f);
            summaryTable.setSpacingAfter(2f);

            PdfPCell cellTotalQty = new PdfPCell(new Phrase("Tổng SL: " + QUANTITY_FORMAT.format(totalQuantitySum), fontTotalSummary));
            cellTotalQty.setBorder(Rectangle.NO_BORDER);
            cellTotalQty.setHorizontalAlignment(Element.ALIGN_LEFT);
            cellTotalQty.setPadding(1f);
            summaryTable.addCell(cellTotalQty);

            BigDecimal grandTotal = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
            PdfPCell cellTotalAmount = new PdfPCell(new Phrase("Tổng TT: " + CURRENCY_FORMAT.format(grandTotal) + " đ", fontTotalSummary));
            cellTotalAmount.setBorder(Rectangle.NO_BORDER);
            cellTotalAmount.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cellTotalAmount.setPadding(1f);
            summaryTable.addCell(cellTotalAmount);

            document.add(summaryTable);

            // Dòng tiếp theo (In nghiêng toàn bộ):
            // "Viết bằng chữ: [Số tiền bằng chữ tiếng Việt]." (nhãn in nghiêng thường, nội dung vừa in nghiêng vừa in đậm)
            Paragraph wordsPara = new Paragraph();
            wordsPara.setSpacingBefore(1f);
            wordsPara.setSpacingAfter(4f);
            wordsPara.setLeading(10f);
            wordsPara.add(new Chunk("Viết bằng chữ: ", fontWordsLabel));
            String words = VietnameseNumberToWords.convert(grandTotal);
            wordsPara.add(new Chunk(words, fontWordsValue));
            document.add(wordsPara);

            // =========================================================================
            // 5. PHẦN CHÂN TRANG (FOOTER - CĂN GIỮA, CHỮ IN NGHIÊNG)
            // =========================================================================
            Paragraph pFooter1 = new Paragraph("Cảm ơn quý khách hàng đã mua hàng", fontFooter);
            pFooter1.setAlignment(Element.ALIGN_CENTER);
            pFooter1.setSpacingBefore(4f);
            pFooter1.setSpacingAfter(1f);
            document.add(pFooter1);

            Paragraph pFooter2 = new Paragraph("Rất mong nhận được sự góp ý về phương thức phục vụ, giá cả, chất lượng của chúng tôi", fontFooter);
            pFooter2.setAlignment(Element.ALIGN_CENTER);
            pFooter2.setLeading(9.5f);
            document.add(pFooter2);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Lỗi khi xuất hóa đơn bán lẻ PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi xuất hóa đơn bán lẻ PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Định dạng ngày giờ: dd/MM/yyyy hh:mm:ss SA/CH (ví dụ: 07/06/2018 12:00:00 SA)
     */
    public static String formatReceiptDateTime(LocalDateTime dt) {
        if (dt == null) return "";
        int h24 = dt.getHour();
        int h12 = h24 % 12;
        if (h12 == 0) h12 = 12;
        String amPm = (h24 < 12) ? "SA" : "CH";
        return String.format("%02d/%02d/%04d %02d:%02d:%02d %s",
                dt.getDayOfMonth(), dt.getMonthValue(), dt.getYear(),
                h12, dt.getMinute(), dt.getSecond(), amPm);
    }

    private LineSeparator createSolidLine() {
        LineSeparator sep = new LineSeparator(0.8f, 100f, Color.BLACK, Element.ALIGN_CENTER, -1f);
        return sep;
    }

    private void addMetaRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell cLabel = new PdfPCell(new Phrase(label, font));
        cLabel.setBorder(Rectangle.NO_BORDER);
        cLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
        cLabel.setPaddingTop(1f);
        cLabel.setPaddingBottom(1f);
        table.addCell(cLabel);

        PdfPCell cVal = new PdfPCell(new Phrase(value, font));
        cVal.setBorder(Rectangle.NO_BORDER);
        cVal.setHorizontalAlignment(Element.ALIGN_LEFT);
        cVal.setPaddingTop(1f);
        cVal.setPaddingBottom(1f);
        table.addCell(cVal);
    }

    private void addTableCell(PdfPTable table, String text, Font font, int alignment, boolean isHeader) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        if (isHeader) {
            cell.setBorder(Rectangle.BOTTOM);
            cell.setBorderWidthBottom(0.8f);
            cell.setPaddingBottom(2.5f);
            cell.setPaddingTop(1f);
        } else {
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPaddingTop(1.5f);
            cell.setPaddingBottom(1.5f);
        }
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(cell);
    }

    /**
     * Tạo Font Times New Roman Unicode hỗ trợ đầy đủ tiếng Việt.
     */
    private Font createFont(float size, int style) {
        BaseFont baseFont = resolveTimesBaseFont(style);
        return new Font(baseFont, size, style, Color.BLACK);
    }

    private BaseFont resolveTimesBaseFont(int style) {
        // Đường dẫn font Times New Roman trên Windows
        String fontPath;
        if ((style & Font.BOLD) != 0 && (style & Font.ITALIC) != 0) {
            fontPath = "C:/Windows/Fonts/timesbi.ttf";
        } else if ((style & Font.BOLD) != 0) {
            fontPath = "C:/Windows/Fonts/timesbd.ttf";
        } else if ((style & Font.ITALIC) != 0) {
            fontPath = "C:/Windows/Fonts/timesi.ttf";
        } else {
            fontPath = "C:/Windows/Fonts/times.ttf";
        }

        File file = new File(fontPath);
        if (file.exists()) {
            try {
                return BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception e) {
                log.warn("Không thể load font từ {}: {}", fontPath, e.getMessage());
            }
        }

        // Kiểm tra các đường dẫn Linux thông dụng
        String[] linuxCandidates = {
                "/usr/share/fonts/truetype/msttcorefonts/times.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf",
                "/usr/share/fonts/liberation/LiberationSerif-Regular.ttf"
        };
        for (String path : linuxCandidates) {
            File f = new File(path);
            if (f.exists()) {
                try {
                    return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                } catch (Exception ignored) {}
            }
        }

        // Fallback font tích hợp nếu không tìm thấy file font trên hệ thống
        try {
            return BaseFont.createFont(BaseFont.TIMES_ROMAN, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException("Không thể khởi tạo font: " + e.getMessage(), e);
        }
    }
}
