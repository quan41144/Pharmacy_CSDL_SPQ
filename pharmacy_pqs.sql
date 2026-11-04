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
-- Quản lý nhân viên, tài khoản, khách hàng, nhà cung cấp
-- 1. Truy vấn thông tin nhân viên kèm tính tuổi tròn
SELECT employee_id, full_name, date_of_birth,
       DATE_PART('year', AGE(CURRENT_DATE, date_of_birth)) AS age,
       position, work_status
FROM employees
WHERE username = :username;

-- 2. Trigger chặn nhân viên ngừng làm việc lập chứng từ (hóa đơn, phiếu nhập, phiếu hủy)
CREATE OR REPLACE FUNCTION fn_check_invoice_init()
RETURNS TRIGGER AS $$
DECLARE
    v_staff_status VARCHAR(30);
    v_staff_pos VARCHAR(100);
    v_warehouse_status VARCHAR(30);
BEGIN
    SELECT work_status, position INTO v_staff_status, v_staff_pos FROM employees WHERE employee_id = NEW.sales_staff_id;
    IF v_staff_status IS DISTINCT FROM 'ACTIVE' THEN
        RAISE EXCEPTION 'Nhân viên đã ngừng làm việc, không được lập hóa đơn.';
    END IF;

    -- Ràng buộc: Nhân viên kho không được bán hàng
    IF v_staff_pos NOT IN ('ROLE_SALES', 'ROLE_ADMIN') THEN
        RAISE EXCEPTION 'Nhân viên kho không được bán hàng (chỉ nhân viên bán hàng hoặc quản trị viên mới được lập hóa đơn).';
    END IF;

    SELECT status INTO v_warehouse_status FROM warehouses WHERE warehouse_id = NEW.warehouse_id;
    IF v_warehouse_status IS DISTINCT FROM 'ACTIVE' THEN
        RAISE EXCEPTION 'Kho đã ngừng hoạt động, không được chọn cho hóa đơn.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Quản lý kho, tồn kho, ràng buộc ngừng bán
-- Trigger kiểm tra ràng buộc trước khi vô hiệu hóa kho
CREATE OR REPLACE FUNCTION fn_check_warehouse_deactivation()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status = 'ACTIVE' AND NEW.status = 'INACTIVE' THEN
        -- 1. Kiểm tra tồn kho phải bằng 0
        IF EXISTS (SELECT 1 FROM inventories WHERE warehouse_id = NEW.warehouse_id AND quantity > 0) THEN
            RAISE EXCEPTION 'Không thể ngừng hoạt động kho khi vẫn còn hàng tồn.';
        END IF;

        -- 2. Kiểm tra chứng từ treo
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

-- Nhập thuốc từ nhà cung cấp
-- 1. Trigger tự động tính thành tiền dòng chi tiết phiếu nhập
CREATE OR REPLACE FUNCTION fn_calc_import_item()
RETURNS TRIGGER AS $$
BEGIN
    NEW.sub_total := NEW.quantity * NEW.unit_price;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 2. Trigger tự động cập nhật tổng tiền phiếu nhập
CREATE OR REPLACE FUNCTION fn_update_import_total()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE import_slips
    SET total_amount = COALESCE((
        SELECT SUM(sub_total) FROM import_slip_items WHERE import_slip_id = COALESCE(NEW.import_slip_id, OLD.import_slip_id)
    ), 0.00)
    WHERE import_slip_id = COALESCE(NEW.import_slip_id, OLD.import_slip_id);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- 3. Stored Procedure xác nhận nhập kho (cộng tồn kho UPSERT)
CREATE OR REPLACE PROCEDURE sp_confirm_import_slip(p_import_slip_id BIGINT)
LANGUAGE plpgsql AS $$
DECLARE
    v_warehouse_id BIGINT;
    v_status VARCHAR(30);
BEGIN
    SELECT warehouse_id, status INTO v_warehouse_id, v_status
    FROM import_slips WHERE import_slip_id = p_import_slip_id FOR UPDATE;

    IF v_status <> 'PENDING' THEN
        RAISE EXCEPTION 'Phiếu nhập không ở trạng thái chờ duyệt (PENDING).';
    END IF;

    -- Cộng tồn kho bằng UPSERT
    INSERT INTO inventories(warehouse_id, batch_id, quantity, updated_at)
    SELECT v_warehouse_id, isi.batch_id, isi.quantity, CURRENT_TIMESTAMP
    FROM import_slip_items isi
    WHERE isi.import_slip_id = p_import_slip_id
    ON CONFLICT (warehouse_id, batch_id)
    DO UPDATE SET
        quantity = inventories.quantity + EXCLUDED.quantity,
        updated_at = CURRENT_TIMESTAMP;

    -- Đổi trạng thái sang IMPORTED
    UPDATE import_slips SET status = 'IMPORTED' WHERE import_slip_id = p_import_slip_id;
END;
$$;

-- Bán thuốc và lập hóa đơn
-- 1. Query tìm kiếm lô FEFO khả dụng tại kho
SELECT b.batch_id, b.batch_code, b.exp_date, i.quantity, p.selling_price
FROM batches b
JOIN inventories i ON i.batch_id = b.batch_id
JOIN products p ON p.product_id = b.product_id
WHERE i.warehouse_id = :warehouseId
  AND b.product_id = :productId
  AND i.quantity > 0
  AND b.exp_date > CURRENT_DATE
ORDER BY b.exp_date ASC;

-- 2. Stored Procedure xác nhận thanh toán hóa đơn (Atomic + FOR UPDATE)
CREATE OR REPLACE PROCEDURE sp_confirm_invoice_payment(p_invoice_id BIGINT)
LANGUAGE plpgsql AS $$
DECLARE
    v_warehouse_id BIGINT;
    v_status VARCHAR(30);
    v_cur_stock INT;
    r RECORD;
BEGIN
    SELECT warehouse_id, status INTO v_warehouse_id, v_status
    FROM invoices WHERE invoice_id = p_invoice_id FOR UPDATE;

    IF v_status <> 'UNPAID' THEN
        RAISE EXCEPTION 'Hóa đơn không ở trạng thái chưa thanh toán (UNPAID).';
    END IF;

    -- Kiểm tra hạn dùng và tồn kho từng lô
    FOR r IN
        SELECT ii.batch_id, ii.quantity AS req_qty, b.exp_date
        FROM invoice_items ii
        JOIN batches b ON b.batch_id = ii.batch_id
        WHERE ii.invoice_id = p_invoice_id
    LOOP
        IF r.exp_date <= CURRENT_DATE THEN
            RAISE EXCEPTION 'Lô thuốc % đã hết hạn sử dụng (%).', r.batch_id, r.exp_date;
        END IF;

        SELECT quantity INTO v_cur_stock
        FROM inventories
        WHERE warehouse_id = v_warehouse_id AND batch_id = r.batch_id
        FOR UPDATE;

        IF NOT FOUND OR v_cur_stock < r.req_qty THEN
            RAISE EXCEPTION 'Lô % không đủ tồn kho (Tồn: %, Cần bán: %).', r.batch_id, COALESCE(v_cur_stock, 0), r.req_qty;
        END IF;

        -- Trừ kho đúng kho-lô
        UPDATE inventories
        SET quantity = quantity - r.req_qty, updated_at = CURRENT_TIMESTAMP
        WHERE warehouse_id = v_warehouse_id AND batch_id = r.batch_id;
    END LOOP;

    -- Đổi trạng thái sang PAID
    UPDATE invoices SET status = 'PAID' WHERE invoice_id = p_invoice_id;
END;
$$;

-- Xuất hủy thuốc hỏng / hết hạn
-- 1. Trigger bắt buộc lý do hủy và số lượng > 0
CREATE OR REPLACE FUNCTION fn_check_disposal_item()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.quantity <= 0 THEN
        RAISE EXCEPTION 'Số lượng hủy phải lớn hơn 0.';
    END IF;
    IF NEW.reason IS NULL OR TRIM(NEW.reason) = '' THEN
        RAISE EXCEPTION 'Lý do hủy thuốc là bắt buộc.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 2. Stored Procedure xác nhận thực hiện hủy thuốc
CREATE OR REPLACE PROCEDURE sp_confirm_disposal_slip(p_disposal_slip_id BIGINT)
LANGUAGE plpgsql AS $$
DECLARE
    v_warehouse_id BIGINT;
    v_status VARCHAR(30);
    v_cur_stock INT;
    r RECORD;
BEGIN
    SELECT warehouse_id, status INTO v_warehouse_id, v_status
    FROM disposal_slips WHERE disposal_slip_id = p_disposal_slip_id FOR UPDATE;

    IF v_status <> 'PENDING' THEN
        RAISE EXCEPTION 'Phiếu hủy không ở trạng thái chờ hủy (PENDING).';
    END IF;

    -- Kiểm tra và trừ tồn từng dòng
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
            RAISE EXCEPTION 'Từ chối hủy: Lô % không đủ tồn kho (Tồn: %, Cần hủy: %).',
                r.batch_id, COALESCE(v_cur_stock, 0), r.req_qty;
        END IF;

        UPDATE inventories
        SET quantity = quantity - r.req_qty, updated_at = CURRENT_TIMESTAMP
        WHERE warehouse_id = v_warehouse_id AND batch_id = r.batch_id;
    END LOOP;

    -- Cập nhật ngày hủy thực tế và trạng thái DISPOSED
    UPDATE disposal_slips
    SET status = 'DISPOSED', disposal_date = CURRENT_TIMESTAMP
    WHERE disposal_slip_id = p_disposal_slip_id;
END;
$$;

-- Chặn chỉnh sửa hóa đơn đã thanh toán, phiếu nhập đã được xác nhận, phiếu hủy đã hủy, hoặc những phiếu/hóa đơn đã hủy, tính toán dẫn xuất
-- Trigger khóa bất biến chứng từ đã hoàn tất hoặc đã hủy
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

CREATE TRIGGER trg_lock_import_slips BEFORE UPDATE OR DELETE ON import_slips FOR EACH ROW EXECUTE FUNCTION fn_prevent_modify_completed_slips();
CREATE TRIGGER trg_lock_invoices BEFORE UPDATE OR DELETE ON invoices FOR EACH ROW EXECUTE FUNCTION fn_prevent_modify_completed_slips();
CREATE TRIGGER trg_lock_disposal_slips BEFORE UPDATE OR DELETE ON disposal_slips FOR EACH ROW EXECUTE FUNCTION fn_prevent_modify_completed_slips();