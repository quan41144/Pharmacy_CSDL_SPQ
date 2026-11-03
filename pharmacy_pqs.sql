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

-- 1. Kiểm tra nhân viên đang làm việc và kho đang hoạt động khi tạo chứng từ mới
create or replace function fn_check_active_staff_and_warehouse()
returns trigger as $$
declare
	v_staff_status varchar(30);
	v_warehouse_status varchar(30);
	v_staff_id bigint;
begin
	v_staff_id := case
		when tg_table_name = 'invoices' then new.sales_staff_id
		else new.warehouse_staff_id
	end;

	select work_status into v_staff_status from employees where employee_id = v_staff_id;
	if v_staff_status is distinct from 'ACTIVE' then
		raise exception 'Nhan vien da ngung lam viec, khong duoc lap chung tu moi.';
	end if;

	select status into v_warehouse_status from warehouses where warehouse_id = new.warehouse_id;
	if v_warehouse_status is distinct from 'ACTIVE' then
		raise exception 'Kho da ngung hoat dong, khong duoc chon cho chung tu moi.';
	end if;

	return new;
end;
$$ language plpgsql;

create trigger trg_check_import_slip_init
before insert on import_slips
for each row execute function fn_check_active_staff_and_warehouse();

create trigger trg_check_invoice_init
before insert on invoices
for each row execute function fn_check_active_staff_and_warehouse();

create trigger trg_check_disposal_slip_init
before insert on disposal_slips
for each row execute function fn_check_active_staff_and_warehouse();

-- 2. Chặn xóa hoặc sửa chứng từ đã hoàn tất / đã hủy
create or replace function fn_prevent_modify_completed_slips()
returns trigger as $$
begin
	if tg_op = 'DELETE' then
		if old.status in ('IMPORTED', 'PAID', 'DISPOSED', 'CANCELLED') then
			raise exception 'Chung tu da hoan tat hoac da huy, khong duoc xoa.';
		end if;
		return old;
	elsif tg_op = 'UPDATE' then
		if old.status in ('IMPORTED', 'PAID', 'DISPOSED', 'CANCELLED') then
			raise exception 'Chung tu da hoan tat hoac da huy, khong duoc phep chinh sua.';
		end if;
		return new;
	end if;
	return null;
end;
$$ language plpgsql;

create trigger trg_lock_import_slips
before update or delete on import_slips
for each row execute function fn_prevent_modify_completed_slips();

create trigger trg_lock_invoices
before update or delete on invoices
for each row execute function fn_prevent_modify_completed_slips();

create trigger trg_lock_disposal_slips
before update or delete on disposal_slips
for each row execute function fn_prevent_modify_completed_slips();

-- 3. Ràng buộc chỉ cho phép ngừng hoạt động kho khi hết hàng tồn và không còn chứng từ đang chờ
create or replace function fn_check_warehouse_deactivation()
returns trigger as $$
begin
	if old.status = 'ACTIVE' and new.status = 'INACTIVE' then
		if exists (select 1 from inventories where warehouse_id = new.warehouse_id and quantity > 0) then
			raise exception 'Khong the ngung hoat dong kho khi van con hang ton.';
		end if;

		if exists (select 1 from import_slips where warehouse_id = new.warehouse_id and status = 'PENDING')
		   or exists (select 1 from invoices where warehouse_id = new.warehouse_id and status = 'UNPAID')
		   or exists (select 1 from disposal_slips where warehouse_id = new.warehouse_id and status = 'PENDING') then
			raise exception 'Khong the ngung hoat dong kho khi van con chung tu dang cho xu ly.';
		end if;
	end if;
	return new;
end;
$$ language plpgsql;

create trigger trg_check_warehouse_deactivation
before update of status on warehouses
for each row execute function fn_check_warehouse_deactivation();

-- 4. Tự động tính thành tiền dòng nhập và cập nhật tổng tiền phiếu nhập
create or replace function fn_calc_import_item()
returns trigger as $$
declare
	v_status varchar(30);
begin
	select status into v_status from import_slips where import_slip_id = coalesce(new.import_slip_id, old.import_slip_id);
	if v_status <> 'PENDING' then
		raise exception 'Chi duoc them/sua/xoa chi tiet khi phieu nhap o trang thai PENDING.';
	end if;

	if tg_op in ('INSERT', 'UPDATE') then
		new.sub_total := new.quantity * new.unit_price;
		return new;
	end if;
	return old;
end;
$$ language plpgsql;

create trigger trg_calc_import_item
before insert or update or delete on import_slip_items
for each row execute function fn_calc_import_item();

create or replace function fn_update_import_total()
returns trigger as $$
declare
	v_slip_id bigint := coalesce(new.import_slip_id, old.import_slip_id);
begin
	update import_slips
	set total_amount = coalesce((select sum(sub_total) from import_slip_items where import_slip_id = v_slip_id), 0.00)
	where import_slip_id = v_slip_id;
	return null;
end;
$$ language plpgsql;

create trigger trg_update_import_total
after insert or update or delete on import_slip_items
for each row execute function fn_update_import_total();

-- 5. Tự động lấy giá bán hiện tại của thuốc khi thêm dòng hóa đơn, kiểm tra sơ bộ HSD/tồn kho và tính tổng tiền
create or replace function fn_calc_invoice_item()
returns trigger as $$
declare
	v_status varchar(30);
	v_warehouse_id bigint;
	v_exp_date date;
	v_stock int;
	v_selling_price numeric(15,2);
begin
	select status, warehouse_id into v_status, v_warehouse_id
	from invoices
	where invoice_id = coalesce(new.invoice_id, old.invoice_id);

	if v_status <> 'UNPAID' then
		raise exception 'Chi duoc them/sua/xoa chi tiet khi hoa don o trang thai UNPAID.';
	end if;

	if tg_op in ('INSERT', 'UPDATE') then
		select b.exp_date, p.selling_price, coalesce(i.quantity, 0)
		into v_exp_date, v_selling_price, v_stock
		from batches b
		join products p on p.product_id = b.product_id
		left join inventories i on i.batch_id = b.batch_id and i.warehouse_id = v_warehouse_id
		where b.batch_id = new.batch_id;

		if v_exp_date <= current_date then
			raise exception 'Lo thuoc % da het han su dung (%)', new.batch_id, v_exp_date;
		end if;

		if v_stock < new.quantity then
			raise exception 'So luong ton kho khong du cho lo % (Ton: %, Can ban: %)', new.batch_id, v_stock, new.quantity;
		end if;

		-- Bảo toàn giá bán lịch sử từ bảng products nếu chưa truyền vào
		if tg_op = 'INSERT' and (new.unit_price is null or new.unit_price = 0) then
			new.unit_price := v_selling_price;
		end if;

		new.sub_total := new.quantity * new.unit_price;
		return new;
	end if;
	return old;
end;
$$ language plpgsql;

create trigger trg_calc_invoice_item
before insert or update or delete on invoice_items
for each row execute function fn_calc_invoice_item();

create or replace function fn_update_invoice_total()
returns trigger as $$
declare
	v_inv_id bigint := coalesce(new.invoice_id, old.invoice_id);
begin
	update invoices
	set total_amount = coalesce((select sum(sub_total) from invoice_items where invoice_id = v_inv_id), 0.00)
	where invoice_id = v_inv_id;
	return null;
end;
$$ language plpgsql;

create trigger trg_update_invoice_total
after insert or update or delete on invoice_items
for each row execute function fn_update_invoice_total();

-- 6. Kiểm tra sơ bộ tồn kho khi thêm chi tiết phiếu hủy
create or replace function fn_check_disposal_item()
returns trigger as $$
declare
	v_status varchar(30);
	v_warehouse_id bigint;
	v_stock int;
begin
	select status, warehouse_id into v_status, v_warehouse_id
	from disposal_slips
	where disposal_slip_id = coalesce(new.disposal_slip_id, old.disposal_slip_id);

	if v_status <> 'PENDING' then
		raise exception 'Chi duoc them/sua/xoa chi tiet khi phieu huy o trang thai PENDING.';
	end if;

	if tg_op in ('INSERT', 'UPDATE') then
		select coalesce(quantity, 0) into v_stock
		from inventories
		where warehouse_id = v_warehouse_id and batch_id = new.batch_id;

		if v_stock < new.quantity then
			raise exception 'So luong huy vuot qua ton kho cua lo % (Ton: %, Huy: %).', new.batch_id, v_stock, new.quantity;
		end if;
		return new;
	end if;
	return old;
end;
$$ language plpgsql;

create trigger trg_check_disposal_item
before insert or update or delete on disposal_slip_items
for each row execute function fn_check_disposal_item();

-- =====================================================================
-- PHẦN 3: NGHIỆP VỤ NHẬP – BÁN – HỦY (CHUẨN QUY TRÌNH NHÁP -> XÁC NHẬN)
-- =====================================================================

-- 3.1. NGHIỆP VỤ NHẬP THUỐC
-- Tạo mới phiếu nhập (trạng thái PENDING, chưa cộng tồn kho)
create or replace function add_import_slip(
	p_warehouse_staff_id bigint,
	p_supplier_id bigint,
	p_warehouse_id bigint
) returns bigint language plpgsql as $$
declare
	v_import_slip_id bigint;
begin
	insert into import_slips(warehouse_staff_id, supplier_id, warehouse_id, import_date, total_amount, status)
	values (p_warehouse_staff_id, p_supplier_id, p_warehouse_id, current_timestamp, 0.00, 'PENDING')
	returning import_slip_id into v_import_slip_id;
	return v_import_slip_id;
end;
$$;

-- Thêm/gộp dòng chi tiết phiếu nhập (nếu trùng lô trong cùng phiếu thì gộp số lượng)
create or replace function sp_add_import_item(
	p_import_slip_id bigint,
	p_batch_id bigint,
	p_quantity int,
	p_unit_price numeric(15,2)
) returns void language plpgsql as $$
begin
	insert into import_slip_items(import_slip_id, batch_id, quantity, unit_price, sub_total)
	values (p_import_slip_id, p_batch_id, p_quantity, p_unit_price, p_quantity * p_unit_price)
	on conflict (import_slip_id, batch_id)
	do update set
		quantity = import_slip_items.quantity + excluded.quantity,
		unit_price = excluded.unit_price,
		sub_total = (import_slip_items.quantity + excluded.quantity) * excluded.unit_price;
end;
$$;

-- Xác nhận phiếu nhập (PENDING -> IMPORTED) và cộng tồn kho theo cặp (Kho, Lô)
create or replace function sp_confirm_import_slip(p_import_slip_id bigint)
returns void language plpgsql as $$
declare
	v_warehouse_id bigint;
	v_status varchar(30);
	v_item_count int;
begin
	select warehouse_id, status into v_warehouse_id, v_status
	from import_slips
	where import_slip_id = p_import_slip_id
	for update;

	if not found then
		raise exception 'Khong tim thay phieu nhap %.', p_import_slip_id;
	end if;

	if v_status <> 'PENDING' then
		raise exception 'Phieu nhap khong o trang thai cho nhap (PENDING).';
	end if;

	select count(*) into v_item_count from import_slip_items where import_slip_id = p_import_slip_id;
	if v_item_count = 0 then
		raise exception 'Phieu nhap phai co it nhat mot dong chi tiet hop le.';
	end if;

	insert into inventories(warehouse_id, batch_id, quantity, updated_at)
	select v_warehouse_id, batch_id, quantity, current_timestamp
	from import_slip_items
	where import_slip_id = p_import_slip_id
	on conflict (warehouse_id, batch_id)
	do update set
		quantity = inventories.quantity + excluded.quantity,
		updated_at = current_timestamp;

	update import_slips
	set status = 'IMPORTED'
	where import_slip_id = p_import_slip_id;
end;
$$;

-- Hủy phiếu nhập đang chờ (PENDING -> CANCELLED, tồn kho không đổi)
create or replace function sp_cancel_import_slip(p_import_slip_id bigint)
returns void language plpgsql as $$
begin
	update import_slips
	set status = 'CANCELLED'
	where import_slip_id = p_import_slip_id and status = 'PENDING';

	if not found then
		raise exception 'Chi co the huy phieu nhap dang o trang thai PENDING.';
	end if;
end;
$$;

-- 3.2. NGHIỆP VỤ BÁN THUỐC & HÓA ĐƠN
-- Tạo mới hóa đơn (trạng thái UNPAID, chưa trừ tồn kho)
create or replace function add_invoice(
	p_sales_staff_id bigint,
	p_customer_id bigint,
	p_warehouse_id bigint,
	p_note text default null
) returns bigint language plpgsql as $$
declare
	v_invoice_id bigint;
begin
	insert into invoices(sales_staff_id, customer_id, warehouse_id, created_at, total_amount, status, note)
	values (p_sales_staff_id, p_customer_id, p_warehouse_id, current_timestamp, 0.00, 'UNPAID', p_note)
	returning invoice_id into v_invoice_id;
	return v_invoice_id;
end;
$$;

-- Thêm/gộp dòng chi tiết hóa đơn (chưa trừ tồn kho, tự lấy giá bán hiện tại qua Trigger)
create or replace function sp_add_invoice_item(
	p_invoice_id bigint,
	p_batch_id bigint,
	p_quantity int
) returns void language plpgsql as $$
begin
	insert into invoice_items(invoice_id, batch_id, quantity, unit_price, sub_total)
	values (p_invoice_id, p_batch_id, p_quantity, 0.00, 0.00)
	on conflict (invoice_id, batch_id)
	do update set
		quantity = invoice_items.quantity + excluded.quantity;
end;
$$;

-- Xác nhận thanh toán hóa đơn (UNPAID -> PAID): kiểm tra lại toàn bộ HSD & Tồn kho, trừ tồn nguyên tử
create or replace function sp_confirm_invoice_payment(p_invoice_id bigint)
returns void language plpgsql as $$
declare
	v_warehouse_id bigint;
	v_status varchar(30);
	v_item_count int;
	r record;
begin
	select warehouse_id, status into v_warehouse_id, v_status
	from invoices
	where invoice_id = p_invoice_id
	for update;

	if not found then
		raise exception 'Khong tim thay hoa don %.', p_invoice_id;
	end if;

	if v_status <> 'UNPAID' then
		raise exception 'Hoa don khong o trang thai chua thanh toan (UNPAID).';
	end if;

	select count(*) into v_item_count from invoice_items where invoice_id = p_invoice_id;
	if v_item_count = 0 then
		raise exception 'Hoa don phai co it nhat mot dong chi tiet.';
	end if;

	for r in
		select ii.batch_id, ii.quantity as req_qty, b.exp_date, coalesce(inv.quantity, 0) as cur_stock
		from invoice_items ii
		join batches b on b.batch_id = ii.batch_id
		left join inventories inv on inv.warehouse_id = v_warehouse_id and inv.batch_id = ii.batch_id
		where ii.invoice_id = p_invoice_id
		for update of inv
	loop
		if r.exp_date <= current_date then
			raise exception 'Tu choi thanh toan: Lo thuoc % da het han su dung (%).', r.batch_id, r.exp_date;
		end if;

		if r.cur_stock < r.req_qty then
			raise exception 'Tu choi thanh toan: Lo thuoc % khong du ton kho (Ton: %, Can: %).', r.batch_id, r.cur_stock, r.req_qty;
		end if;

		update inventories
		set quantity = quantity - r.req_qty,
		    updated_at = current_timestamp
		where warehouse_id = v_warehouse_id and batch_id = r.batch_id;
	end loop;

	update invoices
	set status = 'PAID'
	where invoice_id = p_invoice_id;
end;
$$;

-- Hủy hóa đơn chưa thanh toán (UNPAID -> CANCELLED, tồn kho không đổi)
create or replace function sp_cancel_invoice(p_invoice_id bigint)
returns void language plpgsql as $$
begin
	update invoices
	set status = 'CANCELLED'
	where invoice_id = p_invoice_id and status = 'UNPAID';

	if not found then
		raise exception 'Chi co the huy hoa don dang o trang thai UNPAID.';
	end if;
end;
$$;

-- 3.3. NGHIỆP VỤ HỦY THUỐC
-- Tạo mới phiếu hủy (trạng thái PENDING, chưa có ngày hủy thực tế, chưa trừ tồn kho)
create or replace function add_disposal_slip(
	p_warehouse_staff_id bigint,
	p_warehouse_id bigint
) returns bigint language plpgsql as $$
declare
	v_disposal_slip_id bigint;
begin
	insert into disposal_slips(warehouse_staff_id, warehouse_id, disposal_date, status)
	values (p_warehouse_staff_id, p_warehouse_id, null, 'PENDING')
	returning disposal_slip_id into v_disposal_slip_id;
	return v_disposal_slip_id;
end;
$$;

-- Thêm/gộp dòng chi tiết phiếu hủy (chưa trừ tồn kho)
create or replace function sp_add_disposal_item(
	p_disposal_slip_id bigint,
	p_batch_id bigint,
	p_quantity int,
	p_reason text
) returns void language plpgsql as $$
begin
	insert into disposal_slip_items(disposal_slip_id, batch_id, quantity, reason)
	values (p_disposal_slip_id, p_batch_id, p_quantity, p_reason)
	on conflict (disposal_slip_id, batch_id)
	do update set
		quantity = disposal_slip_items.quantity + excluded.quantity,
		reason = excluded.reason;
end;
$$;

-- Xác nhận thực hiện hủy thuốc (PENDING -> DISPOSED): trừ tồn kho và ghi nhận ngày hủy thực tế
create or replace function sp_confirm_disposal_slip(p_disposal_slip_id bigint)
returns void language plpgsql as $$
declare
	v_warehouse_id bigint;
	v_status varchar(30);
	v_item_count int;
	r record;
begin
	select warehouse_id, status into v_warehouse_id, v_status
	from disposal_slips
	where disposal_slip_id = p_disposal_slip_id
	for update;

	if not found then
		raise exception 'Khong tim thay phieu huy %.', p_disposal_slip_id;
	end if;

	if v_status <> 'PENDING' then
		raise exception 'Phieu huy khong o trang thai cho huy (PENDING).';
	end if;

	select count(*) into v_item_count from disposal_slip_items where disposal_slip_id = p_disposal_slip_id;
	if v_item_count = 0 then
		raise exception 'Phieu huy phai co it nhat mot dong chi tiet.';
	end if;

	for r in
		select dsi.batch_id, dsi.quantity as req_qty, coalesce(inv.quantity, 0) as cur_stock
		from disposal_slip_items dsi
		left join inventories inv on inv.warehouse_id = v_warehouse_id and inv.batch_id = dsi.batch_id
		where dsi.disposal_slip_id = p_disposal_slip_id
		for update of inv
	loop
		if r.cur_stock < r.req_qty then
			raise exception 'Tu choi xac nhan huy: Lo % khong du ton kho (Ton: %, Can huy: %).', r.batch_id, r.cur_stock, r.req_qty;
		end if;

		update inventories
		set quantity = quantity - r.req_qty,
		    updated_at = current_timestamp
		where warehouse_id = v_warehouse_id and batch_id = r.batch_id;
	end loop;

	update disposal_slips
	set status = 'DISPOSED',
	    disposal_date = current_timestamp
	where disposal_slip_id = p_disposal_slip_id;
end;
$$;

-- Hủy phiếu hủy đang chờ (PENDING -> CANCELLED, tồn kho không đổi)
create or replace function sp_cancel_disposal_slip(p_disposal_slip_id bigint)
returns void language plpgsql as $$
begin
	update disposal_slips
	set status = 'CANCELLED'
	where disposal_slip_id = p_disposal_slip_id and status = 'PENDING';

	if not found then
		raise exception 'Chi co the huy phieu huy dang o trang thai PENDING.';
	end if;
end;
$$;

-- =====================================================================
-- PHẦN 4: CÁC HÀM QUẢN LÝ DANH MỤC, TRA CỨU & THỐNG KÊ (ĐÃ ĐỒNG BỘ CỘT)
-- =====================================================================

-- 4.1. QUẢN LÝ THUỐC & GỢI Ý LÔ FEFO
create or replace procedure add_product(
	p_name varchar,
	p_dosage varchar,
	p_uses text,
	p_dosage_form varchar,
	p_unit varchar,
	p_selling_price numeric(15,2),
	p_category_id bigint
) language plpgsql as $$
begin
	insert into products(product_name, dosage, uses, dosage_form, unit, selling_price, category_id)
	values (p_name, p_dosage, p_uses, p_dosage_form, p_unit, p_selling_price, p_category_id);
end;
$$;

create or replace procedure update_product(
	p_id bigint,
	p_name varchar,
	p_dosage varchar,
	p_uses text,
	p_dosage_form varchar,
	p_unit varchar,
	p_selling_price numeric(15,2),
	p_category_id bigint
) language plpgsql as $$
begin
	update products
	set product_name = p_name,
		dosage = p_dosage,
		uses = p_uses,
		dosage_form = p_dosage_form,
		unit = p_unit,
		selling_price = p_selling_price,
		category_id = p_category_id
	where product_id = p_id;
end;
$$;

create or replace function info_product(p_id bigint)
returns table (
	product_id bigint,
	product_name varchar,
	dosage varchar,
	uses text,
	dosage_form varchar,
	unit varchar,
	selling_price numeric(15,2),
	category_id bigint
) language plpgsql as $$
begin
	return query
	select p.product_id, p.product_name, p.dosage, p.uses, p.dosage_form, p.unit, p.selling_price, p.category_id
	from products p where p.product_id = p_id;
end;
$$;

create or replace function list_product()
returns table(
	product_id bigint,
	product_name varchar,
	category_name varchar,
	dosage varchar,
	uses text,
	dosage_form varchar,
	unit varchar,
	selling_price numeric(15,2),
	total_stock bigint
) language plpgsql as $$
begin
	return query
	select 
		p.product_id, p.product_name, c.category_name, p.dosage, p.uses,
		p.dosage_form, p.unit, p.selling_price,
		coalesce(sum(inv.quantity), 0) as total_stock
	from products p
	left join categories c on p.category_id = c.category_id
	left join batches b on b.product_id = p.product_id
	left join inventories inv on inv.batch_id = b.batch_id
	group by p.product_id, c.category_name
	order by p.product_id;
end;
$$;

create or replace function search_product_by_name(
	p_name varchar,
	p_min_stock int default 0
) returns table(
	product_id bigint,
	product_name varchar,
	unit varchar,
	selling_price numeric(15,2),
	total_stock bigint
) language plpgsql as $$
begin
	return query
	select p.product_id, p.product_name, p.unit, p.selling_price, coalesce(sum(inv.quantity), 0) as total_stock
	from products p
	left join batches b on b.product_id = p.product_id
	left join inventories inv on inv.batch_id = b.batch_id
	where p.product_name ilike '%' || p_name || '%'
	group by p.product_id
	having coalesce(sum(inv.quantity), 0) >= p_min_stock
	order by p.product_id;
end;
$$;

-- Hàm gợi ý lô thuốc khi bán tại kho (ưu tiên lô còn hạn có HSD gần nhất - FEFO)
create or replace function suggest_batches_fefo(
	p_warehouse_id bigint,
	p_product_id bigint
) returns table(
	batch_id bigint,
	batch_code varchar,
	mfg_date date,
	exp_date date,
	available_stock int,
	selling_price numeric(15,2)
) language plpgsql as $$
begin
	return query
	select
		b.batch_id,
		b.batch_code,
		b.mfg_date,
		b.exp_date,
		i.quantity as available_stock,
		p.selling_price
	from inventories i
	join batches b on b.batch_id = i.batch_id
	join products p on p.product_id = b.product_id
	where i.warehouse_id = p_warehouse_id
	  and b.product_id = p_product_id
	  and i.quantity > 0
	  and b.exp_date > current_date
	order by b.exp_date asc;
end;
$$;

-- 4.2. QUẢN LÝ KHÁCH HÀNG
create or replace procedure add_customer(
	p_full_name varchar,
	p_date_of_birth date,
	p_gender varchar,
	p_address text,
	p_note text,
	p_phone varchar
) language plpgsql as $$
begin
	insert into customers(full_name, date_of_birth, gender, address, note, phone)
	values (p_full_name, p_date_of_birth, p_gender, p_address, p_note, p_phone);
end;
$$;

create or replace procedure update_customer(
	p_id bigint,
	p_full_name varchar,
	p_date_of_birth date,
	p_gender varchar,
	p_address text,
	p_note text,
	p_phone varchar
) language plpgsql as $$
begin
	update customers 
	set full_name = p_full_name,
		date_of_birth = p_date_of_birth,
		gender = p_gender,
		address = p_address,
		note = p_note,
		phone = p_phone
	where customer_id = p_id;
end;
$$;

create or replace function list_customer()
returns table(
	customer_id bigint,
	full_name varchar,
	date_of_birth date,
	age int,
	gender varchar,
	address text,
	note text,
	phone varchar
) language plpgsql as $$
begin
	return query
	select 
		c.customer_id, c.full_name, c.date_of_birth,
		extract(year from age(current_date, c.date_of_birth))::int as age,
		c.gender, c.address, c.note, c.phone
	from customers c
	order by c.customer_id;
end;
$$;

-- 4.3. TRA CỨU HÓA ĐƠN & CHI TIẾT HÓA ĐƠN
create or replace function get_invoice_details(p_invoice_id bigint)
returns table (
	invoice_id bigint,
	batch_id bigint,
	batch_code varchar,
	product_name varchar,
	unit varchar,
	quantity int,
	unit_price numeric(15,2),
	sub_total numeric(15,2)
) language plpgsql as $$
begin
	return query
	select 
		ii.invoice_id, ii.batch_id, b.batch_code, p.product_name, p.unit,
		ii.quantity, ii.unit_price, ii.sub_total
	from invoice_items ii
	join batches b on ii.batch_id = b.batch_id
	join products p on b.product_id = p.product_id
	where ii.invoice_id = p_invoice_id
	order by ii.batch_id;
end;
$$;

-- 4.4. THỐNG KÊ DOANH THU (CHỈ TÍNH HÓA ĐƠN ĐÃ THANH TOÁN - status = 'PAID')
create or replace function daily_revenue(p_date date)
returns numeric(15,2) language plpgsql as $$
declare v_revenue numeric(15,2);
begin
	select sum(total_amount) into v_revenue
	from invoices
	where status = 'PAID'
	  and created_at >= p_date::timestamp
	  and created_at < (p_date + interval '1 day')::timestamp;
	return coalesce(v_revenue, 0.00);
end;
$$;

create or replace function monthly_revenue(p_month int, p_year int)
returns numeric(15,2) language plpgsql as $$
declare
	v_date date := make_date(p_year, p_month, 1);
	v_revenue numeric(15,2);
begin
	select sum(total_amount) into v_revenue
	from invoices
	where status = 'PAID'
	  and created_at >= v_date::timestamp
	  and created_at < (v_date + interval '1 month')::timestamp;
	return coalesce(v_revenue, 0.00);
end;
$$;

create or replace function year_revenue(p_year int)
returns numeric(15,2) language plpgsql as $$
declare
	v_date date := make_date(p_year, 1, 1);
	v_revenue numeric(15,2);
begin
	select sum(total_amount) into v_revenue
	from invoices
	where status = 'PAID'
	  and created_at >= v_date::timestamp
	  and created_at < (v_date + interval '1 year')::timestamp;
	return coalesce(v_revenue, 0.00);
end;
$$;

-- 4.5. VIEW THUỘC TÍNH DẪN XUẤT TUỔI (NHÂN VIÊN & KHÁCH HÀNG)
create or replace view vw_employees_with_age as
select
	e.*,
	extract(year from age(current_date, e.date_of_birth))::int as age
from employees e;

create or replace view vw_customers_with_age as
select
	c.*,
	extract(year from age(current_date, c.date_of_birth))::int as age
from customers c;