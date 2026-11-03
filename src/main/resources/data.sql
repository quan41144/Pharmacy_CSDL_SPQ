-- ========================================================
-- PHARMACY MANAGEMENT SYSTEM - INITIAL SEED DATA
-- ========================================================

-- 1. categories
INSERT INTO categories (category_id, category_name, description) VALUES
(1, 'Giảm đau - Hạ sốt - Kháng viêm', 'Các loại thuốc giảm đau, hạ sốt, điều trị triệu chứng cảm cúm'),
(2, 'Kháng sinh - Kháng nấm', 'Thuốc kháng sinh diệt khuẩn, kháng virus, trị nấm theo đơn'),
(3, 'Tim mạch - Huyết áp', 'Thuốc điều trị tăng huyết áp, rối loạn nhịp tim và tim mạch'),
(4, 'Tiêu hóa - Dạ dày', 'Thuốc dạ dày, men vi sinh, chống tiêu chảy, táo bón'),
(5, 'Hô hấp - Dị ứng', 'Thuốc trị ho, long đờm, viêm mũi dị ứng, hen suyễn'),
(6, 'Vitamin & Thực phẩm chức năng', 'Bổ sung vi chất dinh dưỡng, tăng cường đề kháng'),
(7, 'Cơ xương khớp', 'Thuốc giảm đau khớp, gel bôi xoa bóp cơ bắp'),
(8, 'Dược mỹ phẩm & Da liễu', 'Kem bôi da liễu sát trùng, chống dị ứng da')
ON CONFLICT (category_id) DO NOTHING;

-- 2. warehouses
INSERT INTO warehouses (warehouse_id, warehouse_name, warehouse_address, status) VALUES
(1, 'Kho Tổng Hà Nội', '102 Trường Chinh, Đống Đa, Hà Nội', 'ACTIVE'),
(2, 'Kho Bán Lẻ Cầu Giấy', '45 Cầu Giấy, Quan Hoa, Cầu Giấy, Hà Nội', 'ACTIVE'),
(3, 'Kho Dự Phòng Thanh Xuân', '88 Nguyễn Trãi, Thanh Xuân, Hà Nội', 'ACTIVE')
ON CONFLICT (warehouse_id) DO NOTHING;

-- 3. employees (Password is: 123456 -> BCrypt: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi)
INSERT INTO employees (employee_id, username, password, full_name, date_of_birth, gender, address, phone, start_date, position, work_status, balance, warehouse_id) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Dược sĩ Nguyễn Văn Quản Trị', '1985-05-15', 'Nam', '15 Hoàng Cầu, Đống Đa, Hà Nội', '0912000001', '2020-01-01', 'ROLE_ADMIN', 'ACTIVE', 50000000.00, 1),
(2, 'sales', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Nguyễn Thị Bán Hàng', '1996-08-20', 'Nữ', '28 Trần Thái Tông, Cầu Giấy, Hà Nội', '0912000002', '2022-03-15', 'ROLE_SALES', 'ACTIVE', 0.00, 1),
(3, 'warehouse', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Trần Văn Thủ Kho', '1992-11-10', 'Nam', '50 Láng Hạ, Đống Đa, Hà Nội', '0912000003', '2021-06-01', 'ROLE_WAREHOUSE', 'ACTIVE', 0.00, 1),
(4, 'inactive_staff', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Lê Thị Tạm Nghỉ', '1998-03-25', 'Nữ', '12 Chùa Bộc, Đống Đa, Hà Nội', '0912000004', '2023-01-10', 'ROLE_SALES', 'INACTIVE', 0.00, 1)
ON CONFLICT (employee_id) DO NOTHING;

-- 4. customers
INSERT INTO customers (customer_id, full_name, date_of_birth, gender, phone, address, note) VALUES
(1, 'Nguyễn Văn An', '1988-04-12', 'Nam', '0912345678', '12 Nguyễn Trãi, Thanh Xuân, Hà Nội', 'Khách hàng thân thiết'),
(2, 'Trần Thị Mai', '1995-09-22', 'Nữ', '0987654321', '45 Cầu Giấy, Hà Nội', 'Dị ứng Aspirin nhẹ'),
(3, 'Lê Hoàng Nam', '1970-12-05', 'Nam', '0903123456', '88 Hoàng Hoa Thám, Ba Đình, Hà Nội', 'Cao huyết áp định kỳ'),
(4, 'Phạm Thu Trang', '2000-01-18', 'Nữ', '0978999888', '24 Thái Hà, Đống Đa, Hà Nội', ''),
(5, 'Vũ Đình Khang', '1965-06-30', 'Nam', '0933222111', '102 Xã Đàn, Hà Nội', 'Khách cao tuổi')
ON CONFLICT (customer_id) DO NOTHING;

-- 5. suppliers
INSERT INTO suppliers (supplier_id, supplier_name, address, tax_code, representative_name, phone) VALUES
(1, 'Cty CP Dược Hậu Giang (DHG Pharma)', '288 Bis Nguyễn Đình Chiểu, Q3, TP.HCM', '0300432145', 'Đoàn Đình Duy Khương', '02923891433'),
(2, 'Cty CP Traphaco', '75 Yên Ninh, Ba Đình, Hà Nội', '0100108656', 'Trần Túc Mã', '18006612'),
(3, 'Cty CP Dược phẩm Imexpharm', 'Số 4, Đường 30/4, Cao Lãnh, Đồng Tháp', '1400384433', 'Trần Thị Đào', '02773851941'),
(4, 'Cty Sanofi-Aventis Việt Nam', 'Số 10 Hàm Nghi, Bến Nghé, Q1, TP.HCM', '0302302830', 'Emin Turan', '02838298526'),
(5, 'Cty CP Dược phẩm Hà Tây (Hataphar)', '10A Quang Trung, Hà Đông, Hà Nội', '0500105315', 'Lê Văn Lớ', '02433522203')
ON CONFLICT (supplier_id) DO NOTHING;

-- 6. products (24 products)
INSERT INTO products (product_id, category_id, product_name, dosage, uses, dosage_form, unit, selling_price, image_url) VALUES
(1, 1, 'Panadol Extra Đỏ', 'Paracetamol 500mg, Caffeine 65mg', 'Giảm đau từ nhẹ đến vừa, hạ sốt, nhức đầu, đau răng, đau nhức cơ bắp.', 'Viên nén bao phim', 'Hộp', 25000.00, 'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=300'),
(2, 1, 'Efferalgan Sủi 500mg', 'Paracetamol 500mg', 'Hạ sốt nhanh chóng, giảm đau nhức toàn thân do cảm cúm.', 'Viên sủi', 'Hộp', 45000.00, 'https://images.unsplash.com/photo-1550572017-edd951aa8f72?w=300'),
(3, 2, 'Augmentin 1g GSK', 'Amoxicillin 875mg, Clavulanic 125mg', 'Kháng sinh điều trị nhiễm khuẩn đường hô hấp trên và dưới, nhiễm trùng tai mũi họng.', 'Viên nén', 'Hộp', 210000.00, 'https://images.unsplash.com/photo-1471864190281-a93a3070b6de?w=300'),
(4, 2, 'Klacid 500mg Abbott', 'Clarithromycin 500mg', 'Kháng sinh nhóm Macrolid điều trị viêm phổi, viêm xoang, viêm họng hạt.', 'Viên bao phim', 'Hộp', 350000.00, 'https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=300'),
(5, 3, 'Amlor 5mg Pfizer', 'Amlodipine 5mg', 'Điều trị tăng huyết áp vô căn, đau thắt ngực ổn định mạn tính.', 'Viên nang cứng', 'Hộp', 320000.00, 'https://images.unsplash.com/photo-1628771065518-0d82f1938462?w=300'),
(6, 3, 'Concor 5mg Merck', 'Bisoprolol fumarate 5mg', 'Chẹn beta điều trị tăng huyết áp, bệnh mạch vành, suy tim mạn tính.', 'Viên bao phim', 'Hộp', 180000.00, 'https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=300'),
(7, 4, 'Nexium Mups 40mg AstraZeneca', 'Esomeprazole 40mg', 'Điều trị viêm loét dạ dày tá tràng, hội chứng trào ngược dạ dày thực quản GERD.', 'Viên nén kháng acid', 'Hộp', 420000.00, 'https://images.unsplash.com/photo-1550572017-edd951aa8f72?w=300'),
(8, 4, 'Gaviscon Dual Action Reckitt', 'Sodium alginate 500mg', 'Tạo gel bảo vệ niêm mạc, giảm ợ nóng, ợ chua, trào ngược tức thì.', 'Gói hỗn dịch', 'Hộp', 165000.00, 'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=300'),
(9, 6, 'Berocca Performance Bayer', 'Vitamin nhóm B, C, Kẽm, Magie', 'Bổ sung vi chất, tăng cường năng lượng tỉnh táo, giảm mệt mỏi căng thẳng.', 'Viên sủi', 'Tuýp', 85000.00, 'https://images.unsplash.com/photo-1576602976047-174e57a47881?w=300'),
(10, 6, 'Vitamin C 500mg Boston', 'Acid ascorbic 500mg', 'Tăng cường sức đề kháng cơ thể, bảo vệ thành mạch, chống oxy hóa.', 'Viên nang', 'Lọ', 40000.00, 'https://images.unsplash.com/photo-1576602976047-174e57a47881?w=300'),
(11, 5, 'Telfast HD 180mg Sanofi', 'Fexofenadine hydrochloride 180mg', 'Thuốc kháng histamin trị viêm mũi dị ứng theo mùa, mày đay tự phát mạn tính.', 'Viên bao phim', 'Vỉ', 95000.00, 'https://images.unsplash.com/photo-1471864190281-a93a3070b6de?w=300'),
(12, 5, 'Singulair 10mg Organon', 'Montelukast sodium 10mg', 'Dự phòng và điều trị hen phế quản mạn tính, giảm triệu chứng viêm mũi dị ứng ban ngày ban đêm.', 'Viên bao phim', 'Hộp', 460000.00, 'https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=300'),
(13, 5, 'Acemuc 200mg Sanofi', 'Acetylcysteine 200mg', 'Làm tiêu chất nhầy đường hô hấp trong các bệnh viêm phế quản cấp và mạn tính.', 'Gói cốm', 'Hộp', 80000.00, 'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=300'),
(14, 5, 'Eugica Fort Mega We Care', 'Eucalyptol, Tinh dầu tràm, Gừng', 'Trị ho, đau họng, rát họng, khản tiếng, cảm cúm đường hô hấp trên.', 'Viên nang mềm', 'Hộp', 72000.00, 'https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=300'),
(15, 7, 'Salonpas Gel 30g Hisamitsu', 'Methyl salicylate, l-Menthol', 'Giảm đau cơ bắp, mỏi vai gáy, bầm tím, bong gân thể thao.', 'Gel bôi ngoài da', 'Tuýp', 38000.00, 'https://images.unsplash.com/photo-1550572017-edd951aa8f72?w=300'),
(16, 7, 'Voltaren Emulgel 20g GSK', 'Diclofenac diethylamine 1.16%', 'Chống viêm giảm đau tại chỗ trong thoái hóa khớp, viêm gân màng bao.', 'Gel bôi ngoài da', 'Tuýp', 75000.00, 'https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=300'),
(17, 8, 'Fucidin 2% Cream 15g LEO', 'Acid fusidic 20mg/g', 'Điều trị các bệnh nhiễm trùng da như chốc lở, nhọt, viêm nang lông.', 'Kem bôi da', 'Tuýp', 115000.00, 'https://images.unsplash.com/photo-1576602976047-174e57a47881?w=300'),
(18, 8, 'Bepanthen Balm 30g Bayer', 'Dexpanthenol 5%', 'Chăm sóc và bảo vệ làn da nhạy cảm của trẻ sơ sinh, chống hăm tã, nứt núm vú.', 'Thuốc mỡ', 'Tuýp', 65000.00, 'https://images.unsplash.com/photo-1550572017-edd951aa8f72?w=300'),
(19, 4, 'Smecta 3g Ipsen', 'Diosmectite 3g', 'Điều trị tiêu chảy cấp và mạn tính ở người lớn và trẻ em.', 'Bột pha hỗn dịch', 'Hộp', 110000.00, 'https://images.unsplash.com/photo-1471864190281-a93a3070b6de?w=300'),
(20, 4, 'Oresol 245 Boston', 'Glucose, Natri clorid, Kali clorid', 'Bù nước và điện giải kịp thời trong sốt cao, tiêu chảy mất nước.', 'Gói bột', 'Hộp', 35000.00, 'https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=300'),
(21, 7, 'Glucosamine 1500mg Kirkland', 'Glucosamine HCl 1500mg, MSM', 'Bổ sung chất nhờn dịch khớp, tái tạo sụn khớp, giảm thoái hóa khớp.', 'Viên nén', 'Lọ', 650000.00, 'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=300'),
(22, 4, 'Berberin 100mg Dược Hà Tĩnh', 'Berberin clorid 100mg', 'Kháng khuẩn đường ruột, trị lỵ trực trùng, viêm ruột tiêu chảy.', 'Viên nén', 'Lọ', 20000.00, 'https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=300'),
(23, 1, 'Paracetamol 500mg Traphaco', 'Paracetamol 500mg', 'Hạ sốt, giảm các chứng đau đầu, đau cơ khớp nhẹ.', 'Viên nén', 'Vỉ', 12000.00, 'https://images.unsplash.com/photo-1550572017-edd951aa8f72?w=300'),
(24, 2, 'Cefixim 200mg US Pharma', 'Cefixime 200mg', 'Kháng sinh Cephalosporin thế hệ 3 điều trị nhiễm khuẩn tiết niệu, tai giữa.', 'Viên nang', 'Hộp', 125000.00, 'https://images.unsplash.com/photo-1628771065518-0d82f1938462?w=300')
ON CONFLICT (product_id) DO NOTHING;

-- 7. batches
INSERT INTO batches (batch_id, product_id, batch_code, mfg_date, exp_date) VALUES
(1, 1, 'LOT-PAN-2024A', '2024-01-15', '2027-01-15'),
(2, 1, 'LOT-PAN-2024B', '2024-06-10', '2027-06-10'),
(3, 2, 'LOT-EFF-2024A', '2024-02-20', '2027-02-20'),
(4, 3, 'LOT-AUG-2024A', '2024-03-01', '2026-12-31'),
(5, 3, 'LOT-AUG-2024B', '2024-05-15', '2027-05-15'),
(6, 4, 'LOT-KLA-2024A', '2024-01-10', '2027-01-10'),
(7, 5, 'LOT-AML-2024A', '2024-02-05', '2028-02-05'),
(8, 6, 'LOT-CON-2024A', '2024-04-12', '2027-04-12'),
(9, 7, 'LOT-NEX-2024A', '2024-03-25', '2027-03-25'),
(10, 8, 'LOT-GAV-2024A', '2024-05-01', '2027-05-01'),
(11, 9, 'LOT-BER-2024A', '2024-06-01', '2027-06-01'),
(12, 10, 'LOT-VTC-2024A', '2024-01-18', '2027-01-18'),
(13, 11, 'LOT-TEL-2024A', '2024-02-14', '2027-02-14'),
(14, 12, 'LOT-SIN-2024A', '2024-04-20', '2027-04-20'),
(15, 13, 'LOT-ACE-2024A', '2024-03-10', '2027-03-10'),
(16, 14, 'LOT-EUG-2024A', '2024-05-12', '2027-05-12'),
(17, 15, 'LOT-SAL-2024A', '2024-01-22', '2028-01-22'),
(18, 16, 'LOT-VOL-2024A', '2024-02-18', '2027-02-18'),
(19, 17, 'LOT-FUC-2024A', '2024-03-30', '2027-03-30'),
(20, 18, 'LOT-BEP-2024A', '2024-04-15', '2027-04-15'),
(21, 19, 'LOT-SME-2024A', '2024-02-28', '2027-02-28'),
(22, 20, 'LOT-ORE-2024A', '2024-01-05', '2027-01-05'),
(23, 21, 'LOT-GLU-2024A', '2024-03-12', '2028-03-12'),
(24, 22, 'LOT-BBR-2024A', '2024-04-01', '2027-04-01'),
(25, 23, 'LOT-PAR-2024A', '2024-01-10', '2027-01-10'),
(26, 24, 'LOT-CEF-2024A', '2024-05-20', '2027-05-20'),
-- Expired batch for testing validation
(27, 1, 'LOT-PAN-EXPIRED', '2021-01-01', '2023-01-01')
ON CONFLICT (batch_id) DO NOTHING;

-- 8. inventories (Stock in Kho Tổng Hà Nội = 1 & Kho Cầu Giấy = 2)
INSERT INTO inventories (warehouse_id, batch_id, quantity, updated_at) VALUES
(1, 1, 150, CURRENT_TIMESTAMP),
(1, 2, 80, CURRENT_TIMESTAMP),
(1, 3, 200, CURRENT_TIMESTAMP),
(1, 4, 120, CURRENT_TIMESTAMP),
(1, 5, 90, CURRENT_TIMESTAMP),
(1, 6, 75, CURRENT_TIMESTAMP),
(1, 7, 110, CURRENT_TIMESTAMP),
(1, 8, 140, CURRENT_TIMESTAMP),
(1, 9, 60, CURRENT_TIMESTAMP),
(1, 10, 180, CURRENT_TIMESTAMP),
(1, 11, 220, CURRENT_TIMESTAMP),
(1, 12, 130, CURRENT_TIMESTAMP),
(1, 13, 105, CURRENT_TIMESTAMP),
(1, 14, 50, CURRENT_TIMESTAMP),
(1, 15, 160, CURRENT_TIMESTAMP),
(1, 16, 175, CURRENT_TIMESTAMP),
(1, 17, 190, CURRENT_TIMESTAMP),
(1, 18, 95, CURRENT_TIMESTAMP),
(1, 19, 85, CURRENT_TIMESTAMP),
(1, 20, 140, CURRENT_TIMESTAMP),
(1, 21, 210, CURRENT_TIMESTAMP),
(1, 22, 300, CURRENT_TIMESTAMP),
(1, 23, 45, CURRENT_TIMESTAMP),
(1, 24, 250, CURRENT_TIMESTAMP),
(1, 25, 320, CURRENT_TIMESTAMP),
(1, 26, 115, CURRENT_TIMESTAMP),
(1, 27, 10, CURRENT_TIMESTAMP),
-- Kho 2
(2, 1, 50, CURRENT_TIMESTAMP),
(2, 2, 40, CURRENT_TIMESTAMP),
(2, 3, 60, CURRENT_TIMESTAMP),
(2, 4, 30, CURRENT_TIMESTAMP)
ON CONFLICT (warehouse_id, batch_id) DO UPDATE SET quantity = EXCLUDED.quantity;

-- Reset sequence values
SELECT setval('categories_category_id_seq', (SELECT MAX(category_id) FROM categories));
SELECT setval('warehouses_warehouse_id_seq', (SELECT MAX(warehouse_id) FROM warehouses));
SELECT setval('employees_employee_id_seq', (SELECT MAX(employee_id) FROM employees));
SELECT setval('customers_customer_id_seq', (SELECT MAX(customer_id) FROM customers));
SELECT setval('suppliers_supplier_id_seq', (SELECT MAX(supplier_id) FROM suppliers));
SELECT setval('products_product_id_seq', (SELECT MAX(product_id) FROM products));
SELECT setval('batches_batch_id_seq', (SELECT MAX(batch_id) FROM batches));
