package com.pharmacy.service;

import com.pharmacy.dto.DashboardKPIsDTO;
import com.pharmacy.dto.ImportSlipResponseDTO;
import com.pharmacy.dto.InventoryReportItemDTO;
import com.pharmacy.dto.InvoiceResponseDTO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public byte[] exportReportExcel(DashboardKPIsDTO kpi,
                                   List<InvoiceResponseDTO> invoices,
                                   List<ImportSlipResponseDTO> importSlips,
                                   List<InventoryReportItemDTO> inventories) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Styles
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat dataFormat = workbook.createDataFormat();
            currencyStyle.setDataFormat(dataFormat.getFormat("#,##0 \"đ\""));

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setAlignment(HorizontalAlignment.CENTER);

            // ================= SHEET 1: DOANH THU =================
            Sheet sheet1 = workbook.createSheet("Doanh thu & Hóa đơn");
            int rIdx = 0;

            Row rTitle = sheet1.createRow(rIdx++);
            Cell cTitle = rTitle.createCell(0);
            cTitle.setCellValue("BÁO CÁO DOANH THU HIỆU THUỐC - " + kpi.getFilterPeriod().toUpperCase());
            cTitle.setCellStyle(titleStyle);

            rIdx++; // Empty row

            // KPI rows
            Row rKpi1 = sheet1.createRow(rIdx++);
            rKpi1.createCell(0).setCellValue("Tổng tiền nhập hàng:");
            Cell cK1 = rKpi1.createCell(1);
            cK1.setCellValue(kpi.getTotalImportAmount().doubleValue());
            cK1.setCellStyle(currencyStyle);

            Row rKpi2 = sheet1.createRow(rIdx++);
            rKpi2.createCell(0).setCellValue("Tổng tiền bán ra (Doanh thu):");
            Cell cK2 = rKpi2.createCell(1);
            cK2.setCellValue(kpi.getTotalRevenue().doubleValue());
            cK2.setCellStyle(currencyStyle);

            Row rKpi3 = sheet1.createRow(rIdx++);
            rKpi3.createCell(0).setCellValue("Số tiền lãi (Lợi nhuận gộp):");
            Cell cK3 = rKpi3.createCell(1);
            cK3.setCellValue(kpi.getGrossProfit().doubleValue());
            cK3.setCellStyle(currencyStyle);

            Row rKpi4 = sheet1.createRow(rIdx++);
            rKpi4.createCell(0).setCellValue("Số dư Quỹ Chủ cửa hàng:");
            Cell cK4 = rKpi4.createCell(1);
            cK4.setCellValue(kpi.getAdminBalance().doubleValue());
            cK4.setCellStyle(currencyStyle);

            rIdx += 2; // Empty rows

            // Invoices Table
            String[] invHeaders = {"Mã HĐ", "Khách hàng", "Số điện thoại", "Nhân viên bán", "Kho xuất hàng", "Ngày tạo", "Tổng tiền", "Trạng thái"};
            Row rInvHeader = sheet1.createRow(rIdx++);
            for (int i = 0; i < invHeaders.length; i++) {
                Cell c = rInvHeader.createCell(i);
                c.setCellValue(invHeaders[i]);
                c.setCellStyle(headerStyle);
            }

            for (InvoiceResponseDTO inv : invoices) {
                Row r = sheet1.createRow(rIdx++);
                r.createCell(0).setCellValue(inv.getInvoiceCode());
                r.createCell(1).setCellValue(inv.getCustomerName());
                r.createCell(2).setCellValue(inv.getCustomerPhone() != null ? inv.getCustomerPhone() : "");
                r.createCell(3).setCellValue(inv.getSalesStaffName());
                r.createCell(4).setCellValue(inv.getWarehouseName());
                r.createCell(5).setCellValue(inv.getCreatedAt() != null ? inv.getCreatedAt().format(dateTimeFormatter) : "");

                Cell cAmt = r.createCell(6);
                cAmt.setCellValue(inv.getTotalAmount().doubleValue());
                cAmt.setCellStyle(currencyStyle);

                r.createCell(7).setCellValue(inv.getStatus());
            }

            for (int i = 0; i < invHeaders.length; i++) {
                sheet1.autoSizeColumn(i);
            }

            // ================= SHEET 2: TỒN KHO =================
            Sheet sheet2 = workbook.createSheet("Tồn kho & Lô thuốc");
            int r2 = 0;

            String[] stockHeaders = {"Kho", "Tên thuốc", "Nhóm thuốc", "Mã lô", "Ngày SX", "Hạn SD", "Số lượng tồn", "ĐVT", "Giá bán", "Tổng giá trị", "Số ngày còn", "Trạng thái"};
            Row rStockHeader = sheet2.createRow(r2++);
            for (int i = 0; i < stockHeaders.length; i++) {
                Cell c = rStockHeader.createCell(i);
                c.setCellValue(stockHeaders[i]);
                c.setCellStyle(headerStyle);
            }

            for (InventoryReportItemDTO it : inventories) {
                Row r = sheet2.createRow(r2++);
                r.createCell(0).setCellValue(it.getWarehouseName());
                r.createCell(1).setCellValue(it.getProductName());
                r.createCell(2).setCellValue(it.getCategoryName());
                r.createCell(3).setCellValue(it.getBatchCode());
                r.createCell(4).setCellValue(it.getMfgDate() != null ? it.getMfgDate().toString() : "");
                r.createCell(5).setCellValue(it.getExpDate() != null ? it.getExpDate().toString() : "");
                r.createCell(6).setCellValue(it.getQuantity());
                r.createCell(7).setCellValue(it.getUnit());

                Cell cPrice = r.createCell(8);
                cPrice.setCellValue(it.getSellingPrice().doubleValue());
                cPrice.setCellStyle(currencyStyle);

                Cell cVal = r.createCell(9);
                cVal.setCellValue(it.getTotalValue().doubleValue());
                cVal.setCellStyle(currencyStyle);

                r.createCell(10).setCellValue(it.getDaysRemaining());
                r.createCell(11).setCellValue(it.getExpiryStatus());
            }

            for (int i = 0; i < stockHeaders.length; i++) {
                sheet2.autoSizeColumn(i);
            }

            // ================= SHEET 3: PHIẾU NHẬP =================
            Sheet sheet3 = workbook.createSheet("Phiếu nhập hàng");
            int r3 = 0;

            String[] impHeaders = {"Mã phiếu", "Nhà cung cấp", "Nhân viên kho", "Kho nhập", "Ngày nhập", "Tổng tiền nhập", "Trạng thái"};
            Row rImpHeader = sheet3.createRow(r3++);
            for (int i = 0; i < impHeaders.length; i++) {
                Cell c = rImpHeader.createCell(i);
                c.setCellValue(impHeaders[i]);
                c.setCellStyle(headerStyle);
            }

            for (ImportSlipResponseDTO imp : importSlips) {
                Row r = sheet3.createRow(r3++);
                r.createCell(0).setCellValue("PN" + String.format("%05d", imp.getImportSlipId()));
                r.createCell(1).setCellValue(imp.getSupplierName());
                r.createCell(2).setCellValue(imp.getWarehouseStaffName());
                r.createCell(3).setCellValue(imp.getWarehouseName());
                r.createCell(4).setCellValue(imp.getImportDate() != null ? imp.getImportDate().format(dateTimeFormatter) : "");

                Cell cTot = r.createCell(5);
                cTot.setCellValue(imp.getTotalAmount().doubleValue());
                cTot.setCellStyle(currencyStyle);

                r.createCell(6).setCellValue(imp.getStatus());
            }

            for (int i = 0; i < impHeaders.length; i++) {
                sheet3.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xuất file Excel: " + e.getMessage(), e);
        }
    }
}
