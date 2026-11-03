-- ========================================================
-- PHARMACY MANAGEMENT SYSTEM - SCHEMA SCRIPT (14 TABLES 3NF)
-- FULLY SYNCHRONIZED ENTITIES, COLUMNS, TRIGGERS & PROCEDURES
-- ========================================================

-- Drop triggers if exist
DROP TRIGGER IF EXISTS trg_check_import_slip_init ON import_slips;
DROP TRIGGER IF EXISTS trg_check_invoice_init ON invoices;
DROP TRIGGER IF EXISTS trg_check_disposal_slip_init ON disposal_slips;
DROP TRIGGER IF EXISTS trg_lock_import_slips ON import_slips;
DROP TRIGGER IF EXISTS trg_lock_invoices ON invoices;
DROP TRIGGER IF EXISTS trg_lock_disposal_slips ON disposal_slips;
DROP TRIGGER IF EXISTS trg_calc_import_item ON import_slip_items;
DROP TRIGGER IF EXISTS trg_update_import_total ON import_slip_items;
DROP TRIGGER IF EXISTS trg_calc_invoice_item ON invoice_items;
DROP TRIGGER IF EXISTS trg_update_invoice_total ON invoice_items;
DROP TRIGGER IF EXISTS trg_check_disposal_item ON disposal_slip_items;
DROP TRIGGER IF EXISTS trg_check_warehouse_deactivation ON warehouses;

-- Drop views if exist
DROP VIEW IF EXISTS vw_inventory_batches;
DROP VIEW IF EXISTS vw_employees_with_age;
DROP VIEW IF EXISTS vw_customers_with_age;

-- Drop tables with CASCADE
DROP TABLE IF EXISTS disposal_slip_items CASCADE;
DROP TABLE IF EXISTS disposal_slips CASCADE;
DROP TABLE IF EXISTS invoice_items CASCADE;
DROP TABLE IF EXISTS invoices CASCADE;
DROP TABLE IF EXISTS import_slip_items CASCADE;
DROP TABLE IF EXISTS import_slips CASCADE;
DROP TABLE IF EXISTS inventories CASCADE;
DROP TABLE IF EXISTS batches CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS employees CASCADE;
DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS suppliers CASCADE;
DROP TABLE IF EXISTS warehouses CASCADE;

-- 1. categories
CREATE TABLE categories (
    category_id BIGSERIAL PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

-- 2. warehouses
CREATE TABLE warehouses (
    warehouse_id BIGSERIAL PRIMARY KEY,
    warehouse_name VARCHAR(150) NOT NULL,
    warehouse_address VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- 3. products
CREATE TABLE products (
    product_id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES categories(category_id),
    product_name VARCHAR(200) NOT NULL,
    dosage VARCHAR(100),
    uses TEXT,
    dosage_form VARCHAR(100),
    unit VARCHAR(50) NOT NULL,
    selling_price NUMERIC(15,2) NOT NULL DEFAULT 0.00 CHECK (selling_price >= 0),
    image_url TEXT
);

-- 4. batches
CREATE TABLE batches (
    batch_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(product_id),
    batch_code VARCHAR(100) NOT NULL,
    mfg_date DATE NOT NULL,
    exp_date DATE NOT NULL,
    CONSTRAINT chk_batch_dates CHECK (exp_date > mfg_date)
);

-- 5. inventories
CREATE TABLE inventories (
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(warehouse_id),
    batch_id BIGINT NOT NULL REFERENCES batches(batch_id),
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (warehouse_id, batch_id)
);

-- 6. employees
CREATE TABLE employees (
    employee_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20),
    address TEXT,
    phone VARCHAR(30),
    start_date DATE NOT NULL DEFAULT CURRENT_DATE,
    position VARCHAR(100) NOT NULL CHECK (position IN ('ROLE_ADMIN', 'ROLE_SALES', 'ROLE_WAREHOUSE')),
    work_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' CHECK (work_status IN ('ACTIVE', 'INACTIVE')),
    balance NUMERIC(15,2) DEFAULT 0.00 CHECK (balance >= 0),
    warehouse_id BIGINT REFERENCES warehouses(warehouse_id)
);

-- 7. customers
CREATE TABLE customers (
    customer_id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20),
    phone VARCHAR(30),
    address TEXT NOT NULL,
    note TEXT
);

-- 8. suppliers
CREATE TABLE suppliers (
    supplier_id BIGSERIAL PRIMARY KEY,
    supplier_name VARCHAR(150) NOT NULL,
    address TEXT,
    tax_code VARCHAR(50),
    representative_name VARCHAR(100),
    phone VARCHAR(30) NOT NULL
);

-- 9. import_slips
CREATE TABLE import_slips (
    import_slip_id BIGSERIAL PRIMARY KEY,
    supplier_id BIGINT NOT NULL REFERENCES suppliers(supplier_id),
    warehouse_staff_id BIGINT NOT NULL REFERENCES employees(employee_id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(warehouse_id),
    import_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00 CHECK (total_amount >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'IMPORTED', 'CANCELLED'))
);

-- 10. import_slip_items
CREATE TABLE import_slip_items (
    import_slip_id BIGINT NOT NULL REFERENCES import_slips(import_slip_id) ON DELETE CASCADE,
    batch_id BIGINT NOT NULL REFERENCES batches(batch_id),
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(15,2) NOT NULL CHECK (unit_price >= 0),
    sub_total NUMERIC(15,2) NOT NULL CHECK (sub_total >= 0),
    PRIMARY KEY (import_slip_id, batch_id)
);

-- 11. invoices
CREATE TABLE invoices (
    invoice_id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(customer_id),
    sales_staff_id BIGINT NOT NULL REFERENCES employees(employee_id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(warehouse_id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00 CHECK (total_amount >= 0),
    status VARCHAR(30) NOT NULL DEFAULT 'UNPAID' CHECK (status IN ('UNPAID', 'PAID', 'CANCELLED')),
    note TEXT
);

-- 12. invoice_items
CREATE TABLE invoice_items (
    invoice_id BIGINT NOT NULL REFERENCES invoices(invoice_id) ON DELETE CASCADE,
    batch_id BIGINT NOT NULL REFERENCES batches(batch_id),
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(15,2) NOT NULL CHECK (unit_price >= 0),
    sub_total NUMERIC(15,2) NOT NULL CHECK (sub_total >= 0),
    PRIMARY KEY (invoice_id, batch_id)
);

-- 13. disposal_slips
CREATE TABLE disposal_slips (
    disposal_slip_id BIGSERIAL PRIMARY KEY,
    warehouse_staff_id BIGINT NOT NULL REFERENCES employees(employee_id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(warehouse_id),
    disposal_date TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'DISPOSED', 'CANCELLED'))
);

-- 14. disposal_slip_items
CREATE TABLE disposal_slip_items (
    disposal_slip_id BIGINT NOT NULL REFERENCES disposal_slips(disposal_slip_id) ON DELETE CASCADE,
    batch_id BIGINT NOT NULL REFERENCES batches(batch_id),
    quantity INT NOT NULL CHECK (quantity > 0),
    reason TEXT NOT NULL,
    PRIMARY KEY (disposal_slip_id, batch_id)
);

-- ========================================================
-- VIEWS
-- ========================================================

CREATE OR REPLACE VIEW vw_employees_with_age AS
SELECT
    e.*,
    EXTRACT(YEAR FROM age(CURRENT_DATE, e.date_of_birth))::int AS age
FROM employees e;

CREATE OR REPLACE VIEW vw_customers_with_age AS
SELECT
    c.*,
    EXTRACT(YEAR FROM age(CURRENT_DATE, c.date_of_birth))::int AS age
FROM customers c;

CREATE OR REPLACE VIEW vw_inventory_batches AS
SELECT
    i.warehouse_id,
    w.warehouse_name,
    b.batch_id,
    b.batch_code,
    b.mfg_date,
    b.exp_date,
    (b.exp_date - CURRENT_DATE) AS days_remaining,
    CASE 
        WHEN b.exp_date <= CURRENT_DATE THEN 'EXPIRED'
        WHEN b.exp_date <= CURRENT_DATE + INTERVAL '30 days' THEN 'NEAR_EXPIRY'
        ELSE 'VALID'
    END AS expiry_status,
    i.quantity,
    p.product_id,
    p.product_name,
    p.unit,
    p.selling_price,
    c.category_name
FROM inventories i
JOIN warehouses w ON w.warehouse_id = i.warehouse_id
JOIN batches b ON b.batch_id = i.batch_id
JOIN products p ON p.product_id = b.product_id
JOIN categories c ON c.category_id = p.category_id;

-- ========================================================
-- TRIGGERS & BUSINESS FUNCTIONS
-- ========================================================

-- 1. Check active staff & warehouse on slip creation
CREATE OR REPLACE FUNCTION fn_check_invoice_init()
RETURNS TRIGGER AS $$
DECLARE
    v_staff_status VARCHAR(30);
    v_warehouse_status VARCHAR(30);
BEGIN
    SELECT work_status INTO v_staff_status FROM employees WHERE employee_id = NEW.sales_staff_id;
    IF v_staff_status IS DISTINCT FROM 'ACTIVE' THEN
        RAISE EXCEPTION 'Nhan vien da ngung lam viec, khong duoc lap hoa don.';
    END IF;

    SELECT status INTO v_warehouse_status FROM warehouses WHERE warehouse_id = NEW.warehouse_id;
    IF v_warehouse_status IS DISTINCT FROM 'ACTIVE' THEN
        RAISE EXCEPTION 'Kho da ngung hoat dong, khong duoc chon cho hoa don.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_invoice_init
BEFORE INSERT ON invoices
FOR EACH ROW EXECUTE FUNCTION fn_check_invoice_init();

CREATE OR REPLACE FUNCTION fn_check_slip_init()
RETURNS TRIGGER AS $$
DECLARE
    v_staff_status VARCHAR(30);
    v_warehouse_status VARCHAR(30);
BEGIN
    SELECT work_status INTO v_staff_status FROM employees WHERE employee_id = NEW.warehouse_staff_id;
    IF v_staff_status IS DISTINCT FROM 'ACTIVE' THEN
        RAISE EXCEPTION 'Nhan vien da ngung lam viec, khong duoc lap chung tu moi.';
    END IF;

    SELECT status INTO v_warehouse_status FROM warehouses WHERE warehouse_id = NEW.warehouse_id;
    IF v_warehouse_status IS DISTINCT FROM 'ACTIVE' THEN
        RAISE EXCEPTION 'Kho da ngung hoat dong, khong duoc chon cho chung tu moi.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_import_slip_init
BEFORE INSERT ON import_slips
FOR EACH ROW EXECUTE FUNCTION fn_check_slip_init();

CREATE TRIGGER trg_check_disposal_slip_init
BEFORE INSERT ON disposal_slips
FOR EACH ROW EXECUTE FUNCTION fn_check_slip_init();

-- 2. Prevent modify completed or cancelled slips
CREATE OR REPLACE FUNCTION fn_prevent_modify_completed_slips()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.status NOT IN ('PENDING', 'UNPAID') THEN
            RAISE EXCEPTION 'Chứng từ đã hoàn tất hoặc đã hủy, không được xóa.';
        END IF;
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        IF OLD.status IN ('IMPORTED', 'PAID', 'DISPOSED', 'CANCELLED') THEN
            RAISE EXCEPTION 'Chứng từ đã hoàn tất hoặc đã hủy, không được phép chỉnh sửa.';
        END IF;
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_lock_import_slips
BEFORE UPDATE OR DELETE ON import_slips
FOR EACH ROW EXECUTE FUNCTION fn_prevent_modify_completed_slips();

CREATE TRIGGER trg_lock_invoices
BEFORE UPDATE OR DELETE ON invoices
FOR EACH ROW EXECUTE FUNCTION fn_prevent_modify_completed_slips();

CREATE TRIGGER trg_lock_disposal_slips
BEFORE UPDATE OR DELETE ON disposal_slips
FOR EACH ROW EXECUTE FUNCTION fn_prevent_modify_completed_slips();

-- 3. Calculate import slip items & total
CREATE OR REPLACE FUNCTION fn_calc_import_item()
RETURNS TRIGGER AS $$
DECLARE
    v_status VARCHAR(30);
BEGIN
    SELECT status INTO v_status FROM import_slips WHERE import_slip_id = COALESCE(NEW.import_slip_id, OLD.import_slip_id);
    IF v_status <> 'PENDING' THEN
        RAISE EXCEPTION 'Chỉ được thêm/sửa chi tiết khi phiếu nhập ở trạng thái PENDING.';
    END IF;

    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        NEW.sub_total := NEW.quantity * NEW.unit_price;
        RETURN NEW;
    END IF;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_calc_import_item
BEFORE INSERT OR UPDATE OR DELETE ON import_slip_items
FOR EACH ROW EXECUTE FUNCTION fn_calc_import_item();

CREATE OR REPLACE FUNCTION fn_update_import_total()
RETURNS TRIGGER AS $$
DECLARE
    v_slip_id BIGINT := COALESCE(NEW.import_slip_id, OLD.import_slip_id);
BEGIN
    UPDATE import_slips
    SET total_amount = COALESCE((SELECT SUM(sub_total) FROM import_slip_items WHERE import_slip_id = v_slip_id), 0.00)
    WHERE import_slip_id = v_slip_id;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_import_total
AFTER INSERT OR UPDATE OR DELETE ON import_slip_items
FOR EACH ROW EXECUTE FUNCTION fn_update_import_total();

-- 4. Calculate invoice item & total
CREATE OR REPLACE FUNCTION fn_calc_invoice_item()
RETURNS TRIGGER AS $$
DECLARE
    v_status VARCHAR(30);
    v_warehouse_id BIGINT;
    v_exp_date DATE;
    v_stock INT;
    v_selling_price NUMERIC(15,2);
BEGIN
    SELECT status, warehouse_id INTO v_status, v_warehouse_id
    FROM invoices
    WHERE invoice_id = COALESCE(NEW.invoice_id, OLD.invoice_id);

    IF v_status <> 'UNPAID' THEN
        RAISE EXCEPTION 'Chỉ được thêm/sửa chi tiết khi hóa đơn ở trạng thái UNPAID.';
    END IF;

    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        SELECT b.exp_date, p.selling_price, COALESCE(i.quantity, 0)
        INTO v_exp_date, v_selling_price, v_stock
        FROM batches b
        JOIN products p ON p.product_id = b.product_id
        LEFT JOIN inventories i ON i.batch_id = b.batch_id AND i.warehouse_id = v_warehouse_id
        WHERE b.batch_id = NEW.batch_id;

        IF v_exp_date <= CURRENT_DATE THEN
            RAISE EXCEPTION 'Lô thuốc % đã hết hạn sử dụng (%)', NEW.batch_id, v_exp_date;
        END IF;

        IF v_stock < NEW.quantity THEN
            RAISE EXCEPTION 'Số lượng tồn kho không đủ cho lô % (Tồn: %, Cần bán: %)', NEW.batch_id, v_stock, NEW.quantity;
        END IF;

        IF TG_OP = 'INSERT' AND (NEW.unit_price IS NULL OR NEW.unit_price = 0) THEN
            NEW.unit_price := v_selling_price;
        END IF;

        NEW.sub_total := NEW.quantity * NEW.unit_price;
        RETURN NEW;
    END IF;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_calc_invoice_item
BEFORE INSERT OR UPDATE OR DELETE ON invoice_items
FOR EACH ROW EXECUTE FUNCTION fn_calc_invoice_item();

CREATE OR REPLACE FUNCTION fn_update_invoice_total()
RETURNS TRIGGER AS $$
DECLARE
    v_inv_id BIGINT := COALESCE(NEW.invoice_id, OLD.invoice_id);
BEGIN
    UPDATE invoices
    SET total_amount = COALESCE((SELECT SUM(sub_total) FROM invoice_items WHERE invoice_id = v_inv_id), 0.00)
    WHERE invoice_id = v_inv_id;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_invoice_total
AFTER INSERT OR UPDATE OR DELETE ON invoice_items
FOR EACH ROW EXECUTE FUNCTION fn_update_invoice_total();

-- 5. Check disposal item
CREATE OR REPLACE FUNCTION fn_check_disposal_item()
RETURNS TRIGGER AS $$
DECLARE
    v_status VARCHAR(30);
    v_warehouse_id BIGINT;
    v_stock INT;
BEGIN
    SELECT status, warehouse_id INTO v_status, v_warehouse_id
    FROM disposal_slips
    WHERE disposal_slip_id = COALESCE(NEW.disposal_slip_id, OLD.disposal_slip_id);

    IF v_status <> 'PENDING' THEN
        RAISE EXCEPTION 'Chỉ được thêm/sửa chi tiết khi phiếu hủy ở trạng thái PENDING.';
    END IF;

    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        SELECT COALESCE(quantity, 0) INTO v_stock
        FROM inventories
        WHERE warehouse_id = v_warehouse_id AND batch_id = NEW.batch_id;

        IF v_stock < NEW.quantity THEN
            RAISE EXCEPTION 'Số lượng hủy vượt quá tồn kho của lô % (Tồn: %, Hủy: %).', NEW.batch_id, v_stock, NEW.quantity;
        END IF;
        RETURN NEW;
    END IF;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_disposal_item
BEFORE INSERT OR UPDATE OR DELETE ON disposal_slip_items
FOR EACH ROW EXECUTE FUNCTION fn_check_disposal_item();

-- 6. Warehouse deactivation guard
CREATE OR REPLACE FUNCTION fn_check_warehouse_deactivation()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status = 'ACTIVE' AND NEW.status = 'INACTIVE' THEN
        IF EXISTS (SELECT 1 FROM inventories WHERE warehouse_id = NEW.warehouse_id AND quantity > 0) THEN
            RAISE EXCEPTION 'Không thể ngừng hoạt động kho khi vẫn còn hàng tồn.';
        END IF;

        IF EXISTS (SELECT 1 FROM import_slips WHERE warehouse_id = NEW.warehouse_id AND status = 'PENDING')
           OR EXISTS (SELECT 1 FROM invoices WHERE warehouse_id = NEW.warehouse_id AND status = 'UNPAID')
           OR EXISTS (SELECT 1 FROM disposal_slips WHERE warehouse_id = NEW.warehouse_id AND status = 'PENDING') THEN
            RAISE EXCEPTION 'Không thể ngừng hoạt động kho khi vẫn còn chứng từ đang chờ xử lý.';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_warehouse_deactivation
BEFORE UPDATE OF status ON warehouses
FOR EACH ROW EXECUTE FUNCTION fn_check_warehouse_deactivation();

-- ========================================================
-- STORED PROCEDURES
-- ========================================================

-- sp_confirm_invoice_payment: Lock FOR UPDATE, check expiry & stock, deduct inventories, credit Admin balance, status = 'PAID'
CREATE OR REPLACE PROCEDURE sp_confirm_invoice_payment(p_invoice_id BIGINT)
LANGUAGE plpgsql AS $$
DECLARE
    v_warehouse_id BIGINT;
    v_status VARCHAR(30);
    v_item_count INT;
    v_total NUMERIC(15,2);
    v_cur_stock INT;
    r RECORD;
BEGIN
    SELECT warehouse_id, status, total_amount INTO v_warehouse_id, v_status, v_total
    FROM invoices
    WHERE invoice_id = p_invoice_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Không tìm thấy hóa đơn %.', p_invoice_id;
    END IF;

    IF v_status <> 'UNPAID' THEN
        RAISE EXCEPTION 'Hóa đơn không ở trạng thái chưa thanh toán (UNPAID).';
    END IF;

    SELECT COUNT(*) INTO v_item_count FROM invoice_items WHERE invoice_id = p_invoice_id;
    IF v_item_count = 0 THEN
        RAISE EXCEPTION 'Hóa đơn phải có ít nhất một dòng chi tiết.';
    END IF;

    -- Kiểm tra lại từng dòng và trừ tồn kho
    FOR r IN
        SELECT ii.batch_id, ii.quantity AS req_qty, b.exp_date
        FROM invoice_items ii
        JOIN batches b ON b.batch_id = ii.batch_id
        WHERE ii.invoice_id = p_invoice_id
    LOOP
        IF r.exp_date <= CURRENT_DATE THEN
            RAISE EXCEPTION 'Từ chối thanh toán: Lô thuốc % đã hết hạn sử dụng (%).', r.batch_id, r.exp_date;
        END IF;

        SELECT quantity INTO v_cur_stock
        FROM inventories
        WHERE warehouse_id = v_warehouse_id AND batch_id = r.batch_id
        FOR UPDATE;

        IF NOT FOUND OR v_cur_stock < r.req_qty THEN
            RAISE EXCEPTION 'Từ chối thanh toán: Lô thuốc % không đủ tồn kho (Tồn: %, Cần: %).', r.batch_id, COALESCE(v_cur_stock, 0), r.req_qty;
        END IF;

        UPDATE inventories
        SET quantity = quantity - r.req_qty,
            updated_at = CURRENT_TIMESTAMP
        WHERE warehouse_id = v_warehouse_id AND batch_id = r.batch_id;
    END LOOP;

    -- Cộng tiền vào tài khoản Chủ cửa hàng (Admin)
    UPDATE employees
    SET balance = balance + v_total
    WHERE position = 'ROLE_ADMIN' AND work_status = 'ACTIVE';

    -- Đổi trạng thái hóa đơn sang PAID
    UPDATE invoices
    SET status = 'PAID'
    WHERE invoice_id = p_invoice_id;
END;
$$;

-- sp_confirm_import_slip: Deduct Admin balance, add inventory, status = 'IMPORTED'
CREATE OR REPLACE PROCEDURE sp_confirm_import_slip(p_import_slip_id BIGINT)
LANGUAGE plpgsql AS $$
DECLARE
    v_warehouse_id BIGINT;
    v_status VARCHAR(30);
    v_total NUMERIC(15,2);
    v_item_count INT;
    v_admin_balance NUMERIC(15,2);
    v_admin_id BIGINT;
BEGIN
    SELECT warehouse_id, status, total_amount INTO v_warehouse_id, v_status, v_total
    FROM import_slips
    WHERE import_slip_id = p_import_slip_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Không tìm thấy phiếu nhập %.', p_import_slip_id;
    END IF;

    IF v_status <> 'PENDING' THEN
        RAISE EXCEPTION 'Phiếu nhập không ở trạng thái chờ duyệt (PENDING).';
    END IF;

    SELECT COUNT(*) INTO v_item_count FROM import_slip_items WHERE import_slip_id = p_import_slip_id;
    IF v_item_count = 0 THEN
        RAISE EXCEPTION 'Phiếu nhập phải có ít nhất một dòng chi tiết hợp lệ.';
    END IF;

    -- Khóa và kiểm tra số dư Admin
    SELECT employee_id, balance INTO v_admin_id, v_admin_balance
    FROM employees
    WHERE position = 'ROLE_ADMIN' AND work_status = 'ACTIVE'
    ORDER BY employee_id ASC
    LIMIT 1
    FOR UPDATE;

    IF v_admin_balance IS NULL OR v_admin_balance < v_total THEN
        RAISE EXCEPTION 'Số dư chủ cửa hàng không đủ';
    END IF;

    -- Trừ số dư Admin
    UPDATE employees
    SET balance = balance - v_total
    WHERE employee_id = v_admin_id;

    -- Cộng tồn kho
    INSERT INTO inventories(warehouse_id, batch_id, quantity, updated_at)
    SELECT v_warehouse_id, batch_id, quantity, CURRENT_TIMESTAMP
    FROM import_slip_items
    WHERE import_slip_id = p_import_slip_id
    ON CONFLICT (warehouse_id, batch_id)
    DO UPDATE SET
        quantity = inventories.quantity + EXCLUDED.quantity,
        updated_at = CURRENT_TIMESTAMP;

    -- Chuyển trạng thái sang IMPORTED
    UPDATE import_slips
    SET status = 'IMPORTED'
    WHERE import_slip_id = p_import_slip_id;
END;
$$;

-- sp_confirm_disposal_slip: Deduct inventory, status = 'DISPOSED', disposal_date = CURRENT_TIMESTAMP
CREATE OR REPLACE PROCEDURE sp_confirm_disposal_slip(p_disposal_slip_id BIGINT)
LANGUAGE plpgsql AS $$
DECLARE
    v_warehouse_id BIGINT;
    v_status VARCHAR(30);
    v_item_count INT;
    v_cur_stock INT;
    r RECORD;
BEGIN
    SELECT warehouse_id, status INTO v_warehouse_id, v_status
    FROM disposal_slips
    WHERE disposal_slip_id = p_disposal_slip_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Không tìm thấy phiếu hủy %.', p_disposal_slip_id;
    END IF;

    IF v_status <> 'PENDING' THEN
        RAISE EXCEPTION 'Phiếu hủy không ở trạng thái chờ hủy (PENDING).';
    END IF;

    SELECT COUNT(*) INTO v_item_count FROM disposal_slip_items WHERE disposal_slip_id = p_disposal_slip_id;
    IF v_item_count = 0 THEN
        RAISE EXCEPTION 'Phiếu hủy phải có ít nhất một dòng chi tiết.';
    END IF;

    -- Kiểm tra lại tồn kho của tất cả các dòng trước khi trừ
    FOR r IN
        SELECT dsi.batch_id, dsi.quantity AS req_qty
        FROM disposal_slip_items dsi
        WHERE dsi.disposal_slip_id = p_disposal_slip_id
    LOOP
        SELECT quantity INTO v_cur_stock
        FROM inventories
        WHERE warehouse_id = v_warehouse_id AND batch_id = r.batch_id
        FOR UPDATE;

        IF NOT FOUND OR v_cur_stock < r.req_qty THEN
            RAISE EXCEPTION 'Từ chối xác nhận hủy: Lô % không đủ tồn kho (Tồn: %, Cần hủy: %).', r.batch_id, COALESCE(v_cur_stock, 0), r.req_qty;
        END IF;

        UPDATE inventories
        SET quantity = quantity - r.req_qty,
            updated_at = CURRENT_TIMESTAMP
        WHERE warehouse_id = v_warehouse_id AND batch_id = r.batch_id;
    END LOOP;

    UPDATE disposal_slips
    SET status = 'DISPOSED',
        disposal_date = CURRENT_TIMESTAMP
    WHERE disposal_slip_id = p_disposal_slip_id;
END;
$$;
