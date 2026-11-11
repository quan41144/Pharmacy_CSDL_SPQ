# BÁO CÁO CƠ SỞ DỮ LIỆU: TỔNG HỢP CÂU LỆNH SQL THEO 12 CONTROLLER
> **Hệ thống Quản lý Nhà thuốc (Pharmacy Management System)**  
> **Đặc tả kỹ thuật:** 100% SQL thuần (Standard SQL / DML & DQL). **Không** sử dụng VIEW, STORED PROCEDURE, hay TRIGGER.

---

## MỤC LỤC 12 CONTROLLER
1. [AuthController](#1-authcontroller-xác-thực--phân-quyền)
2. [CategoryController](#2-categorycontroller-quản-lý-danh-mục-thuốc)
3. [WarehouseController](#3-warehousecontroller-quản-lý-kho-hàng)
4. [CustomerController](#4-customercontroller-quản-lý-khách-hàng)
5. [SupplierController](#5-suppliercontroller-quản-lý-nhà-cung-cấp)
6. [EmployeeController](#6-employeecontroller-quản-lý-nhân-sự--hồ-sơ)
7. [ProductController](#7-productcontroller-quản-lý-thuốc--tồn-kho)
8. [BatchController](#8-batchcontroller-tra-cứu-lô-hàng--fefo)
9. [InvoiceController](#9-invoicecontroller-quản-lý-bán-hàng--hóa-đơn)
10. [ImportSlipController](#10-importslipcontroller-quản-lý-nhập-kho-nhiều-lô)
11. [DisposalSlipController](#11-disposalslipcontroller-quản-lý-tiêu-hủy-thuốc-nhiều-lô)
12. [ReportController](#12-reportcontroller-báo-cáo-thống-kê--doanh-thu)

---

## 1. AuthController (Xác thực & Phân quyền)

### 1.1. Chức năng `login` (`POST /api/v1/auth/login`)
Truy vấn thông tin tài khoản nhân viên theo tên đăng nhập để ứng dụng đối soát mật khẩu đã mã hóa:
```sql
SELECT employee_id, username, password, full_name, date_of_birth, gender, 
       address, phone, start_date, position, work_status, balance, warehouse_id
FROM employees
WHERE LOWER(username) = LOWER(:username);
```

### 1.2. Chức năng `refreshToken` (`POST /api/v1/auth/refresh`)
Kiểm tra hiệu lực và vai trò của tài khoản khi người dùng yêu cầu cấp lại JWT Token:
```sql
SELECT employee_id, username, position, work_status, balance, warehouse_id
FROM employees
WHERE username = :username;
```

### 1.3. Chức năng `getCurrentUser` (`GET /api/v1/auth/me`)
Lấy thông tin người dùng đang đăng nhập kèm tên kho phụ trách:
```sql
SELECT e.employee_id, e.username, e.full_name, e.position, e.balance,
       e.warehouse_id, w.warehouse_name
FROM employees e
LEFT JOIN warehouses w ON e.warehouse_id = w.warehouse_id
WHERE e.username = :currentUsername;
```

---

## 2. CategoryController (Quản lý Danh mục thuốc)

### 2.1. Lấy toàn bộ danh mục (`GET /api/v1/categories`)
```sql
SELECT category_id, category_name, description 
FROM categories 
ORDER BY category_id ASC;
```

### 2.2. Thêm mới danh mục (`POST /api/v1/categories`)
```sql
INSERT INTO categories (category_name, description) 
VALUES (:categoryName, :description)
RETURNING category_id;
```

### 2.3. Cập nhật danh mục (`POST /api/v1/categories` với id)
```sql
UPDATE categories 
SET category_name = :categoryName, 
    description = :description 
WHERE category_id = :categoryId;
```

### 2.4. Xóa danh mục (`DELETE /api/v1/categories/{id}`)
```sql
DELETE FROM categories 
WHERE category_id = :categoryId;
```

---

## 3. WarehouseController (Quản lý Kho hàng)

### 3.1. Lấy tất cả kho hàng (`GET /api/v1/warehouses`)
```sql
SELECT warehouse_id, warehouse_name, warehouse_address, status 
FROM warehouses 
ORDER BY warehouse_id ASC;
```

### 3.2. Lấy danh sách kho đang hoạt động (`GET /api/v1/warehouses/active`)
```sql
SELECT warehouse_id, warehouse_name, warehouse_address, status 
FROM warehouses 
WHERE status = 'ACTIVE' 
ORDER BY warehouse_id ASC;
```

### 3.3. Thêm mới kho hàng (`POST /api/v1/warehouses`)
```sql
INSERT INTO warehouses (warehouse_name, warehouse_address, status) 
VALUES (:warehouseName, :warehouseAddress, COALESCE(:status, 'ACTIVE'))
RETURNING warehouse_id;
```

### 3.4. Cập nhật thông tin kho (`POST /api/v1/warehouses` với id)
```sql
UPDATE warehouses 
SET warehouse_name = :warehouseName, 
    warehouse_address = :warehouseAddress, 
    status = :status 
WHERE warehouse_id = :warehouseId;
```

---

## 4. CustomerController (Quản lý Khách hàng)

### 4.1. Tìm kiếm nhanh khách hàng (`GET /api/v1/customers/live-search`)
Hỗ trợ tìm kiếm theo Họ tên, Số điện thoại hoặc Ngày sinh:
```sql
SELECT customer_id, full_name, date_of_birth, gender, phone, address, note,
       EXTRACT(YEAR FROM age(CURRENT_DATE, date_of_birth))::int AS age
FROM customers
WHERE :keyword IS NULL OR :keyword = ''
   OR LOWER(full_name) LIKE LOWER('%' || :keyword || '%')
   OR phone LIKE '%' || :keyword || '%'
   OR CAST(date_of_birth AS text) LIKE '%' || :keyword || '%'
ORDER BY customer_id DESC;
```

### 4.2. Lấy danh sách toàn bộ khách hàng (`GET /api/v1/customers`)
```sql
SELECT customer_id, full_name, date_of_birth, gender, phone, address, note,
       EXTRACT(YEAR FROM age(CURRENT_DATE, date_of_birth))::int AS age
FROM customers 
ORDER BY customer_id ASC;
```

### 4.3. Xem chi tiết khách hàng theo ID (`GET /api/v1/customers/{id}`)
```sql
SELECT customer_id, full_name, date_of_birth, gender, phone, address, note,
       EXTRACT(YEAR FROM age(CURRENT_DATE, date_of_birth))::int AS age
FROM customers 
WHERE customer_id = :customerId;
```

### 4.4. Thêm mới khách hàng (`POST /api/v1/customers`)
```sql
INSERT INTO customers (full_name, date_of_birth, gender, phone, address, note)
VALUES (:fullName, :dateOfBirth, :gender, :phone, :address, :note)
RETURNING customer_id;
```

### 4.5. Cập nhật khách hàng (`POST /api/v1/customers` với id)
```sql
UPDATE customers
SET full_name = :fullName, 
    date_of_birth = :dateOfBirth, 
    gender = :gender,
    phone = :phone, 
    address = :address, 
    note = :note
WHERE customer_id = :customerId;
```

### 4.6. Xóa khách hàng (`DELETE /api/v1/customers/{id}`)
```sql
DELETE FROM customers 
WHERE customer_id = :customerId;
```

---

## 5. SupplierController (Quản lý Nhà cung cấp)

### 5.1. Tìm kiếm nhanh nhà cung cấp (`GET /api/v1/suppliers/live-search`)
```sql
SELECT supplier_id, supplier_name, address, tax_code, representative_name, phone
FROM suppliers
WHERE :keyword IS NULL OR :keyword = ''
   OR LOWER(supplier_name) LIKE LOWER('%' || :keyword || '%')
   OR phone LIKE '%' || :keyword || '%'
   OR tax_code LIKE '%' || :keyword || '%'
   OR LOWER(representative_name) LIKE LOWER('%' || :keyword || '%')
   OR LOWER(address) LIKE LOWER('%' || :keyword || '%')
ORDER BY supplier_id ASC;
```

### 5.2. Lấy danh sách toàn bộ nhà cung cấp (`GET /api/v1/suppliers`)
```sql
SELECT supplier_id, supplier_name, address, tax_code, representative_name, phone
FROM suppliers
ORDER BY supplier_id ASC;
```

### 5.3. Thêm mới nhà cung cấp (`POST /api/v1/suppliers`)
```sql
INSERT INTO suppliers (supplier_name, address, tax_code, representative_name, phone)
VALUES (:supplierName, :address, :taxCode, :representativeName, :phone)
RETURNING supplier_id;
```

### 5.4. Cập nhật nhà cung cấp (`POST /api/v1/suppliers` với id)
```sql
UPDATE suppliers
SET supplier_name = :supplierName, 
    address = :address,
    tax_code = :taxCode, 
    representative_name = :representativeName, 
    phone = :phone
WHERE supplier_id = :supplierId;
```

### 5.5. Xóa nhà cung cấp (`DELETE /api/v1/suppliers/{id}`)
```sql
DELETE FROM suppliers 
WHERE supplier_id = :supplierId;
```

---

## 6. EmployeeController (Quản lý Nhân sự & Hồ sơ)

### 6.1. Lấy danh sách toàn bộ nhân viên (`GET /api/v1/employees`)
```sql
SELECT e.employee_id, e.username, e.full_name, e.date_of_birth, e.gender,
       e.address, e.phone, e.start_date, e.position, e.work_status, e.balance,
       e.warehouse_id, w.warehouse_name,
       EXTRACT(YEAR FROM age(CURRENT_DATE, e.date_of_birth))::int AS age
FROM employees e
LEFT JOIN warehouses w ON e.warehouse_id = w.warehouse_id
ORDER BY e.employee_id ASC;
```

### 6.2. Xem thông tin nhân viên theo ID (`GET /api/v1/employees/{id}`)
```sql
SELECT e.employee_id, e.username, e.full_name, e.date_of_birth, e.gender,
       e.address, e.phone, e.start_date, e.position, e.work_status, e.balance,
       e.warehouse_id, w.warehouse_name,
       EXTRACT(YEAR FROM age(CURRENT_DATE, e.date_of_birth))::int AS age
FROM employees e
LEFT JOIN warehouses w ON e.warehouse_id = w.warehouse_id
WHERE e.employee_id = :employeeId;
```

### 6.3. Lấy số dư quỹ của tài khoản Quản trị viên (`GET /api/v1/employees/admin-balance`)
```sql
SELECT COALESCE(balance, 0.00) 
FROM employees 
WHERE position = 'ROLE_ADMIN' AND work_status = 'ACTIVE' 
LIMIT 1;
```

### 6.4. Thêm mới nhân viên (`POST /api/v1/employees`)
```sql
-- 1. Kiểm tra trùng lặp username
SELECT 1 FROM employees WHERE username = :username;

-- 2. Thêm mới bản ghi nhân viên
INSERT INTO employees (username, password, full_name, date_of_birth, gender, address, phone, start_date, position, work_status, balance, warehouse_id)
VALUES (:username, :password, :fullName, :dateOfBirth, :gender, :address, :phone, CURRENT_DATE, :position, 'ACTIVE', 0.00, :warehouseId)
RETURNING employee_id;
```

### 6.5. Cập nhật thông tin nhân viên (`POST /api/v1/employees` với id)
```sql
UPDATE employees
SET full_name = :fullName, 
    date_of_birth = :dateOfBirth, 
    gender = :gender,
    address = :address, 
    phone = :phone, 
    position = :position,
    work_status = :workStatus, 
    warehouse_id = :warehouseId,
    password = COALESCE(:password, password),
    balance = CASE WHEN :position = 'ROLE_ADMIN' AND :balance IS NOT NULL THEN :balance ELSE balance END
WHERE employee_id = :employeeId;
```

### 6.6. Bật/tắt trạng thái làm việc (`PATCH /api/v1/employees/{id}/toggle-status`)
```sql
UPDATE employees
SET work_status = CASE WHEN work_status = 'ACTIVE' THEN 'INACTIVE' ELSE 'ACTIVE' END
WHERE employee_id = :employeeId;
```

### 6.7. Xem & cập nhật hồ sơ cá nhân (`GET` & `PUT /api/v1/employees/profile`)
```sql
-- Xem hồ sơ:
SELECT e.employee_id, e.username, e.full_name, e.date_of_birth, e.gender,
       e.address, e.phone, e.start_date, e.position, e.work_status, e.balance,
       e.warehouse_id, w.warehouse_name
FROM employees e
LEFT JOIN warehouses w ON e.warehouse_id = w.warehouse_id
WHERE e.username = :currentUsername;

-- Cập nhật thông tin:
UPDATE employees
SET full_name = COALESCE(:fullName, full_name),
    date_of_birth = COALESCE(:dateOfBirth, date_of_birth),
    gender = COALESCE(:gender, gender),
    phone = COALESCE(:phone, phone),
    address = COALESCE(:address, address),
    password = COALESCE(:newPassword, password)
WHERE username = :currentUsername;
```

---

## 7. ProductController (Quản lý Thuốc & Tồn kho)

### 7.1. Tìm kiếm và phân trang danh sách thuốc (`GET /api/v1/products`)
> **Điểm ưu việt:** Dùng câu truy vấn con (Subquery) lồng tính tổng số lượng tồn kho của các lô còn hạn (`exp_date > CURRENT_DATE`), hoàn toàn **không cần VIEW**:
```sql
SELECT p.product_id, p.product_name, p.dosage, p.uses, p.dosage_form, p.unit, p.selling_price, p.image_url,
       c.category_id, c.category_name,
       COALESCE(
           (SELECT SUM(i.quantity)
            FROM inventories i
            JOIN batches b ON i.batch_id = b.batch_id
            WHERE b.product_id = p.product_id
              AND (:warehouseId IS NULL OR i.warehouse_id = :warehouseId)
              AND b.exp_date > CURRENT_DATE), 0
       ) AS total_stock
FROM products p
JOIN categories c ON p.category_id = c.category_id
WHERE (:keyword IS NULL OR LOWER(p.product_name) LIKE LOWER('%' || :keyword || '%') OR LOWER(p.uses) LIKE LOWER('%' || :keyword || '%'))
  AND (:categoryId IS NULL OR p.category_id = :categoryId)
ORDER BY p.product_id ASC
LIMIT :pageSize OFFSET :offset;
```

### 7.2. Tìm kiếm nhanh thuốc cho Autocomplete / POS (`GET /api/v1/products/live-search`)
```sql
SELECT p.product_id, p.product_name, p.dosage, p.uses, p.dosage_form, p.unit, p.selling_price, p.image_url,
       c.category_id, c.category_name,
       COALESCE(
           (SELECT SUM(i.quantity) 
            FROM inventories i 
            JOIN batches b ON i.batch_id = b.batch_id 
            WHERE b.product_id = p.product_id AND b.exp_date > CURRENT_DATE), 0
       ) AS total_stock
FROM products p
JOIN categories c ON p.category_id = c.category_id
WHERE :keyword IS NULL OR :keyword = ''
   OR LOWER(p.product_name) LIKE LOWER('%' || :keyword || '%')
   OR LOWER(p.uses) LIKE LOWER('%' || :keyword || '%')
   OR LOWER(p.dosage) LIKE LOWER('%' || :keyword || '%')
   OR LOWER(c.category_name) LIKE LOWER('%' || :keyword || '%')
ORDER BY p.product_id ASC;
```

### 7.3. Xem chi tiết thông tin thuốc & các lô trong kho (`GET /api/v1/products/{id}`)
```sql
-- Thông tin chung:
SELECT p.product_id, p.product_name, p.dosage, p.uses, p.dosage_form, p.unit, p.selling_price, p.image_url,
       c.category_id, c.category_name
FROM products p
JOIN categories c ON p.category_id = c.category_id
WHERE p.product_id = :productId;

-- Danh sách các lô hàng trong kho:
SELECT b.batch_id, b.batch_code, b.mfg_date, b.exp_date,
       i.warehouse_id, w.warehouse_name, i.quantity,
       CASE 
           WHEN b.exp_date <= CURRENT_DATE THEN 'EXPIRED'
           WHEN b.exp_date <= CURRENT_DATE + INTERVAL '30 days' THEN 'NEAR_EXPIRY'
           ELSE 'VALID'
       END AS status
FROM batches b
JOIN inventories i ON b.batch_id = i.batch_id
JOIN warehouses w ON i.warehouse_id = w.warehouse_id
WHERE b.product_id = :productId
  AND (:warehouseId IS NULL OR i.warehouse_id = :warehouseId)
ORDER BY b.exp_date ASC;
```

### 7.4. Thêm mới, cập nhật, xóa thuốc (`POST` / `DELETE /api/v1/products`)
```sql
-- Thêm mới thuốc:
INSERT INTO products (category_id, product_name, dosage, uses, dosage_form, unit, selling_price, image_url)
VALUES (:categoryId, :productName, :dosage, :uses, :dosageForm, :unit, :sellingPrice, :imageUrl)
RETURNING product_id;

-- Cập nhật thuốc:
UPDATE products
SET category_id = :categoryId, 
    product_name = :productName, 
    dosage = :dosage,
    uses = :uses, 
    dosage_form = :dosageForm, 
    unit = :unit,
    selling_price = :sellingPrice, 
    image_url = :imageUrl
WHERE product_id = :productId;

-- Xóa thuốc:
DELETE FROM products 
WHERE product_id = :productId;
```

---

## 8. BatchController (Tra cứu Lô hàng & FEFO)

### 8.1. Lấy danh sách lô thuốc xuất bán theo chuẩn FEFO (`GET /api/v1/batches/fefo`)
> **Tiêu chuẩn FEFO (First Expired, First Out):** Ưu tiên xuất lô thuốc còn hạn (`exp_date > CURRENT_DATE`), còn tồn kho (`quantity > 0`), hạn gần nhất xuất trước (`ORDER BY b.exp_date ASC`):
```sql
SELECT b.batch_id, b.batch_code, b.mfg_date, b.exp_date,
       p.product_id, p.product_name, p.unit, p.selling_price,
       i.quantity, w.warehouse_id, w.warehouse_name
FROM batches b
JOIN inventories i ON b.batch_id = i.batch_id
JOIN warehouses w ON i.warehouse_id = w.warehouse_id
JOIN products p ON b.product_id = p.product_id
WHERE i.warehouse_id = :warehouseId
  AND b.product_id = :productId
  AND i.quantity > 0
  AND b.exp_date > CURRENT_DATE
ORDER BY b.exp_date ASC;
```

### 8.2. Tìm kiếm lô thuốc để đề xuất tiêu hủy (`GET /api/v1/batches/search-disposal`)
```sql
SELECT b.batch_id, b.batch_code, b.mfg_date, b.exp_date,
       p.product_id, p.product_name, p.unit,
       i.quantity, w.warehouse_id, w.warehouse_name,
       CASE
           WHEN b.exp_date <= CURRENT_DATE THEN 'Hết hạn'
           WHEN b.exp_date <= CURRENT_DATE + INTERVAL '30 days' THEN 'Gần hết hạn'
           ELSE 'Còn hạn'
       END AS status
FROM batches b
JOIN inventories i ON b.batch_id = i.batch_id
JOIN warehouses w ON i.warehouse_id = w.warehouse_id
JOIN products p ON b.product_id = p.product_id
WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(b.batch_code) LIKE LOWER('%' || :keyword || '%') OR LOWER(p.product_name) LIKE LOWER('%' || :keyword || '%'))
  AND (:warehouseId IS NULL OR i.warehouse_id = :warehouseId)
  AND i.quantity > 0
ORDER BY b.exp_date ASC;
```

---

## 9. InvoiceController (Quản lý Bán hàng & Hóa đơn)

### 9.1. Lập hóa đơn bán hàng mới (`POST /api/v1/invoices`)
Khởi tạo hóa đơn với trạng thái ban đầu là `UNPAID` (chờ thanh toán):
```sql
-- Bước 1: Xác thực nhân viên bán hàng (ACTIVE, ROLE_SALES hoặc ROLE_ADMIN)
SELECT employee_id, full_name, work_status, position 
FROM employees 
WHERE username = :username 
  AND work_status = 'ACTIVE' 
  AND position IN ('ROLE_SALES', 'ROLE_ADMIN');

-- Bước 2: Kiểm tra kho hoạt động & khách hàng tồn tại
SELECT warehouse_id FROM warehouses WHERE warehouse_id = :warehouseId AND status = 'ACTIVE';
SELECT customer_id FROM customers WHERE customer_id = :customerId;

-- Bước 3: Tạo hóa đơn mới
INSERT INTO invoices (customer_id, sales_staff_id, warehouse_id, created_at, total_amount, status, note)
VALUES (:customerId, :salesStaffId, :warehouseId, CURRENT_TIMESTAMP, 0.00, 'UNPAID', :note)
RETURNING invoice_id;

-- Bước 4: Lấy giá bán niêm yết của từng lô thuốc
SELECT p.selling_price 
FROM batches b 
JOIN products p ON b.product_id = p.product_id 
WHERE b.batch_id = :batchId;

-- Bước 5: Chèn từng dòng mặt hàng vào hóa đơn
INSERT INTO invoice_items (invoice_id, batch_id, quantity, unit_price, sub_total)
VALUES (:invoiceId, :batchId, :quantity, :unitPrice, :quantity * :unitPrice);

-- Bước 6: Cập nhật tổng tiền hóa đơn
UPDATE invoices
SET total_amount = (
    SELECT COALESCE(SUM(sub_total), 0.00) 
    FROM invoice_items 
    WHERE invoice_id = :invoiceId
)
WHERE invoice_id = :invoiceId;
```

### 9.2. Xác nhận thanh toán & Trừ kho (`POST /api/v1/invoices/{id}/confirm-payment`)
Thực hiện trong 1 Transaction hoàn chỉnh bằng SQL thuần:
```sql
-- Bước 1: Khóa dòng hóa đơn để đảm bảo tính an toàn giao dịch
SELECT invoice_id, warehouse_id, status, total_amount
FROM invoices
WHERE invoice_id = :invoiceId
FOR UPDATE;

-- Bước 2: Kiểm tra hạn dùng và số lượng tồn kho từng lô
SELECT ii.batch_id, ii.quantity AS req_qty, b.exp_date, COALESCE(i.quantity, 0) AS cur_stock
FROM invoice_items ii
JOIN batches b ON b.batch_id = ii.batch_id
LEFT JOIN inventories i ON i.batch_id = ii.batch_id AND i.warehouse_id = :warehouseId
WHERE ii.invoice_id = :invoiceId;

-- Bước 3: Trừ số lượng tồn kho thực tế của từng lô tại kho bán
UPDATE inventories
SET quantity = quantity - :reqQty,
    updated_at = CURRENT_TIMESTAMP
WHERE warehouse_id = :warehouseId AND batch_id = :batchId;

-- Bước 4: Cập nhật trạng thái hóa đơn sang PAID
UPDATE invoices
SET status = 'PAID'
WHERE invoice_id = :invoiceId;
```

### 9.3. Hủy hóa đơn chưa thanh toán (`POST /api/v1/invoices/{id}/cancel`)
```sql
UPDATE invoices
SET status = 'CANCELLED'
WHERE invoice_id = :invoiceId AND status = 'UNPAID';
```

### 9.4. Lấy chi tiết hóa đơn (`GET /api/v1/invoices/{id}`)
```sql
-- Thông tin chung:
SELECT i.invoice_id, i.customer_id, c.full_name AS customer_name, c.phone AS customer_phone,
       i.sales_staff_id, e.full_name AS sales_staff_name,
       i.warehouse_id, w.warehouse_name,
       i.created_at, i.total_amount, i.status, i.note
FROM invoices i
LEFT JOIN customers c ON i.customer_id = c.customer_id
JOIN employees e ON i.sales_staff_id = e.employee_id
JOIN warehouses w ON i.warehouse_id = w.warehouse_id
WHERE i.invoice_id = :invoiceId;

-- Danh sách mặt hàng:
SELECT ii.batch_id, b.batch_code, p.product_id, p.product_name, p.unit,
       ii.quantity, ii.unit_price, ii.sub_total
FROM invoice_items ii
JOIN batches b ON ii.batch_id = b.batch_id
JOIN products p ON b.product_id = p.product_id
WHERE ii.invoice_id = :invoiceId;
```

### 9.5. Tìm kiếm danh sách hóa đơn (`GET /api/v1/invoices`)
```sql
SELECT i.invoice_id, i.customer_id, c.full_name AS customer_name, c.phone AS customer_phone,
       i.sales_staff_id, e.full_name AS sales_staff_name,
       i.warehouse_id, w.warehouse_name,
       i.created_at, i.total_amount, i.status, i.note
FROM invoices i
LEFT JOIN customers c ON i.customer_id = c.customer_id
JOIN employees e ON i.sales_staff_id = e.employee_id
JOIN warehouses w ON i.warehouse_id = w.warehouse_id
WHERE :keyword IS NULL OR :keyword = ''
   OR LOWER(c.full_name) LIKE LOWER('%' || :keyword || '%')
   OR c.phone LIKE '%' || :keyword || '%'
   OR (:idFromCode IS NOT NULL AND i.invoice_id = :idFromCode)
ORDER BY i.invoice_id DESC;
```

---

## 10. ImportSlipController (Quản lý Nhập kho nhiều lô)

### 10.1. Tạo phiếu nhập kho chứa nhiều lô thuốc (`POST /api/v1/import-slips`)
Cho phép nhập danh sách nhiều lô thuốc cùng nhà cung cấp và kho nhập:
```sql
-- Bước 1: Kiểm tra quyền nhân viên kho (ACTIVE, ROLE_WAREHOUSE hoặc ROLE_ADMIN)
SELECT employee_id FROM employees 
WHERE username = :username 
  AND work_status = 'ACTIVE' 
  AND position IN ('ROLE_WAREHOUSE', 'ROLE_ADMIN');

-- Bước 2: Kiểm tra kho hoạt động & nhà cung cấp tồn tại
SELECT warehouse_id FROM warehouses WHERE warehouse_id = :warehouseId AND status = 'ACTIVE';
SELECT supplier_id FROM suppliers WHERE supplier_id = :supplierId;

-- Bước 3: Duyệt danh sách các lô thuốc cần nhập (items)
-- Kiểm tra xem số lô đã có trong hệ thống chưa:
SELECT batch_id FROM batches WHERE batch_code = :batchCode;
-- Nếu chưa có số lô này, thực hiện thêm mới vào bảng batches:
INSERT INTO batches (product_id, batch_code, mfg_date, exp_date)
VALUES (:productId, :batchCode, :mfgDate, :expDate)
RETURNING batch_id;

-- Bước 4: Tạo phiếu nhập ở trạng thái PENDING
INSERT INTO import_slips (supplier_id, warehouse_staff_id, warehouse_id, import_date, total_amount, status)
VALUES (:supplierId, :staffId, :warehouseId, CURRENT_TIMESTAMP, :totalAmount, 'PENDING')
RETURNING import_slip_id;

-- Bước 5: Chèn chi tiết từng lô thuốc vào phiếu nhập (import_slip_items)
INSERT INTO import_slip_items (import_slip_id, batch_id, quantity, unit_price, sub_total)
VALUES (:importSlipId, :batchId, :quantity, :unitPrice, :quantity * :unitPrice);
```

### 10.2. Duyệt phiếu nhập kho & Tăng tồn kho (`POST /api/v1/import-slips/{id}/confirm`)
Cộng dồn tồn kho cho tất cả các lô thuốc trong phiếu bằng câu lệnh **UPSERT** thuần:
```sql
-- Bước 1: Khóa dòng phiếu nhập để kiểm tra trạng thái PENDING
SELECT import_slip_id, warehouse_id, status, total_amount
FROM import_slips
WHERE import_slip_id = :importSlipId
FOR UPDATE;

-- Bước 2: Tăng số lượng tồn kho cho các lô (UPSERT)
INSERT INTO inventories (warehouse_id, batch_id, quantity, updated_at)
SELECT :warehouseId, batch_id, quantity, CURRENT_TIMESTAMP
FROM import_slip_items
WHERE import_slip_id = :importSlipId
ON CONFLICT (warehouse_id, batch_id)
DO UPDATE SET
    quantity = inventories.quantity + EXCLUDED.quantity,
    updated_at = CURRENT_TIMESTAMP;

-- Bước 3: Chuyển trạng thái phiếu nhập sang IMPORTED
UPDATE import_slips
SET status = 'IMPORTED'
WHERE import_slip_id = :importSlipId;
```

### 10.3. Hủy phiếu nhập kho (`POST /api/v1/import-slips/{id}/cancel`)
```sql
UPDATE import_slips
SET status = 'CANCELLED'
WHERE import_slip_id = :importSlipId AND status = 'PENDING';
```

### 10.4. Lấy danh sách phiếu nhập & Chi tiết các lô (`GET /api/v1/import-slips`)
```sql
-- Danh sách phiếu nhập:
SELECT s.import_slip_id, s.supplier_id, sup.supplier_name,
       s.warehouse_staff_id, e.full_name AS warehouse_staff_name,
       s.warehouse_id, w.warehouse_name,
       s.import_date, s.total_amount, s.status
FROM import_slips s
JOIN suppliers sup ON s.supplier_id = sup.supplier_id
JOIN employees e ON s.warehouse_staff_id = e.employee_id
JOIN warehouses w ON s.warehouse_id = w.warehouse_id
WHERE (:status IS NULL OR s.status = :status)
ORDER BY s.import_slip_id DESC;

-- Chi tiết các lô hàng trong phiếu nhập:
SELECT isi.batch_id, b.batch_code, b.mfg_date, b.exp_date,
       p.product_id, p.product_name, p.unit,
       isi.quantity, isi.unit_price, isi.sub_total
FROM import_slip_items isi
JOIN batches b ON isi.batch_id = b.batch_id
JOIN products p ON b.product_id = p.product_id
WHERE isi.import_slip_id = :importSlipId;
```

---

## 11. DisposalSlipController (Quản lý Tiêu hủy thuốc nhiều lô)

### 11.1. Lập và xác nhận phiếu hủy nhiều lô hàng (`POST /api/v1/disposal-slips`)
Cho phép tiêu hủy đồng thời nhiều lô thuốc trong cùng 1 phiếu:
```sql
-- Bước 1: Kiểm tra quyền nhân viên kho và trạng thái kho
SELECT employee_id FROM employees 
WHERE username = :username 
  AND work_status = 'ACTIVE' 
  AND position IN ('ROLE_WAREHOUSE', 'ROLE_ADMIN');

SELECT warehouse_id FROM warehouses WHERE warehouse_id = :warehouseId AND status = 'ACTIVE';

-- Bước 2: Khóa và kiểm tra số lượng tồn kho của từng lô cần hủy
SELECT quantity 
FROM inventories 
WHERE warehouse_id = :warehouseId AND batch_id = :batchId 
FOR UPDATE;

-- Bước 3: Tạo phiếu hủy hàng ở trạng thái DISPOSED
INSERT INTO disposal_slips (warehouse_staff_id, warehouse_id, disposal_date, status)
VALUES (:staffId, :warehouseId, CURRENT_TIMESTAMP, 'DISPOSED')
RETURNING disposal_slip_id;

-- Bước 4: Chèn từng dòng chi tiết hủy lô thuốc (disposal_slip_items)
INSERT INTO disposal_slip_items (disposal_slip_id, batch_id, quantity, reason)
VALUES (:disposalSlipId, :batchId, :quantity, :reason);

-- Bước 5: Trừ số lượng tồn kho tương ứng của từng lô
UPDATE inventories
SET quantity = quantity - :quantity,
    updated_at = CURRENT_TIMESTAMP
WHERE warehouse_id = :warehouseId AND batch_id = :batchId;
```

### 11.2. Lấy danh sách phiếu tiêu hủy & Chi tiết các lô (`GET /api/v1/disposal-slips`)
```sql
-- Danh sách phiếu hủy:
SELECT ds.disposal_slip_id, ds.warehouse_staff_id, e.full_name AS warehouse_staff_name,
       ds.warehouse_id, w.warehouse_name, ds.disposal_date, ds.status
FROM disposal_slips ds
JOIN employees e ON ds.warehouse_staff_id = e.employee_id
JOIN warehouses w ON ds.warehouse_id = w.warehouse_id
ORDER BY ds.disposal_slip_id DESC;

-- Chi tiết các lô thuốc tiêu hủy:
SELECT dsi.batch_id, b.batch_code, p.product_id, p.product_name, p.unit,
       dsi.quantity, dsi.reason
FROM disposal_slip_items dsi
JOIN batches b ON dsi.batch_id = b.batch_id
JOIN products p ON b.product_id = p.product_id
WHERE dsi.disposal_slip_id = :disposalSlipId;
```

---

## 12. ReportController (Báo cáo Thống kê & Doanh thu)

### 12.1. Báo cáo các chỉ số tài chính KPI (`GET /api/v1/reports/kpis`)
```sql
-- 1. Tổng tiền nhập hàng đã duyệt (giá vốn hàng nhập) trong kỳ
SELECT COALESCE(SUM(total_amount), 0.00)
FROM import_slips
WHERE status = 'IMPORTED' 
  AND import_date >= :startDate 
  AND import_date <= :endDate;

-- 2. Tổng doanh thu thuần từ các hóa đơn đã thanh toán (PAID) trong kỳ
SELECT COALESCE(SUM(total_amount), 0.00)
FROM invoices
WHERE status = 'PAID' 
  AND created_at >= :startDate 
  AND created_at <= :endDate;

-- 3. Số dư tài khoản Quản trị viên
SELECT COALESCE(balance, 0.00) 
FROM employees 
WHERE position = 'ROLE_ADMIN' AND work_status = 'ACTIVE' 
LIMIT 1;

-- 4. Thống kê số lượng chứng từ phát sinh trong kỳ
SELECT COUNT(*) FROM invoices WHERE created_at >= :startDate AND created_at <= :endDate;
SELECT COUNT(*) FROM import_slips WHERE import_date >= :startDate AND import_date <= :endDate;
SELECT COUNT(*) FROM disposal_slips;
```

### 12.2. Lấy danh sách hóa đơn theo kỳ báo cáo (`GET /api/v1/reports/invoices`)
```sql
-- Truy vấn tất cả hóa đơn hiển thị trên giao diện:
SELECT i.invoice_id, i.customer_id, c.full_name AS customer_name, c.phone AS customer_phone,
       i.sales_staff_id, e.full_name AS sales_staff_name,
       i.warehouse_id, w.warehouse_name,
       i.created_at, i.total_amount, i.status, i.note
FROM invoices i
LEFT JOIN customers c ON i.customer_id = c.customer_id
JOIN employees e ON i.sales_staff_id = e.employee_id
JOIN warehouses w ON i.warehouse_id = w.warehouse_id
WHERE i.created_at >= :startDate AND i.created_at <= :endDate
ORDER BY i.invoice_id DESC;

-- Truy vấn danh sách hóa đơn ĐÃ HOÀN THÀNH phục vụ IN BÁO CÁO A4 (khớp doanh thu):
SELECT i.invoice_id, i.customer_id, c.full_name AS customer_name,
       i.sales_staff_id, e.full_name AS sales_staff_name,
       i.created_at, i.total_amount, i.status
FROM invoices i
LEFT JOIN customers c ON i.customer_id = c.customer_id
JOIN employees e ON i.sales_staff_id = e.employee_id
WHERE i.status = 'PAID'
  AND i.created_at >= :startDate 
  AND i.created_at <= :endDate
ORDER BY i.invoice_id DESC;
```

### 12.3. Báo cáo chi tiết tồn kho theo lô (`GET /api/v1/reports/inventory`)
> **Không sử dụng View:** Dùng câu truy vấn SELECT kết hợp JOIN các bảng `inventories`, `warehouses`, `batches`, `products`, `categories` và dùng `CASE WHEN` phân loại hạn sử dụng trực tiếp:
```sql
SELECT i.warehouse_id, w.warehouse_name,
       b.batch_id, b.batch_code, b.mfg_date, b.exp_date,
       (b.exp_date - CURRENT_DATE) AS days_remaining,
       CASE
           WHEN b.exp_date <= CURRENT_DATE THEN 'Hết hạn'
           WHEN b.exp_date <= CURRENT_DATE + INTERVAL '30 days' THEN 'Gần hết hạn'
           ELSE 'Còn hạn'
       END AS expiry_status,
       i.quantity,
       p.product_id, p.product_name, p.unit, p.selling_price,
       (i.quantity * p.selling_price) AS total_value,
       c.category_name
FROM inventories i
JOIN warehouses w ON i.warehouse_id = w.warehouse_id
JOIN batches b ON i.batch_id = b.batch_id
JOIN products p ON b.product_id = p.product_id
LEFT JOIN categories c ON p.category_id = c.category_id
WHERE (:warehouseId IS NULL OR w.warehouse_id = :warehouseId)
  AND (:keyword IS NULL OR :keyword = '' 
       OR LOWER(p.product_name) LIKE LOWER('%' || :keyword || '%') 
       OR LOWER(b.batch_code) LIKE LOWER('%' || :keyword || '%'))
ORDER BY p.product_name ASC, b.exp_date ASC;
```
