-- Bảng nhóm thuốc
create table categories(
	category_id bigserial primary key,
	category_name varchar(100) not null unique,
	description text
);

-- Bảng thuốc
create table products(
	product_id bigserial primary key,
	category_id bigint not null references categories(category_id),
	product_name varchar(200) not null,
	dosage varchar(100),
	uses text,
	dosage_form varchar(100),
	unit varchar(50) not null,
	selling_price numeric(15,2) not null default 0.00 check (selling_price >= 0)
);

-- Bảng lô thuốc
create table batches(
	batch_id bigserial primary key,
	product_id bigint not null references products(product_id),
	batch_code varchar(100) not null,
	mfg_date date not null,
	exp_date date not null,
	constraint chk_batch_dates check (exp_date > mfg_date)
);

-- Bảng kho
create table warehouses(
	warehouse_id bigserial primary key,
	warehouse_name varchar(150) not null,
	warehouse_address varchar(255) not null,
	status varchar(30) not null default 'ACTIVE' check (status in ('ACTIVE', 'INACTIVE'))
);

-- Bảng tồn kho
create table inventories(
	warehouse_id bigint not null references warehouses(warehouse_id),
	batch_id bigint not null references batches(batch_id),
	quantity int not null default 0 check (quantity >= 0),
	updated_at timestamp not null default current_timestamp,
	primary key (warehouse_id, batch_id)
);

-- Bảng nhân viên
create table employees(
	employee_id bigserial primary key,
	username varchar(50) not null unique,
	password varchar(255) not null,
	full_name varchar(100) not null,
	date_of_birth date not null,
	gender varchar(20),
	address text,
	phone varchar(30), 
	start_date date not null default current_date,
	position varchar(100),
	work_status varchar(30) not null default 'ACTIVE' check (work_status in ('ACTIVE', 'INACTIVE'))
);

-- Bảng khách hàng
create table customers(
	customer_id bigserial primary key,
	full_name varchar(100) not null,
	date_of_birth date not null,
	gender varchar(20),
	phone varchar(30),
	address text not null,
	note text
);

-- Bảng nhà cung cấp
create table suppliers(
	supplier_id bigserial primary key,
	supplier_name varchar(150) not null,
	address text,
	tax_code varchar(50),
	representative_name varchar(100),
	phone varchar(30) not null
);

-- Bảng phiếu nhập
create table import_slips(
	import_slip_id bigserial primary key,
	supplier_id bigint not null references suppliers(supplier_id),
	warehouse_staff_id bigint not null references employees(employee_id),
	warehouse_id bigint not null references warehouses(warehouse_id),
	import_date timestamp not null default current_timestamp,
	total_amount numeric(15,2) not null default 0.00 check (total_amount >= 0),
	status varchar(30) not null default 'PENDING' check (status in ('PENDING', 'IMPORTED', 'CANCELLED'))
);

-- Bảng chi tiết phiếu nhập
create table import_slip_items(
	import_slip_id bigint not null references import_slips(import_slip_id) on delete cascade,
	batch_id bigint not null references batches(batch_id),
	quantity int not null check (quantity > 0),
	unit_price numeric(15,2) not null check (unit_price >= 0),
	sub_total numeric(15,2) not null check (sub_total >= 0),
	primary key (import_slip_id, batch_id)
);

-- Bảng hóa đơn
create table invoices(
	invoice_id bigserial primary key,
	customer_id bigint not null references customers(customer_id),
	sales_staff_id bigint not null references employees(employee_id),
	warehouse_id bigint not null references warehouses(warehouse_id),
	created_at timestamp not null default current_timestamp,
	total_amount numeric(15,2) not null default 0.00 check (total_amount >= 0),
	status varchar(30) not null default 'UNPAID' check (status in ('UNPAID', 'PAID', 'CANCELLED')),
	note text
);

-- Bảng chi tiết hóa đơn
create table invoice_items(
	invoice_id bigint not null references invoices(invoice_id) on delete cascade,
	batch_id bigint not null references batches(batch_id),
	quantity int not null check (quantity > 0),
	unit_price numeric(15,2) not null check (unit_price >= 0),
	sub_total numeric(15,2) not null check (sub_total >= 0),
	primary key (invoice_id, batch_id)
);

-- Bảng phiếu hủy
create table disposal_slips(
	disposal_slip_id bigserial primary key,
	warehouse_staff_id bigint not null references employees(employee_id),
	warehouse_id bigint not null references warehouses(warehouse_id),
	disposal_date timestamp,
	status varchar(30) not null default 'PENDING' check (status in ('PENDING', 'DISPOSED', 'CANCELLED'))
);

-- Bảng chi tiết phiếu hủy
create table disposal_slip_items(
	disposal_slip_id bigint not null references disposal_slips(disposal_slip_id) on delete cascade,
	batch_id bigint not null references batches(batch_id),
	quantity int not null check (quantity > 0),
	reason text not null,
	primary key (disposal_slip_id, batch_id)
);
-- AuthController

-- 1.1. Đăng nhập
select employee_id, username, password, full_name, date_of_birth, gender, 
       address, phone, start_date, position, work_status, balance, warehouse_id
from employees
where lower(username) = lower(:username);

-- 1.2. Cấp lại token
select employee_id, username, position, work_status, balance, warehouse_id
from employees
where username = :username;

-- 1.3. Lấy thông tin tài khoản đang đăng nhập
select e.employee_id, e.username, e.full_name, e.position, e.balance,
       e.warehouse_id, w.warehouse_name
from employees e
left join warehouses w on e.warehouse_id = w.warehouse_id
where e.username = :currentUsername;

-- CategoryController

-- 2.1. Lấy toàn bộ danh mục
select category_id, category_name, description 
from categories 
order by category_id asc;

-- 2.2. Thêm mới danh mục
insert into categories (category_name, description) 
values (:categoryName, :description)
returning category_id;

-- 2.3. Cập nhật danh mục
update categories 
set category_name = :categoryName, description = :description 
where category_id = :categoryId;

-- 2.4. Xóa danh mục
delete from categories 
where category_id = :categoryId;

-- WarehouseController

-- 3.1. Lấy tất cả kho hàng
select warehouse_id, warehouse_name, warehouse_address, status 
from warehouses 
order by warehouse_id asc;

-- 3.2. Lấy danh sách kho đang hoạt động
select warehouse_id, warehouse_name, warehouse_address, status 
from warehouses 
where status = 'ACTIVE' 
order by warehouse_id asc;

-- 3.3. Thêm mới kho hàng
insert into warehouses (warehouse_name, warehouse_address, status) 
values (:warehouseName, :warehouseAddress, coalesce(:status, 'ACTIVE'))
returning warehouse_id;

-- 3.4. Cập nhật kho hàng
update warehouses 
set warehouse_name = :warehouseName, 
    warehouse_address = :warehouseAddress, 
    status = :status 
where warehouse_id = :warehouseId;

-- CustomerController

-- 4.1. Tìm kiếm nhanh khách hàng (GET /api/v1/customers/live-search)
select customer_id, full_name, date_of_birth, gender, phone, address, note,
       extract(year from age(current_date, date_of_birth))::int as age
from customers
where :keyword is null or :keyword = ''
   or lower(full_name) like lower('%' || :keyword || '%')
   or phone like '%' || :keyword || '%'
   or cast(date_of_birth as text) like '%' || :keyword || '%'
order by customer_id desc;

-- 4.2. Lấy danh sách toàn bộ khách hàng (GET /api/v1/customers)
select customer_id, full_name, date_of_birth, gender, phone, address, note,
       extract(year from age(current_date, date_of_birth))::int as age
from customers 
order by customer_id asc;

-- 4.3. Xem chi tiết khách hàng theo ID (GET /api/v1/customers/{id})
select customer_id, full_name, date_of_birth, gender, phone, address, note,
       extract(year from age(current_date, date_of_birth))::int as age
from customers 
where customer_id = :customerId;

-- 4.4. Thêm mới khách hàng (POST /api/v1/customers)
insert into customers (full_name, date_of_birth, gender, phone, address, note)
values (:fullName, :dateOfBirth, :gender, :phone, :address, :note)
returning customer_id;

-- 4.5. Cập nhật khách hàng (POST /api/v1/customers với id)
update customers
set full_name = :fullName, date_of_birth = :dateOfBirth, gender = :gender,
    phone = :phone, address = :address, note = :note
where customer_id = :customerId;

-- 4.6. Xóa khách hàng (DELETE /api/v1/customers/{id})
delete from customers 
where customer_id = :customerId;

-- SupplierController

-- 5.1. Tìm kiếm nhanh nhà cung cấp (GET /api/v1/suppliers/live-search)
select supplier_id, supplier_name, address, tax_code, representative_name, phone
from suppliers
where :keyword is null or :keyword = ''
   or lower(supplier_name) like lower('%' || :keyword || '%')
   or phone like '%' || :keyword || '%'
   or tax_code like '%' || :keyword || '%'
   or lower(representative_name) like lower('%' || :keyword || '%')
   or lower(address) like lower('%' || :keyword || '%')
order by supplier_id asc;

-- 5.2. Lấy danh sách toàn bộ nhà cung cấp (GET /api/v1/suppliers)
select supplier_id, supplier_name, address, tax_code, representative_name, phone
from suppliers
order by supplier_id asc;

-- 5.3. Thêm mới nhà cung cấp (POST /api/v1/suppliers)
insert into suppliers (supplier_name, address, tax_code, representative_name, phone)
values (:supplierName, :address, :taxCode, :representativeName, :phone)
returning supplier_id;

-- 5.4. Cập nhật nhà cung cấp (POST /api/v1/suppliers với id)
update suppliers
set supplier_name = :supplierName, address = :address,
    tax_code = :taxCode, representative_name = :representativeName, phone = :phone
where supplier_id = :supplierId;

-- 5.5. Xóa nhà cung cấp (DELETE /api/v1/suppliers/{id})
delete from suppliers 
where supplier_id = :supplierId;

-- EmployeeController

-- 6.1. Lấy danh sách nhân viên (GET /api/v1/employees)
select e.employee_id, e.username, e.full_name, e.date_of_birth, e.gender,
       e.address, e.phone, e.start_date, e.position, e.work_status, e.balance,
       e.warehouse_id, w.warehouse_name,
       extract(year from age(current_date, date_of_birth))::int as age
from employees e
left join warehouses w on e.warehouse_id = w.warehouse_id
order by e.employee_id asc;

-- 6.2. Xem thông tin nhân viên theo ID (GET /api/v1/employees/{id})
SELECT e.employee_id, e.username, e.full_name, e.date_of_birth, e.gender,
       e.address, e.phone, e.start_date, e.position, e.work_status, e.balance,
       e.warehouse_id, w.warehouse_name,
       EXTRACT(YEAR FROM age(CURRENT_DATE, e.date_of_birth))::int AS age
FROM employees e
LEFT JOIN warehouses w ON e.warehouse_id = w.warehouse_id
WHERE e.employee_id = :employeeId;

-- 6.3. Lấy số dư quỹ của tài khoản Quản trị viên (GET /api/v1/employees/admin-balance)
SELECT COALESCE(balance, 0.00) 
FROM employees 
WHERE position = 'ROLE_ADMIN' AND work_status = 'ACTIVE' 
LIMIT 1;

-- 6.4. Thêm mới nhân viên (POST /api/v1/employees)
SELECT 1 FROM employees WHERE username = :username; -- Kiểm tra trùng lặp

INSERT INTO employees (username, password, full_name, date_of_birth, gender, address, phone, start_date, position, work_status, balance, warehouse_id)
VALUES (:username, :password, :fullName, :dateOfBirth, :gender, :address, :phone, CURRENT_DATE, :position, 'ACTIVE', 0.00, :warehouseId)
RETURNING employee_id;

-- 6.5. Cập nhật thông tin nhân viên (POST /api/v1/employees với id)
UPDATE employees
SET full_name = :fullName, date_of_birth = :dateOfBirth, gender = :gender,
    address = :address, phone = :phone, position = :position,
    work_status = :workStatus, warehouse_id = :warehouseId,
    password = COALESCE(:password, password),
    balance = CASE WHEN :position = 'ROLE_ADMIN' AND :balance IS NOT NULL THEN :balance ELSE balance END
WHERE employee_id = :employeeId;

-- 6.6. Bật/tắt trạng thái làm việc (PATCH /api/v1/employees/{id}/toggle-status)
UPDATE employees
SET work_status = CASE WHEN work_status = 'ACTIVE' THEN 'INACTIVE' ELSE 'ACTIVE' END
WHERE employee_id = :employeeId;

-- 6.7. Xem & cập nhật hồ sơ cá nhân (GET & PUT /api/v1/employees/profile)
SELECT e.employee_id, e.username, e.full_name, e.date_of_birth, e.gender,
       e.address, e.phone, e.start_date, e.position, e.work_status, e.balance,
       e.warehouse_id, w.warehouse_name
FROM employees e
LEFT JOIN warehouses w ON e.warehouse_id = w.warehouse_id
WHERE e.username = :currentUsername;

UPDATE employees
SET full_name = COALESCE(:fullName, full_name),
    date_of_birth = COALESCE(:dateOfBirth, date_of_birth),
    gender = COALESCE(:gender, gender),
    phone = COALESCE(:phone, phone),
    address = COALESCE(:address, address),
    password = COALESCE(:newPassword, password)
WHERE username = :currentUsername;

-- ProductController

-- 7.1. Tìm kiếm và phân trang danh sách thuốc kèm tổng tồn kho còn hạn (GET /api/v1/products)
-- Dùng Subquery tính tổng tồn kho các lô còn hạn (exp_date > CURRENT_DATE), không cần View:
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

-- 7.2. Tìm kiếm nhanh thuốc cho Autocomplete / POS (GET /api/v1/products/live-search)
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

-- 7.3. Xem chi tiết thuốc & các lô trong kho (GET /api/v1/products/{id})
SELECT p.product_id, p.product_name, p.dosage, p.uses, p.dosage_form, p.unit, p.selling_price, p.image_url,
       c.category_id, c.category_name
FROM products p
JOIN categories c ON p.category_id = c.category_id
WHERE p.product_id = :productId;

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

-- 7.4. Thêm mới, cập nhật, xóa thuốc (POST / DELETE /api/v1/products)
INSERT INTO products (category_id, product_name, dosage, uses, dosage_form, unit, selling_price, image_url)
VALUES (:categoryId, :productName, :dosage, :uses, :dosageForm, :unit, :sellingPrice, :imageUrl)
RETURNING product_id;

UPDATE products
SET category_id = :categoryId, product_name = :productName, dosage = :dosage,
    uses = :uses, dosage_form = :dosageForm, unit = :unit,
    selling_price = :sellingPrice, image_url = :imageUrl
WHERE product_id = :productId;

DELETE FROM products WHERE product_id = :productId;

-- BatchController

-- 8.1. Lấy danh sách lô thuốc xuất bán theo quy tắc FEFO (GET /api/v1/batches/fefo)
-- Quy tắc: Đúng kho, đúng thuốc, còn hạn (exp_date > CURRENT_DATE), còn tồn kho (> 0), hạn gần nhất xuất trước
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

-- 8.2. Tìm kiếm lô thuốc để đề xuất tiêu hủy (GET /api/v1/batches/search-disposal)
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

-- InvoiceController

-- 9.1. Lập hóa đơn bán hàng mới ở trạng thái UNPAID (POST /api/v1/invoices)
-- Bước 1: Kiểm tra nhân viên bán hàng hợp lệ (ACTIVE, vai trò SALES hoặc ADMIN)
SELECT employee_id, full_name, work_status, position 
FROM employees 
WHERE username = :username 
  AND work_status = 'ACTIVE' 
  AND position IN ('ROLE_SALES', 'ROLE_ADMIN');

-- Bước 2: Kiểm tra kho hoạt động & khách hàng
SELECT warehouse_id FROM warehouses WHERE warehouse_id = :warehouseId AND status = 'ACTIVE';
SELECT customer_id FROM customers WHERE customer_id = :customerId;

-- Bước 3: Tạo hóa đơn mới (tổng tiền khởi tạo 0.00, trạng thái UNPAID)
INSERT INTO invoices (customer_id, sales_staff_id, warehouse_id, created_at, total_amount, status, note)
VALUES (:customerId, :salesStaffId, :warehouseId, CURRENT_TIMESTAMP, 0.00, 'UNPAID', :note)
RETURNING invoice_id;

-- Bước 4: Lấy giá bán niêm yết của từng lô thuốc
SELECT p.selling_price 
FROM batches b 
JOIN products p ON b.product_id = p.product_id 
WHERE b.batch_id = :batchId;

-- Bước 5: Chèn từng dòng mặt hàng vào hóa đơn (invoice_items)
INSERT INTO invoice_items (invoice_id, batch_id, quantity, unit_price, sub_total)
VALUES (:invoiceId, :batchId, :quantity, :unitPrice, :quantity * :unitPrice);

-- Bước 6: Cập nhật tổng tiền hóa đơn tự động
UPDATE invoices
SET total_amount = (
    SELECT COALESCE(SUM(sub_total), 0.00) 
    FROM invoice_items 
    WHERE invoice_id = :invoiceId
)
WHERE invoice_id = :invoiceId;

-- 9.2. Xác nhận thanh toán hóa đơn & Trừ tồn kho (POST /api/v1/invoices/{id}/confirm-payment)
-- Bước 1: Khóa dòng hóa đơn (kiểm tra trạng thái UNPAID)
SELECT invoice_id, warehouse_id, status, total_amount
FROM invoices
WHERE invoice_id = :invoiceId
FOR UPDATE;

-- Bước 2: Kiểm tra hạn dùng và số lượng tồn kho từng lô trong hóa đơn
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

-- Bước 4: Chuyển trạng thái hóa đơn sang PAID
UPDATE invoices
SET status = 'PAID'
WHERE invoice_id = :invoiceId;

-- 9.3. Hủy hóa đơn chưa thanh toán (POST /api/v1/invoices/{id}/cancel)
UPDATE invoices
SET status = 'CANCELLED'
WHERE invoice_id = :invoiceId AND status = 'UNPAID';

-- 9.4. Xem chi tiết hóa đơn (GET /api/v1/invoices/{id})
SELECT i.invoice_id, i.customer_id, c.full_name AS customer_name, c.phone AS customer_phone,
       i.sales_staff_id, e.full_name AS sales_staff_name,
       i.warehouse_id, w.warehouse_name,
       i.created_at, i.total_amount, i.status, i.note
FROM invoices i
LEFT JOIN customers c ON i.customer_id = c.customer_id
JOIN employees e ON i.sales_staff_id = e.employee_id
JOIN warehouses w ON i.warehouse_id = w.warehouse_id
WHERE i.invoice_id = :invoiceId;

SELECT ii.batch_id, b.batch_code, p.product_id, p.product_name, p.unit,
       ii.quantity, ii.unit_price, ii.sub_total
FROM invoice_items ii
JOIN batches b ON ii.batch_id = b.batch_id
JOIN products p ON b.product_id = p.product_id
WHERE ii.invoice_id = :invoiceId;

-- 9.5. Tìm kiếm danh sách hóa đơn (GET /api/v1/invoices)
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

-- ImportSlipController

-- 10.1. Tạo phiếu nhập kho chứa danh sách nhiều lô thuốc (POST /api/v1/import-slips)
-- Bước 1: Kiểm tra nhân viên kho (ACTIVE, ROLE_WAREHOUSE hoặc ROLE_ADMIN)
SELECT employee_id FROM employees 
WHERE username = :username 
  AND work_status = 'ACTIVE' 
  AND position IN ('ROLE_WAREHOUSE', 'ROLE_ADMIN');

-- Bước 2: Kiểm tra kho hoạt động & nhà cung cấp tồn tại
SELECT warehouse_id FROM warehouses WHERE warehouse_id = :warehouseId AND status = 'ACTIVE';
SELECT supplier_id FROM suppliers WHERE supplier_id = :supplierId;

-- Bước 3: Lặp qua từng lô hàng cần nhập trong danh sách items:
-- Kiểm tra số lô đã có trong hệ thống chưa:
SELECT batch_id FROM batches WHERE batch_code = :batchCode;
-- Nếu chưa có số lô này, tự động thêm mới vào bảng batches:
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

-- 10.2. Duyệt phiếu nhập kho & Tăng tồn kho (POST /api/v1/import-slips/{id}/confirm)
-- Bước 1: Khóa dòng phiếu nhập để kiểm tra trạng thái PENDING
SELECT import_slip_id, warehouse_id, status, total_amount
FROM import_slips
WHERE import_slip_id = :importSlipId
FOR UPDATE;

-- Bước 2: Cộng số lượng tồn kho cho các lô (sử dụng UPSERT thuần túy)
INSERT INTO inventories (warehouse_id, batch_id, quantity, updated_at)
SELECT :warehouseId, batch_id, quantity, CURRENT_TIMESTAMP
FROM import_slip_items
WHERE import_slip_id = :importSlipId
ON CONFLICT (warehouse_id, batch_id)
DO UPDATE SET
    quantity = inventories.quantity + EXCLUDED.quantity,
    updated_at = CURRENT_TIMESTAMP;

-- Bước 3: Chuyển trạng thái phiếu nhập sang IMPORTED (Đã nhập kho)
UPDATE import_slips
SET status = 'IMPORTED'
WHERE import_slip_id = :importSlipId;

-- 10.3. Hủy phiếu nhập kho (POST /api/v1/import-slips/{id}/cancel)
UPDATE import_slips
SET status = 'CANCELLED'
WHERE import_slip_id = :importSlipId AND status = 'PENDING';

-- 10.4. Lấy danh sách phiếu nhập kho (GET /api/v1/import-slips)
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

-- 10.5. Lấy danh sách chi tiết các lô hàng trong 1 phiếu nhập
SELECT isi.batch_id, b.batch_code, b.mfg_date, b.exp_date,
       p.product_id, p.product_name, p.unit,
       isi.quantity, isi.unit_price, isi.sub_total
FROM import_slip_items isi
JOIN batches b ON isi.batch_id = b.batch_id
JOIN products p ON b.product_id = p.product_id
WHERE isi.import_slip_id = :importSlipId;

-- DisposalSlipController

-- 11.1. Lập và xác nhận phiếu hủy nhiều lô hàng (POST /api/v1/disposal-slips)
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

-- 11.2. Lấy danh sách phiếu tiêu hủy (GET /api/v1/disposal-slips)
SELECT ds.disposal_slip_id, ds.warehouse_staff_id, e.full_name AS warehouse_staff_name,
       ds.warehouse_id, w.warehouse_name, ds.disposal_date, ds.status
FROM disposal_slips ds
JOIN employees e ON ds.warehouse_staff_id = e.employee_id
JOIN warehouses w ON ds.warehouse_id = w.warehouse_id
ORDER BY ds.disposal_slip_id DESC;

-- 11.3. Lấy danh sách chi tiết các lô thuốc đã tiêu hủy trong phiếu
SELECT dsi.batch_id, b.batch_code, p.product_id, p.product_name, p.unit,
       dsi.quantity, dsi.reason
FROM disposal_slip_items dsi
JOIN batches b ON dsi.batch_id = b.batch_id
JOIN products p ON b.product_id = p.product_id
WHERE dsi.disposal_slip_id = :disposalSlipId;

-- ReportController

-- 12.1. Báo cáo các chỉ số tài chính KPI (GET /api/v1/reports/kpis)
-- Chỉ số 1: Tổng tiền nhập hàng đã duyệt (giá vốn hàng nhập) trong kỳ
SELECT COALESCE(SUM(total_amount), 0.00)
FROM import_slips
WHERE status = 'IMPORTED' 
  AND import_date >= :startDate 
  AND import_date <= :endDate;

-- Chỉ số 2: Tổng doanh thu thuần từ các hóa đơn đã thanh toán (PAID) trong kỳ
SELECT COALESCE(SUM(total_amount), 0.00)
FROM invoices
WHERE status = 'PAID' 
  AND created_at >= :startDate 
  AND created_at <= :endDate;

-- Chỉ số 3: Số dư tài khoản Quản trị viên
SELECT COALESCE(balance, 0.00) 
FROM employees 
WHERE position = 'ROLE_ADMIN' AND work_status = 'ACTIVE' 
LIMIT 1;

-- Chỉ số 4: Thống kê số lượng chứng từ phát sinh trong kỳ
SELECT COUNT(*) FROM invoices WHERE created_at >= :startDate AND created_at <= :endDate;
SELECT COUNT(*) FROM import_slips WHERE import_date >= :startDate AND import_date <= :endDate;
SELECT COUNT(*) FROM disposal_slips;

-- 12.2. Lấy danh sách hóa đơn theo kỳ báo cáo (GET /api/v1/reports/invoices)
-- Truy vấn tất cả hóa đơn hiển thị trên màn hình đối soát:
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

-- 12.3. Báo cáo chi tiết tồn kho theo lô (GET /api/v1/reports/inventory)
-- Câu truy vấn thuần túy, tính số ngày còn hạn và trạng thái hạn dùng bằng CASE WHEN, không dùng VIEW:
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