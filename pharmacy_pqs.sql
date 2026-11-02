create table roles(
	role_id bigserial primary key,
	role_name varchar(50) not null unique,
	description text
);
create table users(
	user_id bigserial primary key,
	username varchar(50) not null unique,
	password varchar(255) not null,
	full_name varchar(100) not null,
	balance numeric(15,2) not null default 0.00,
	status varchar(20) default 'ACTIVE',
	created_at timestamp default current_timestamp,
	date_of_birth date,
	phone varchar(30),
	address text,
	gender varchar(20),
	position varchar(100),
	work_status varchar(30),
	start_date date
);
create table categories(
	category_id bigserial primary key,
	category_name varchar(100) not null unique,
	description text
);
create table warehouses(
	warehouse_id bigserial primary key,
	warehouse_name varchar(150) not null,
	warehouse_address varchar(255) not null,
	created_at timestamp not null default current_timestamp
);
create table suppliers(
	supplier_id bigserial primary key,
	supplier_name varchar(150) not null,
	phone varchar(30),
	representative_name varchar(100),
	tax_code varchar(50),
	address text,
	created_at timestamp not null default current_timestamp
);
create table user_roles(
	user_id bigint not null references users(user_id) on delete cascade,
	role_id bigint not null references roles(role_id) on delete cascade,
	primary key (user_id, role_id)
);
create table notifications(
	notification_id bigserial primary key,
	sender_id bigint references users(user_id) on delete set null,
	receiver_id bigint not null references users(user_id) on delete cascade,
	type varchar(30) not null,
	reference_id bigint,
	message text not null,
	is_read boolean not null default false,
	action_status varchar(20) not null default 'PENDING',
	created_at timestamp not null default current_timestamp
);
create table products(
	product_id bigserial primary key,
	product_name varchar(200) not null,
	category_id bigint not null references categories(category_id),
	image_url text,
	uses text,
	ingredients text,
	dosage_form varchar(100),
	unit varchar(50),
	import_price numeric(15,2) not null default 10000.0,
	selling_price numeric(15,2) not null default 10000.0,
	total_stock int not null default 0,
	is_active boolean default true,
	created_at timestamp not null default current_timestamp
	
);
create table invoices(
	invoice_id bigserial primary key,
	customer_id bigint not null references users(user_id),
	sales_staff_id bigint not null references users(user_id),
	total_amount numeric(15,2) not null,
	payment_method varchar(30) not null,
	result_state varchar(20) default 'PENDING',
	status varchar(20) not null default 'UNPAID',
	note text,
	created_at timestamp not null default current_timestamp,
	expires_at timestamp not null
);
create table warehouse_slips(
	warehouse_slip_id bigserial primary key,
	type varchar(20) not null,
	warehouse_staff_id bigint not null references users(user_id),
	supplier_id bigint references suppliers(supplier_id),
	warehouse_id bigint references warehouses(warehouse_id),
	disposal_date timestamp,
	total_amount numeric(15,2) not null default 0.00,
	result_state varchar(20) default 'PENDING',
	status varchar(20) not null default 'PENDING',
	created_at timestamp not null default current_timestamp
);
create table invoice_items(
	invoice_items_id bigserial primary key,
	invoice_id bigint not null references invoices(invoice_id) on delete cascade,
	product_id bigint not null references products(product_id),
	quantity int not null,
	unit_price numeric(15,2) not null,
	sub_total numeric(15,2) not null
);
create table batches(
	batch_id bigserial primary key,
	product_id bigint not null references products(product_id),
	warehouse_id bigint references warehouses(warehouse_id),
	mfg_date date not null,
	exp_date date not null,
	quantify int not null,
	import_price numeric(15,2) not null,
	status varchar(30) not null default 'WAITING_FOR_PRICE',
	is_damaged boolean not null default false,
	created_at timestamp not null default current_timestamp,
	updated_at timestamp default current_timestamp
);
create table warehouse_slip_items(
	warehouse_slip_items_id bigserial primary key,
	warehouse_slip_id bigint not null references warehouse_slips(warehouse_slip_id) on delete cascade,
	product_id bigint not null references products(product_id),
	batch_id bigint not null references batches(batch_id),
	quantity int not null,
	unit_price numeric(15,2) not null,
	sub_total numeric(15,2) not null,
	reason text,
	constraint uq_slip_batch unique(warehouse_slip_id, batch_id)
);