/**
 * PHARMACY MANAGEMENT SYSTEM - CORE JAVASCRIPT
 * Full-stack Vanilla JavaScript Application
 */

const app = {
    // Application State
    state: {
        token: localStorage.getItem('accessToken'),
        refreshToken: localStorage.getItem('refreshToken'),
        user: JSON.parse(localStorage.getItem('user') || 'null'),
        warehouses: [],
        categories: [],
        activeWarehouseId: null,
        cart: [],
        selectedCustomer: null,
        activeInvoice: null,
        // POS Pagination & Search
        posPage: 0,
        posSize: 20,
        posTotalPages: 1,
        posKeyword: '',
        posCategoryId: null,
        // Debounce Timers
        searchTimer: null,
        custSearchTimer: null,
        supSearchTimer: null,
        importProdSearchTimer: null,
        dispBatchSearchTimer: null,
        // Selected items for slips
        selectedImportSupplier: null,
        selectedImportProduct: null,
        selectedDisposalBatch: null,
        // Report
        reportFilterType: 'MONTH',
        reportFilterValue: '',
        activeReportTab: 'invoices',
        // Admin Master Data
        activeMgmtTab: 'categories'
    },

    // ================= INITIALIZATION & AUTH GUARD =================
    init() {
        if (!this.state.token || !this.state.user) {
            this.showLoginView();
        } else {
            this.showAppView();
        }
    },

    showLoginView() {
        document.getElementById('app-header').style.display = 'none';
        document.querySelectorAll('.view-section').forEach(el => el.classList.remove('active'));
        document.getElementById('view-login').classList.add('active');
    },

    async showAppView() {
        document.getElementById('app-header').style.display = 'block';
        this.updateHeaderUserInfo();
        await this.loadWarehouses();
        await this.loadCategories();
        this.navigate('pos');
        this.initDefaultDateInputs();
    },

    updateHeaderUserInfo() {
        const u = this.state.user;
        if (!u) return;

        document.getElementById('header-user-name').textContent = u.fullName || u.username;
        document.getElementById('header-user-role').textContent = u.position ? u.position.replace('ROLE_', '') : 'STAFF';
        document.getElementById('header-warehouse-name').textContent = u.warehouseName || 'Kho Tổng';

        const isAdmin = u.position === 'ROLE_ADMIN';
        document.querySelectorAll('.admin-only').forEach(el => {
            el.style.display = isAdmin ? 'inline-flex' : 'none';
        });

        if (isAdmin) {
            this.refreshAdminBalance();
        }
    },

    async refreshAdminBalance() {
        try {
            const res = await this.apiFetch('/api/v1/employees/admin-balance');
            if (res.success) {
                const bal = res.data != null ? res.data : 0;
                document.getElementById('val-admin-balance').textContent = this.formatCurrency(bal) + ' đ';
                const kpiBal = document.getElementById('kpi-admin-balance');
                if (kpiBal) kpiBal.textContent = this.formatCurrency(bal) + ' đ';
            }
        } catch (e) {
            console.error('Refresh balance failed', e);
        }
    },

    // ================= API FETCH INTERCEPTOR =================
    async apiFetch(url, options = {}) {
        options.headers = options.headers || {};
        if (this.state.token) {
            options.headers['Authorization'] = 'Bearer ' + this.state.token;
        }
        if (!(options.body instanceof FormData) && !options.headers['Content-Type']) {
            options.headers['Content-Type'] = 'application/json';
        }

        try {
            const response = await fetch(url, options);

            // 401 Unauthorized -> try refresh or redirect to login
            if (response.status === 401) {
                const refreshed = await this.tryRefreshToken();
                if (refreshed) {
                    options.headers['Authorization'] = 'Bearer ' + this.state.token;
                    return (await fetch(url, options)).json();
                } else {
                    this.logout();
                    throw new Error('Phiên đăng nhập đã hết hạn');
                }
            }

            const data = await response.json();
            return data;
        } catch (err) {
            throw err;
        }
    },

    async tryRefreshToken() {
        if (!this.state.refreshToken) return false;
        try {
            const res = await fetch('/api/v1/auth/refresh', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ refreshToken: this.state.refreshToken })
            });
            const data = await res.json();
            if (data.success && data.data && data.data.accessToken) {
                this.state.token = data.data.accessToken;
                this.state.refreshToken = data.data.refreshToken;
                localStorage.setItem('accessToken', data.data.accessToken);
                localStorage.setItem('refreshToken', data.data.refreshToken);
                return true;
            }
        } catch (e) {
            console.error('Refresh token error', e);
        }
        return false;
    },

    async handleLogin(e) {
        e.preventDefault();
        const username = document.getElementById('login-username').value.trim();
        const password = document.getElementById('login-password').value.trim();
        const errEl = document.getElementById('login-error');
        errEl.style.display = 'none';

        try {
            const res = await fetch('/api/v1/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });
            const data = await res.json();

            if (data.success) {
                this.state.token = data.data.accessToken;
                this.state.refreshToken = data.data.refreshToken;
                this.state.user = data.data;
                this.state.activeWarehouseId = data.data.warehouseId || 1;

                localStorage.setItem('accessToken', data.data.accessToken);
                localStorage.setItem('refreshToken', data.data.refreshToken);
                localStorage.setItem('user', JSON.stringify(data.data));

                this.showAppView();
            } else {
                errEl.textContent = data.message || 'Đăng nhập không thành công';
                errEl.style.display = 'block';
            }
        } catch (err) {
            errEl.textContent = 'Lỗi kết nối máy chủ: ' + err.message;
            errEl.style.display = 'block';
        }
    },

    quickFillLogin(user, pass) {
        document.getElementById('login-username').value = user;
        document.getElementById('login-password').value = pass;
    },

    logout() {
        this.state.token = null;
        this.state.refreshToken = null;
        this.state.user = null;
        this.state.cart = [];
        this.state.selectedCustomer = null;
        localStorage.clear();
        this.showLoginView();
    },

    // ================= NAVIGATION =================
    navigate(viewName) {
        document.querySelectorAll('.view-section').forEach(el => el.classList.remove('active'));
        document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));

        const targetView = document.getElementById('view-' + viewName);
        const targetNav = document.getElementById('nav-' + viewName);

        if (targetView) targetView.classList.add('active');
        if (targetNav) targetNav.classList.add('active');

        if (viewName === 'pos') {
            this.loadPosProducts();
        } else if (viewName === 'invoices') {
            this.loadInvoicesList();
        } else if (viewName === 'reports') {
            this.loadReportDashboard();
        } else if (viewName === 'admin-mgmt') {
            this.switchMgmtTab(this.state.activeMgmtTab);
        } else if (viewName === 'import') {
            this.initImportView();
        } else if (viewName === 'disposal') {
            this.initDisposalView();
        }
    },

    // ================= COMMON LOADERS =================
    async loadWarehouses() {
        try {
            const res = await this.apiFetch('/api/v1/warehouses/active');
            if (res.success) {
                this.state.warehouses = res.data;
                const selectPos = document.getElementById('pos-warehouse-select');
                const selectImp = document.getElementById('import-warehouse-select');
                const selectEmp = document.getElementById('emp-warehouse-select');

                const optionsHtml = res.data.map(w => `<option value="${w.warehouseId}">${w.warehouseName}</option>`).join('');

                if (selectPos) selectPos.innerHTML = optionsHtml;
                if (selectImp) selectImp.innerHTML = optionsHtml;
                if (selectEmp) selectEmp.innerHTML = optionsHtml;

                if (this.state.user && this.state.user.warehouseId) {
                    this.state.activeWarehouseId = this.state.user.warehouseId;
                    if (selectPos) selectPos.value = this.state.user.warehouseId;
                    if (selectImp) selectImp.value = this.state.user.warehouseId;
                } else if (res.data.length > 0) {
                    this.state.activeWarehouseId = res.data[0].warehouseId;
                }
            }
        } catch (e) {
            console.error('Load warehouses error', e);
        }
    },

    async loadCategories() {
        try {
            const res = await this.apiFetch('/api/v1/categories');
            if (res.success) {
                this.state.categories = res.data;
                const bar = document.getElementById('category-filter-bar');
                const selectProd = document.getElementById('prod-cat-select');

                if (bar) {
                    let html = `<button class="cat-pill ${this.state.posCategoryId === null ? 'active' : ''}" onclick="app.filterByCategory(null)">Tất cả</button>`;
                    res.data.forEach(c => {
                        html += `<button class="cat-pill ${this.state.posCategoryId === c.categoryId ? 'active' : ''}" onclick="app.filterByCategory(${c.categoryId})">${c.categoryName}</button>`;
                    });
                    bar.innerHTML = html;
                }

                if (selectProd) {
                    selectProd.innerHTML = res.data.map(c => `<option value="${c.categoryId}">${c.categoryName}</option>`).join('');
                }
            }
        } catch (e) {
            console.error('Load categories error', e);
        }
    },

    // ================= POS CATALOG & PRODUCTS =================
    onProductSearchInput(val) {
        clearTimeout(this.state.searchTimer);
        this.state.searchTimer = setTimeout(() => {
            this.state.posKeyword = val.trim();
            this.state.posPage = 0;
            this.loadPosProducts();
        }, 300);
    },

    onProductSearchKeydown(e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            clearTimeout(this.state.searchTimer);
            this.state.posKeyword = e.target.value.trim();
            this.state.posPage = 0;
            this.loadPosProducts();
        }
    },

    onPosWarehouseChange(wId) {
        this.state.activeWarehouseId = parseInt(wId);
        this.state.posPage = 0;
        this.loadPosProducts();
    },

    filterByCategory(catId) {
        this.state.posCategoryId = catId;
        this.state.posPage = 0;
        document.querySelectorAll('#category-filter-bar .cat-pill').forEach(btn => btn.classList.remove('active'));
        if (event && event.target) event.target.classList.add('active');
        this.loadPosProducts();
    },

    async loadPosProducts() {
        const grid = document.getElementById('product-grid');
        grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 3rem; color: var(--gray-400);"><i class="fa-solid fa-spinner fa-spin fa-2x"></i><br>Đang tải danh sách thuốc...</div>';

        const params = new URLSearchParams({
            page: this.state.posPage,
            size: this.state.posSize,
            warehouseId: this.state.activeWarehouseId || ''
        });
        if (this.state.posKeyword) params.append('keyword', this.state.posKeyword);
        if (this.state.posCategoryId) params.append('categoryId', this.state.posCategoryId);

        try {
            const res = await this.apiFetch('/api/v1/products?' + params.toString());
            if (res.success && res.data) {
                const pageData = res.data;
                this.state.posTotalPages = pageData.totalPages || 1;
                document.getElementById('pos-page-info').textContent = `Trang ${this.state.posPage + 1} / ${this.state.posTotalPages}`;
                document.getElementById('pos-prev-page').disabled = this.state.posPage <= 0;
                document.getElementById('pos-next-page').disabled = this.state.posPage >= this.state.posTotalPages - 1;

                if (!pageData.content || pageData.content.length === 0) {
                    grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 3rem; color: var(--gray-400);">Không tìm thấy thuốc phù hợp.</div>';
                    return;
                }

                let html = '';
                pageData.content.forEach(p => {
                    const fallbackImg = 'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=300';
                    const img = p.imageUrl ? p.imageUrl : fallbackImg;
                    const stock = p.totalStock != null ? p.totalStock : 0;
                    const stockClass = stock > 0 ? 'color: var(--success-dark);' : 'color: var(--danger);';

                    html += `
                        <div class="product-card">
                            <img class="product-card-img" src="${img}" alt="${p.productName}" onerror="this.src='${fallbackImg}'" onclick="app.showProductDetail(${p.productId})">
                            <div class="product-card-body" onclick="app.showProductDetail(${p.productId})">
                                <span class="product-card-cat">${p.categoryName || 'Thuốc'}</span>
                                <h4 class="product-card-title">${p.productName}</h4>
                                <p class="product-card-uses" title="${p.uses || ''}">${p.uses || 'Chưa có thông tin công dụng'}</p>
                                <div class="product-card-price-row">
                                    <span class="product-card-price">${app.formatCurrency(p.sellingPrice)} đ</span>
                                    <span class="product-card-unit">/ ${p.unit}</span>
                                </div>
                                <div style="display: flex; justify-content: space-between; font-size: 0.75rem; margin-top: 0.25rem;">
                                    <span>Hàm lượng: ${p.dosage || '-'}</span>
                                    <strong style="${stockClass}">Tồn: ${stock}</strong>
                                </div>
                            </div>
                            <div class="product-card-actions">
                                <input type="number" id="qty-input-${p.productId}" value="1" min="1" max="${Math.max(1, stock)}">
                                <button type="button" onclick="app.addToCartFEFO(${p.productId})" ${stock <= 0 ? 'disabled style="background: var(--gray-400); cursor: not-allowed;"' : ''}>
                                    <i class="fa-solid fa-cart-plus"></i> Thêm vào giỏ
                                </button>
                            </div>
                        </div>
                    `;
                });
                grid.innerHTML = html;
            }
        } catch (e) {
            grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 3rem; color: var(--danger);">Lỗi tải danh mục thuốc: ' + e.message + '</div>';
        }
    },

    changePosPage(delta) {
        const newPage = this.state.posPage + delta;
        if (newPage >= 0 && newPage < this.state.posTotalPages) {
            this.state.posPage = newPage;
            this.loadPosProducts();
        }
    },

    async showProductDetail(productId) {
        try {
            const res = await this.apiFetch(`/api/v1/products/${productId}?warehouseId=${this.state.activeWarehouseId || ''}`);
            if (res.success && res.data) {
                const d = res.data;
                const p = d.product;
                document.getElementById('modal-detail-title').textContent = 'Chi tiết: ' + p.productName;
                document.getElementById('modal-detail-cat').textContent = (p.categoryName || 'NHÓM THUỐC').toUpperCase();
                document.getElementById('modal-detail-name').textContent = p.productName;
                document.getElementById('modal-detail-price').textContent = this.formatCurrency(p.sellingPrice) + ' đ / ' + p.unit;
                document.getElementById('modal-detail-dosage').querySelector('span').textContent = p.dosage || 'Không';
                document.getElementById('modal-detail-form').querySelector('span').textContent = p.dosageForm || 'Không';
                document.getElementById('modal-detail-stock').querySelector('span').textContent = (p.totalStock || 0) + ' ' + p.unit;
                document.getElementById('modal-detail-uses').textContent = p.uses || 'Không có mô tả công dụng';
                document.getElementById('modal-detail-img').src = p.imageUrl || 'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=300';

                const tbody = document.getElementById('modal-detail-batches-body');
                if (!d.batches || d.batches.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--gray-400);">Không có lô hàng nào trong kho.</td></tr>';
                } else {
                    tbody.innerHTML = d.batches.map(b => {
                        let badge = '<span class="status-badge status-paid">Còn hạn</span>';
                        if (b.status === 'EXPIRED') badge = '<span class="status-badge status-cancelled">Đã hết hạn</span>';
                        else if (b.status === 'NEAR_EXPIRY') badge = '<span class="status-badge status-unpaid">Gần hết hạn</span>';

                        return `
                            <tr>
                                <td><strong>${b.batchCode}</strong></td>
                                <td>${b.warehouseName}</td>
                                <td>${b.mfgDate}</td>
                                <td>${b.expDate}</td>
                                <td style="text-align: right; font-weight: bold;">${b.quantity}</td>
                                <td style="text-align: center;">${badge}</td>
                            </tr>
                        `;
                    }).join('');
                }

                this.openModal('modal-product-detail');
            }
        } catch (e) {
            alert('Không thể tải chi tiết thuốc: ' + e.getMessage());
        }
    },

    // ================= POS CART & FEFO LOGIC =================
    async addToCartFEFO(productId) {
        const qtyInput = document.getElementById(`qty-input-${productId}`);
        const requestedQty = parseInt(qtyInput ? qtyInput.value : 1) || 1;
        const wId = this.state.activeWarehouseId;

        try {
            // Lấy danh sách lô còn hạn xếp theo FEFO (exp_date ASC)
            const res = await this.apiFetch(`/api/v1/batches/fefo?warehouseId=${wId}&productId=${productId}`);
            if (!res.success || !res.data || res.data.length === 0) {
                alert('Sản phẩm đã hết hàng hoặc không có lô còn hạn sử dụng tại kho này!');
                return;
            }

            const availableBatches = res.data;
            let remainingNeeded = requestedQty;

            for (const batch of availableBatches) {
                if (remainingNeeded <= 0) break;

                const existingItem = this.state.cart.find(it => it.batchId === batch.batchId);
                const currentInCart = existingItem ? existingItem.quantity : 0;
                const canTakeFromBatch = batch.quantity - currentInCart;

                if (canTakeFromBatch > 0) {
                    const takeQty = Math.min(remainingNeeded, canTakeFromBatch);

                    if (existingItem) {
                        existingItem.quantity += takeQty;
                        existingItem.subTotal = existingItem.quantity * existingItem.unitPrice;
                    } else {
                        // Lấy giá bán sản phẩm
                        const prodRes = await this.apiFetch(`/api/v1/products/${productId}`);
                        const prod = prodRes.data.product;

                        this.state.cart.push({
                            productId: prod.productId,
                            productName: prod.productName,
                            batchId: batch.batchId,
                            batchCode: batch.batchCode,
                            unit: prod.unit,
                            quantity: takeQty,
                            unitPrice: prod.sellingPrice,
                            subTotal: takeQty * prod.sellingPrice,
                            maxStock: batch.quantity
                        });
                    }

                    remainingNeeded -= takeQty;
                }
            }

            if (remainingNeeded > 0) {
                alert(`Lô thuốc không đủ số lượng tồn! Đã thêm tối đa số lượng có thể vào giỏ.`);
            }

            this.renderCart();
        } catch (e) {
            alert('Lỗi thêm vào giỏ hàng: ' + e.message);
        }
    },

    changeCartItemQty(batchId, delta) {
        const item = this.state.cart.find(it => it.batchId === batchId);
        if (!item) return;

        const newQty = item.quantity + delta;
        if (newQty <= 0) {
            this.removeFromCart(batchId);
        } else if (newQty > item.maxStock) {
            alert(`Lô ${item.batchCode} chỉ còn tồn tối đa ${item.maxStock} ${item.unit}!`);
        } else {
            item.quantity = newQty;
            item.subTotal = item.quantity * item.unitPrice;
            this.renderCart();
        }
    },

    removeFromCart(batchId) {
        this.state.cart = this.state.cart.filter(it => it.batchId !== batchId);
        this.renderCart();
    },

    clearCart() {
        this.state.cart = [];
        this.state.activeInvoice = null;
        document.getElementById('invoice-note').value = '';
        this.renderCart();
    },

    renderCart() {
        const tbody = document.getElementById('cart-table-body');
        if (this.state.cart.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--gray-400); padding: 2rem;">Giỏ hàng trống</td></tr>';
            document.getElementById('cart-total-qty').textContent = '0';
            document.getElementById('cart-total-amount').textContent = '0 đ';
            document.getElementById('cart-amount-words').textContent = 'Viết bằng chữ: Không đồng chẵn.';
            document.getElementById('btn-create-invoice').disabled = true;
            document.getElementById('btn-confirm-payment').disabled = true;
            document.getElementById('btn-print-k80').disabled = true;
            return;
        }

        let totalQty = 0;
        let totalAmount = 0;
        let html = '';

        this.state.cart.forEach(it => {
            totalQty += it.quantity;
            totalAmount += it.subTotal;

            html += `
                <tr>
                    <td>
                        <strong>${it.productName}</strong><br>
                        <span style="font-size: 0.72rem; color: var(--gray-400);">${it.unit}</span>
                    </td>
                    <td><span style="font-size: 0.75rem; background: var(--gray-100); padding: 2px 4px; border-radius: 3px;">${it.batchCode}</span></td>
                    <td style="text-align: center;">
                        <div class="qty-control" style="justify-content: center;">
                            <button class="qty-btn" onclick="app.changeCartItemQty(${it.batchId}, -1)">-</button>
                            <span>${it.quantity}</span>
                            <button class="qty-btn" onclick="app.changeCartItemQty(${it.batchId}, 1)">+</button>
                        </div>
                    </td>
                    <td style="text-align: right;">${app.formatCurrency(it.unitPrice)}</td>
                    <td style="text-align: right; font-weight: bold;">${app.formatCurrency(it.subTotal)}</td>
                    <td style="text-align: center;">
                        <button style="border: none; background: transparent; color: var(--danger); cursor: pointer;" onclick="app.removeFromCart(${it.batchId})"><i class="fa-solid fa-trash-can"></i></button>
                    </td>
                </tr>
            `;
        });

        tbody.innerHTML = html;
        document.getElementById('cart-total-qty').textContent = totalQty;
        document.getElementById('cart-total-amount').textContent = this.formatCurrency(totalAmount) + ' đ';
        document.getElementById('cart-amount-words').textContent = 'Viết bằng chữ: ' + this.numberToVietnameseWords(totalAmount);

        document.getElementById('btn-create-invoice').disabled = false;
        // Payment and Print button states depends on invoice creation
        if (this.state.activeInvoice && this.state.activeInvoice.status === 'UNPAID') {
            document.getElementById('btn-confirm-payment').disabled = false;
            document.getElementById('btn-print-k80').disabled = true;
        } else if (this.state.activeInvoice && this.state.activeInvoice.status === 'PAID') {
            document.getElementById('btn-confirm-payment').disabled = true;
            document.getElementById('btn-print-k80').disabled = false;
        } else {
            document.getElementById('btn-confirm-payment').disabled = true;
            document.getElementById('btn-print-k80').disabled = true;
        }
    },

    // ================= CUSTOMER SELECTION & LIVE SEARCH =================
    onCustomerSearchInput(val) {
        clearTimeout(this.state.custSearchTimer);
        const dropdown = document.getElementById('customer-dropdown');
        const q = val.trim();

        if (!q) {
            dropdown.style.display = 'none';
            return;
        }

        this.state.custSearchTimer = setTimeout(async () => {
            try {
                const res = await this.apiFetch('/api/v1/customers/live-search?keyword=' + encodeURIComponent(q));
                if (res.success && res.data && res.data.length > 0) {
                    let html = '';
                    res.data.forEach(c => {
                        html += `
                            <div class="live-dropdown-item" onclick="app.selectCustomer(${JSON.stringify(c).replace(/"/g, '&quot;')})">
                                <strong>${c.fullName}</strong> - SĐT: ${c.phone || 'Chưa có'} (NS: ${c.dateOfBirth})
                                <div style="font-size: 0.75rem; color: var(--gray-600);">${c.address}</div>
                            </div>
                        `;
                    });
                    dropdown.innerHTML = html;
                    dropdown.style.display = 'block';
                } else {
                    dropdown.innerHTML = '<div class="live-dropdown-item" style="color: var(--gray-400);">Không tìm thấy khách hàng. Bấm "+ Khách mới" để thêm.</div>';
                    dropdown.style.display = 'block';
                }
            } catch (e) {
                console.error(e);
            }
        }, 300);
    },

    selectCustomer(cust) {
        this.state.selectedCustomer = cust;
        document.getElementById('customer-dropdown').style.display = 'none';
        document.getElementById('customer-search-input').value = '';
        document.getElementById('card-cust-name').textContent = cust.fullName;
        document.getElementById('card-cust-sub').textContent = `SĐT: ${cust.phone || '-'} | ĐC: ${cust.address}`;
    },

    resetCustomerSelection() {
        this.state.selectedCustomer = null;
        document.getElementById('card-cust-name').textContent = 'Khách vãng lai (KHVL)';
        document.getElementById('card-cust-sub').textContent = 'Khách chưa lưu thông tin';
    },

    async handleSaveCustomer(e) {
        e.preventDefault();
        const payload = {
            fullName: document.getElementById('cust-fullname').value.trim(),
            dateOfBirth: document.getElementById('cust-dob').value,
            gender: document.getElementById('cust-gender').value,
            phone: document.getElementById('cust-phone').value.trim(),
            address: document.getElementById('cust-address').value.trim(),
            note: document.getElementById('cust-note').value.trim()
        };

        try {
            const res = await this.apiFetch('/api/v1/customers', {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            if (res.success && res.data) {
                this.closeModal('modal-new-customer');
                this.selectCustomer(res.data);
                e.target.reset();
            } else {
                alert('Lỗi lưu khách hàng: ' + res.message);
            }
        } catch (err) {
            alert('Lỗi lưu khách hàng: ' + err.message);
        }
    },

    // ================= INVOICE CHECKOUT & PAYMENT =================
    async handleCreateInvoice() {
        if (this.state.cart.length === 0) {
            alert('Giỏ hàng trống!');
            return;
        }

        const customerId = this.state.selectedCustomer ? this.state.selectedCustomer.customerId : 1; // 1 is default KHVL
        const warehouseId = this.state.activeWarehouseId;
        const note = document.getElementById('invoice-note').value.trim();

        const items = this.state.cart.map(it => ({
            batchId: it.batchId,
            quantity: it.quantity,
            unitPrice: it.unitPrice
        }));

        try {
            const res = await this.apiFetch('/api/v1/invoices', {
                method: 'POST',
                body: JSON.stringify({ customerId, warehouseId, note, items })
            });

            if (res.success && res.data) {
                this.state.activeInvoice = res.data;
                alert(`Tạo hóa đơn thành công! Mã HĐ: ${res.data.invoiceCode} (Trạng thái: Chưa thanh toán). Bấm bước 2 để xác nhận thanh toán.`);
                document.getElementById('btn-confirm-payment').disabled = false;
                document.getElementById('btn-create-invoice').disabled = true;
            } else {
                alert('Lỗi tạo hóa đơn: ' + res.message);
            }
        } catch (err) {
            alert('Lỗi tạo hóa đơn: ' + err.message);
        }
    },

    async handleConfirmPayment() {
        if (!this.state.activeInvoice) return;

        try {
            const res = await this.apiFetch(`/api/v1/invoices/${this.state.activeInvoice.invoiceId}/confirm-payment`, {
                method: 'POST'
            });

            if (res.success && res.data) {
                this.state.activeInvoice = res.data;
                alert(`✅ Xác nhận thanh toán thành công hóa đơn ${res.data.invoiceCode}! Tồn kho đã được trừ và tiền đã cộng vào tài khoản Quỹ.`);
                document.getElementById('btn-confirm-payment').disabled = true;
                document.getElementById('btn-print-k80').disabled = false;
                this.refreshAdminBalance();
                this.loadPosProducts(); // Refresh stock numbers
            } else {
                alert('Xác nhận thanh toán thất bại: ' + res.message);
            }
        } catch (err) {
            alert('Xác nhận thanh toán thất bại: ' + err.message);
        }
    },

    // ================= K80 THERMAL RECEIPT =================
    openK80Modal(inv = null) {
        const invoice = inv || this.state.activeInvoice;
        if (!invoice) return;

        this.state.activeInvoice = invoice;

        document.getElementById('k80-date').textContent = this.formatDateTime(invoice.createdAt);
        document.getElementById('k80-staff').textContent = invoice.salesStaffName;
        document.getElementById('k80-customer').textContent = invoice.customerName || 'KHVL';
        document.getElementById('k80-code').textContent = invoice.invoiceCode;

        let totalQty = 0;
        let html = '';
        if (invoice.items) {
            invoice.items.forEach((it, idx) => {
                totalQty += it.quantity;
                html += `
                    <tr>
                        <td style="text-align: center;">${idx + 1}</td>
                        <td style="text-align: left; font-weight: bold;">${it.productName.toUpperCase()}</td>
                        <td style="text-align: center;">${it.unit}</td>
                        <td style="text-align: right;">${it.quantity}</td>
                        <td style="text-align: right;">${app.formatCurrency(it.unitPrice)}</td>
                        <td style="text-align: right;">${app.formatCurrency(it.subTotal)}</td>
                    </tr>
                `;
            });
        }

        document.getElementById('k80-items-body').innerHTML = html;
        document.getElementById('k80-total-qty').textContent = totalQty;
        document.getElementById('k80-total-amount').textContent = this.formatCurrency(invoice.totalAmount);
        document.getElementById('k80-words-val').textContent = invoice.totalAmountInWords || this.numberToVietnameseWords(invoice.totalAmount);

        this.openModal('modal-k80');
    },

    printK80Direct() {
        document.body.classList.add('printing-k80');
        window.print();
        setTimeout(() => {
            document.body.classList.remove('printing-k80');
        }, 1000);
    },

    downloadK80Pdf() {
        if (!this.state.activeInvoice) return;
        window.open(`/api/v1/invoices/${this.state.activeInvoice.invoiceId}/pdf`, '_blank');
    },

    // ================= IMPORT SLIP VIEW =================
    initImportView() {
        const today = new Date();
        const mfg = new Date(today);
        mfg.setDate(mfg.getDate() - 3);

        const exp = new Date(today);
        exp.setFullYear(exp.getFullYear() + 1);

        document.getElementById('import-mfg-date').value = mfg.toISOString().split('T')[0];
        document.getElementById('import-exp-date').value = exp.toISOString().split('T')[0];
        this.calcImportTotal();
    },

    onSupplierSearchInput(val) {
        clearTimeout(this.state.supSearchTimer);
        const dropdown = document.getElementById('import-supplier-dropdown');
        const q = val.trim();

        if (!q) {
            dropdown.style.display = 'none';
            return;
        }

        this.state.supSearchTimer = setTimeout(async () => {
            try {
                const res = await this.apiFetch('/api/v1/suppliers/live-search?keyword=' + encodeURIComponent(q));
                if (res.success && res.data && res.data.length > 0) {
                    dropdown.innerHTML = res.data.map(s => `
                        <div class="live-dropdown-item" onclick="app.selectImportSupplier(${JSON.stringify(s).replace(/"/g, '&quot;')})">
                            <strong>NCC${String(s.supplierId).padStart(4, '0')} - ${s.supplierName}</strong> (SĐT: ${s.phone})
                        </div>
                    `).join('');
                    dropdown.style.display = 'block';
                } else {
                    dropdown.innerHTML = '<div class="live-dropdown-item" style="color: var(--gray-400);">Không tìm thấy nhà cung cấp.</div>';
                    dropdown.style.display = 'block';
                }
            } catch (e) { console.error(e); }
        }, 300);
    },

    selectImportSupplier(s) {
        this.state.selectedImportSupplier = s;
        document.getElementById('import-supplier-dropdown').style.display = 'none';
        document.getElementById('import-supplier-input').value = s.supplierName;
        document.getElementById('import-selected-supplier').textContent = `✓ Đã chọn: NCC${String(s.supplierId).padStart(4, '0')} - ${s.supplierName}`;
    },

    onImportProductSearchInput(val) {
        clearTimeout(this.state.importProdSearchTimer);
        const dropdown = document.getElementById('import-product-dropdown');
        const q = val.trim();

        if (!q) {
            dropdown.style.display = 'none';
            return;
        }

        this.state.importProdSearchTimer = setTimeout(async () => {
            try {
                const res = await this.apiFetch('/api/v1/products/live-search?keyword=' + encodeURIComponent(q));
                if (res.success && res.data && res.data.length > 0) {
                    dropdown.innerHTML = res.data.map(p => `
                        <div class="live-dropdown-item" onclick="app.selectImportProduct(${JSON.stringify(p).replace(/"/g, '&quot;')})">
                            <div style="display: flex; align-items: center; gap: 0.5rem;">
                                <img src="${p.imageUrl || 'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=300'}" style="width: 32px; height: 32px; object-fit: cover; border-radius: 4px;">
                                <div>
                                    <strong>${p.productName}</strong> (${p.unit})
                                    <div style="font-size: 0.72rem; color: var(--gray-400);">Giá nhập mặc định: 10.000 đ</div>
                                </div>
                            </div>
                        </div>
                    `).join('');
                    dropdown.style.display = 'block';
                } else {
                    dropdown.innerHTML = '<div class="live-dropdown-item" style="color: var(--gray-400);">Không tìm thấy thuốc. Bấm "+ Thêm thuốc" để tạo mới.</div>';
                    dropdown.style.display = 'block';
                }
            } catch (e) { console.error(e); }
        }, 300);
    },

    selectImportProduct(p) {
        this.state.selectedImportProduct = p;
        document.getElementById('import-product-dropdown').style.display = 'none';
        document.getElementById('import-product-input').value = p.productName;
        document.getElementById('import-product-name-disp').value = p.productName + ' (' + p.unit + ')';
        document.getElementById('import-selected-product').textContent = `✓ Đã ghim: ${p.productName}`;
        this.calcImportTotal();
    },

    calcImportTotal() {
        const qty = parseInt(document.getElementById('import-quantity').value) || 0;
        const total = qty * 10000;
        document.getElementById('import-calc-box').textContent = `Tổng tiền cần thanh toán: ${this.formatCurrency(total)} VNĐ`;
    },

    async handleCreateImportSlip(e) {
        e.preventDefault();

        if (!this.state.selectedImportSupplier) {
            alert('Vui lòng chọn Nhà cung cấp!');
            return;
        }
        if (!this.state.selectedImportProduct) {
            alert('Vui lòng chọn Thuốc cần nhập!');
            return;
        }

        const payload = {
            supplierId: this.state.selectedImportSupplier.supplierId,
            warehouseId: parseInt(document.getElementById('import-warehouse-select').value),
            productId: this.state.selectedImportProduct.productId,
            batchCode: document.getElementById('import-batch-code').value.trim(),
            mfgDate: document.getElementById('import-mfg-date').value,
            expDate: document.getElementById('import-exp-date').value,
            quantity: parseInt(document.getElementById('import-quantity').value),
            unitPrice: 10000.00
        };

        const btn = document.getElementById('btn-submit-import');
        btn.disabled = true;
        btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Đang xử lý...';

        try {
            const res = await this.apiFetch('/api/v1/import-slips', {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            if (res.success) {
                this.showCenterToast('✅ Đã gửi yêu cầu xác nhận đến Chủ cửa hàng', 'green', 2000);
                e.target.reset();
                document.getElementById('import-selected-supplier').textContent = '';
                document.getElementById('import-selected-product').textContent = '';
                document.getElementById('import-supplier-input').value = '';
                document.getElementById('import-product-input').value = '';
                this.state.selectedImportSupplier = null;
                this.state.selectedImportProduct = null;
                this.initImportView();
            } else {
                if (res.message && res.message.includes('Số dư chủ cửa hàng không đủ')) {
                    this.showCenterToast('❌ Yêu cầu không thành công (Số dư chủ cửa hàng không đủ)', 'red', 2000);
                } else {
                    alert('Lỗi tạo phiếu nhập: ' + res.message);
                }
            }
        } catch (err) {
            if (err.message && err.message.includes('Số dư chủ cửa hàng không đủ')) {
                this.showCenterToast('❌ Yêu cầu không thành công (Số dư chủ cửa hàng không đủ)', 'red', 2000);
            } else {
                alert('Lỗi tạo phiếu nhập: ' + err.message);
            }
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<i class="fa-solid fa-paper-plane"></i> Yêu cầu xác nhận';
        }
    },

    // ================= DISPOSAL SLIP VIEW =================
    initDisposalView() {
        this.state.selectedDisposalBatch = null;
        document.getElementById('disposal-batch-input').value = '';
        document.getElementById('disposal-batch-code-disp').value = '';
        document.getElementById('disposal-product-name-disp').value = '';
        document.getElementById('disposal-warehouse-name-disp').value = '';
        document.getElementById('disposal-stock-disp').value = '0';
        document.getElementById('form-disposal-slip').reset();
    },

    onDisposalBatchSearchInput(val) {
        clearTimeout(this.state.dispBatchSearchTimer);
        const dropdown = document.getElementById('disposal-batch-dropdown');
        const q = val.trim();

        if (!q) {
            dropdown.style.display = 'none';
            return;
        }

        this.state.dispBatchSearchTimer = setTimeout(async () => {
            try {
                const res = await this.apiFetch(`/api/v1/batches/search-disposal?keyword=${encodeURIComponent(q)}&warehouseId=${this.state.activeWarehouseId || ''}`);
                if (res.success && res.data && res.data.length > 0) {
                    dropdown.innerHTML = res.data.map(b => `
                        <div class="live-dropdown-item" onclick="app.selectDisposalBatch(${JSON.stringify(b).replace(/"/g, '&quot;')})">
                            <strong>Mã lô: ${b.batchId} (${b.batchCode})</strong> | Tồn: <strong>${b.quantity}</strong> | Trạng thái: <strong>${b.status}</strong>
                            <div style="font-size: 0.75rem; color: var(--gray-600);">Kho: ${b.warehouseName} | NSX: ${b.mfgDate} - HSD: ${b.expDate}</div>
                        </div>
                    `).join('');
                    dropdown.style.display = 'block';
                } else {
                    dropdown.innerHTML = '<div class="live-dropdown-item" style="color: var(--gray-400);">Không tìm thấy lô hàng có tồn kho phù hợp.</div>';
                    dropdown.style.display = 'block';
                }
            } catch (e) { console.error(e); }
        }, 300);
    },

    selectDisposalBatch(b) {
        this.state.selectedDisposalBatch = b;
        document.getElementById('disposal-batch-dropdown').style.display = 'none';
        document.getElementById('disposal-batch-input').value = `${b.batchCode} (Mã ${b.batchId})`;
        document.getElementById('disposal-batch-code-disp').value = b.batchCode;
        document.getElementById('disposal-product-name-disp').value = `Thuốc ID: ${b.productId}`;
        document.getElementById('disposal-warehouse-name-disp').value = b.warehouseName;
        document.getElementById('disposal-stock-disp').value = b.quantity;
        document.getElementById('disposal-quantity').max = b.quantity;
    },

    async handleCreateDisposalSlip(e) {
        e.preventDefault();
        if (!this.state.selectedDisposalBatch) {
            alert('Vui lòng chọn Lô hàng cần hủy!');
            return;
        }

        const b = this.state.selectedDisposalBatch;
        const qty = parseInt(document.getElementById('disposal-quantity').value);
        const reason = document.getElementById('disposal-reason').value.trim();

        if (qty > b.quantity) {
            alert(`Số lượng hủy (${qty}) không được vượt quá số lượng tồn (${b.quantity})!`);
            return;
        }

        const payload = {
            warehouseId: b.warehouseId,
            batchId: b.batchId,
            quantity: qty,
            reason: reason
        };

        try {
            const res = await this.apiFetch('/api/v1/disposal-slips', {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            if (res.success) {
                this.showCenterToast('✅ Thành công', 'green', 2000);
                this.initDisposalView();
                this.loadPosProducts(); // Update stock
            } else {
                alert('Lỗi xác nhận hủy hàng: ' + res.message);
            }
        } catch (err) {
            alert('Lỗi xác nhận hủy hàng: ' + err.message);
        }
    },

    // ================= INVOICES MANAGEMENT VIEW =================
    async loadInvoicesList() {
        const tbody = document.getElementById('invoices-table-body');
        tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; padding: 2rem;">Đang tải danh sách hóa đơn...</td></tr>';

        try {
            const res = await this.apiFetch('/api/v1/invoices');
            if (res.success && res.data) {
                if (res.data.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--gray-400); padding: 2rem;">Chưa có hóa đơn nào.</td></tr>';
                    return;
                }

                tbody.innerHTML = res.data.map(inv => {
                    let badge = '<span class="status-badge status-unpaid">Chưa thanh toán</span>';
                    if (inv.status === 'PAID') badge = '<span class="status-badge status-paid">Đã thanh toán</span>';
                    else if (inv.status === 'CANCELLED') badge = '<span class="status-badge status-cancelled">Đã hủy</span>';

                    let actions = `<button class="btn-secondary" style="padding: 0.25rem 0.5rem; font-size: 0.75rem;" onclick="app.openK80Modal(${JSON.stringify(inv).replace(/"/g, '&quot;')})"><i class="fa-solid fa-print"></i> K80</button>`;
                    if (inv.status === 'UNPAID') {
                        actions += ` <button class="btn-success" style="padding: 0.25rem 0.5rem; font-size: 0.75rem;" onclick="app.confirmPaymentById(${inv.invoiceId})">Thanh toán</button>`;
                        actions += ` <button class="btn-danger" style="padding: 0.25rem 0.5rem; font-size: 0.75rem;" onclick="app.cancelInvoiceById(${inv.invoiceId})">Hủy</button>`;
                    }

                    return `
                        <tr>
                            <td><strong>${inv.invoiceCode}</strong></td>
                            <td>${inv.customerName}</td>
                            <td>${inv.salesStaffName}</td>
                            <td>${inv.warehouseName}</td>
                            <td>${app.formatDateTime(inv.createdAt)}</td>
                            <td style="text-align: right; font-weight: bold;">${app.formatCurrency(inv.totalAmount)} đ</td>
                            <td style="text-align: center;">${badge}</td>
                            <td style="text-align: center;">${actions}</td>
                        </tr>
                    `;
                }).join('');
            }
        } catch (e) {
            tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--danger);">Lỗi tải hóa đơn: ' + e.message + '</td></tr>';
        }
    },

    async confirmPaymentById(id) {
        if (!confirm('Xác nhận thanh toán cho hóa đơn này?')) return;
        try {
            const res = await this.apiFetch(`/api/v1/invoices/${id}/confirm-payment`, { method: 'POST' });
            if (res.success) {
                alert('Thanh toán thành công!');
                this.loadInvoicesList();
                this.refreshAdminBalance();
            } else {
                alert('Lỗi: ' + res.message);
            }
        } catch (e) { alert('Lỗi: ' + e.message); }
    },

    async cancelInvoiceById(id) {
        if (!confirm('Hủy hóa đơn này?')) return;
        try {
            const res = await this.apiFetch(`/api/v1/invoices/${id}/cancel`, { method: 'POST' });
            if (res.success) {
                alert('Hủy thành công!');
                this.loadInvoicesList();
            } else {
                alert('Lỗi: ' + res.message);
            }
        } catch (e) { alert('Lỗi: ' + e.message); }
    },

    // ================= ADMIN DASHBOARD & REPORTS =================
    onReportFilterTypeChange(type) {
        this.state.reportFilterType = type;
        const valInput = document.getElementById('report-filter-value');
        const now = new Date();
        if (type === 'DAY') {
            valInput.placeholder = 'dd/MM/yyyy (VD: ' + this.formatDate(now) + ')';
            valInput.value = this.formatDate(now);
        } else if (type === 'YEAR') {
            valInput.placeholder = 'yyyy (VD: ' + now.getFullYear() + ')';
            valInput.value = now.getFullYear();
        } else {
            valInput.placeholder = 'MM/yyyy (VD: ' + (now.getMonth() + 1) + '/' + now.getFullYear() + ')';
            valInput.value = (now.getMonth() + 1) + '/' + now.getFullYear();
        }
    },

    async loadReportDashboard() {
        const type = document.getElementById('report-filter-type').value;
        const val = document.getElementById('report-filter-value').value.trim();
        this.state.reportFilterType = type;
        this.state.reportFilterValue = val;

        const params = new URLSearchParams({ filterType: type, filterValue: val });

        try {
            // Load 4 KPI Cards
            const resKpi = await this.apiFetch('/api/v1/reports/kpis?' + params.toString());
            if (resKpi.success && resKpi.data) {
                const k = resKpi.data;
                document.getElementById('kpi-import-amount').textContent = this.formatCurrency(k.totalImportAmount) + ' đ';
                document.getElementById('kpi-import-count').textContent = k.totalImportsCount + ' phiếu nhập đã duyệt';

                document.getElementById('kpi-revenue-amount').textContent = this.formatCurrency(k.totalRevenue) + ' đ';
                document.getElementById('kpi-invoices-count').textContent = k.totalInvoicesCount + ' hóa đơn đã thanh toán';

                document.getElementById('kpi-profit-amount').textContent = this.formatCurrency(k.grossProfit) + ' đ';
                document.getElementById('kpi-admin-balance').textContent = this.formatCurrency(k.adminBalance) + ' đ';

                // A4 report sync
                document.getElementById('a4-val-import').textContent = this.formatCurrency(k.totalImportAmount) + ' đ';
                document.getElementById('a4-val-revenue').textContent = this.formatCurrency(k.totalRevenue) + ' đ';
                document.getElementById('a4-val-profit').textContent = this.formatCurrency(k.grossProfit) + ' đ';
                document.getElementById('a4-val-balance').textContent = this.formatCurrency(k.adminBalance) + ' đ';
                document.getElementById('a4-report-title').textContent = 'BÁO CÁO DOANH THU & TỒN KHO - ' + k.filterPeriod.toUpperCase();
            }

            // Load Invoices Table in period
            const resInv = await this.apiFetch('/api/v1/reports/invoices?' + params.toString());
            if (resInv.success && resInv.data) {
                const tbody = document.getElementById('report-invoices-body');
                const a4body = document.getElementById('a4-invoices-body');

                tbody.innerHTML = resInv.data.map(inv => `
                    <tr>
                        <td><strong>${inv.invoiceCode}</strong></td>
                        <td>${inv.customerName}</td>
                        <td>${inv.salesStaffName}</td>
                        <td>${app.formatDateTime(inv.createdAt)}</td>
                        <td style="text-align: right; font-weight: bold;">${app.formatCurrency(inv.totalAmount)} đ</td>
                        <td style="text-align: center;"><span class="status-badge status-paid">${inv.status}</span></td>
                        <td style="text-align: center;">
                            <button class="btn-secondary" style="padding: 0.25rem 0.5rem; font-size: 0.75rem;" onclick="app.openK80Modal(${JSON.stringify(inv).replace(/"/g, '&quot;')})">K80</button>
                        </td>
                    </tr>
                `).join('');

                a4body.innerHTML = resInv.data.map(inv => `
                    <tr>
                        <td style="text-align: center;">${inv.invoiceCode}</td>
                        <td>${inv.customerName}</td>
                        <td>${inv.salesStaffName}</td>
                        <td style="text-align: center;">${app.formatDateTime(inv.createdAt)}</td>
                        <td style="text-align: right;">${app.formatCurrency(inv.totalAmount)} đ</td>
                        <td style="text-align: center;">${inv.status}</td>
                    </tr>
                `).join('');
            }

            // Load Imports Table in period
            const resImp = await this.apiFetch('/api/v1/import-slips');
            if (resImp.success && resImp.data) {
                const tbody = document.getElementById('report-imports-body');
                tbody.innerHTML = resImp.data.map(imp => {
                    let badge = '<span class="status-badge status-unpaid">Chờ duyệt</span>';
                    if (imp.status === 'IMPORTED') badge = '<span class="status-badge status-paid">Đã nhập</span>';
                    else if (imp.status === 'CANCELLED') badge = '<span class="status-badge status-cancelled">Đã hủy</span>';

                    let actions = '-';
                    if (imp.status === 'PENDING') {
                        actions = `
                            <button class="btn-success" style="padding: 0.25rem 0.5rem; font-size: 0.75rem;" onclick="app.approveImportSlip(${imp.importSlipId})">Duyệt</button>
                            <button class="btn-danger" style="padding: 0.25rem 0.5rem; font-size: 0.75rem;" onclick="app.rejectImportSlip(${imp.importSlipId})">Hủy</button>
                        `;
                    }

                    return `
                        <tr>
                            <td><strong>PN${String(imp.importSlipId).padStart(5, '0')}</strong></td>
                            <td>${imp.supplierName}</td>
                            <td>${imp.warehouseStaffName}</td>
                            <td>${imp.warehouseName}</td>
                            <td>${app.formatDateTime(imp.importDate)}</td>
                            <td style="text-align: right; font-weight: bold;">${app.formatCurrency(imp.totalAmount)} đ</td>
                            <td style="text-align: center;">${badge}</td>
                            <td style="text-align: center;">${actions}</td>
                        </tr>
                    `;
                }).join('');
            }

            // Load Inventory Stock Table
            const resStock = await this.apiFetch('/api/v1/reports/inventory');
            if (resStock.success && resStock.data) {
                const tbody = document.getElementById('report-stock-body');
                tbody.innerHTML = resStock.data.map(it => {
                    let badge = '<span class="status-badge status-paid">Còn hạn</span>';
                    if (it.expiryStatus === 'Hết hạn') badge = '<span class="status-badge status-cancelled">Hết hạn</span>';
                    else if (it.expiryStatus === 'Gần hết hạn') badge = '<span class="status-badge status-unpaid">Gần hết hạn</span>';

                    return `
                        <tr>
                            <td>${it.warehouseName}</td>
                            <td><strong>${it.productName}</strong></td>
                            <td>${it.categoryName}</td>
                            <td>${it.batchCode}</td>
                            <td>${it.mfgDate}</td>
                            <td>${it.expDate}</td>
                            <td style="text-align: right; font-weight: bold;">${it.quantity} ${it.unit}</td>
                            <td style="text-align: right;">${app.formatCurrency(it.sellingPrice)} đ</td>
                            <td style="text-align: right;">${app.formatCurrency(it.totalValue)} đ</td>
                            <td style="text-align: center;">${badge}</td>
                        </tr>
                    `;
                }).join('');
            }
        } catch (e) {
            console.error('Report dashboard load error', e);
        }
    },

    switchReportTab(tabName) {
        this.state.activeReportTab = tabName;
        document.querySelectorAll('#rep-tab-invoices, #rep-tab-imports, #rep-tab-stock').forEach(el => el.style.display = 'none');
        document.querySelectorAll('#tab-btn-rep-inv, #tab-btn-rep-imp, #tab-btn-rep-stock').forEach(el => el.classList.remove('active'));

        document.getElementById('rep-tab-' + tabName).style.display = 'block';
        if (tabName === 'invoices') document.getElementById('tab-btn-rep-inv').classList.add('active');
        if (tabName === 'imports') document.getElementById('tab-btn-rep-imp').classList.add('active');
        if (tabName === 'stock') document.getElementById('tab-btn-rep-stock').classList.add('active');
    },

    async approveImportSlip(id) {
        if (!confirm('Bạn có chắc muốn phê duyệt phiếu nhập này? (Tiền sẽ được trừ vào tài khoản Admin và tồn kho sẽ được cộng).')) return;
        try {
            const res = await this.apiFetch(`/api/v1/import-slips/${id}/confirm`, { method: 'POST' });
            if (res.success) {
                alert('Phê duyệt thành công!');
                this.loadReportDashboard();
                this.refreshAdminBalance();
            } else {
                alert('Lỗi phê duyệt: ' + res.message);
            }
        } catch (e) {
            alert('Lỗi phê duyệt: ' + e.message);
        }
    },

    async rejectImportSlip(id) {
        if (!confirm('Từ chối / Hủy phiếu nhập này?')) return;
        try {
            const res = await this.apiFetch(`/api/v1/import-slips/${id}/cancel`, { method: 'POST' });
            if (res.success) {
                alert('Đã hủy phiếu nhập!');
                this.loadReportDashboard();
            } else {
                alert('Lỗi: ' + res.message);
            }
        } catch (e) { alert('Lỗi: ' + e.message); }
    },

    printA4Report() {
        document.body.classList.add('printing-a4');
        window.print();
        setTimeout(() => {
            document.body.classList.remove('printing-a4');
        }, 1000);
    },

    downloadPdfReport() {
        const type = this.state.reportFilterType;
        const val = this.state.reportFilterValue;
        window.open(`/api/v1/reports/pdf?filterType=${type}&filterValue=${encodeURIComponent(val)}`, '_blank');
    },

    downloadExcelReport() {
        const type = this.state.reportFilterType;
        const val = this.state.reportFilterValue;
        window.open(`/api/v1/reports/excel?filterType=${type}&filterValue=${encodeURIComponent(val)}`, '_blank');
    },

    // ================= ADMIN MASTER DATA MANAGEMENT =================
    switchMgmtTab(tabName) {
        this.state.activeMgmtTab = tabName;
        document.querySelectorAll('#view-admin-mgmt .cat-pill').forEach(btn => btn.classList.remove('active'));
        const activeBtn = document.getElementById('mgmt-tab-' + tabName);
        if (activeBtn) activeBtn.classList.add('active');

        this.renderMgmtTable(tabName);
    },

    async renderMgmtTable(tabName) {
        const container = document.getElementById('mgmt-table-container');
        container.innerHTML = '<div style="text-align: center; padding: 2rem;"><i class="fa-solid fa-spinner fa-spin"></i> Đang tải...</div>';

        try {
            if (tabName === 'categories') {
                const res = await this.apiFetch('/api/v1/categories');
                if (res.success) {
                    container.innerHTML = `
                        <table class="data-table">
                            <thead><tr><th>ID</th><th>Tên nhóm thuốc</th><th>Mô tả</th><th>Thao tác</th></tr></thead>
                            <tbody>
                                ${res.data.map(c => `
                                    <tr>
                                        <td>${c.categoryId}</td>
                                        <td><strong>${c.categoryName}</strong></td>
                                        <td>${c.description || '-'}</td>
                                        <td><button class="btn-danger" style="padding: 0.2rem 0.5rem; font-size: 0.75rem;" onclick="app.deleteCategory(${c.categoryId})">Xóa</button></td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    `;
                }
            } else if (tabName === 'products') {
                const res = await this.apiFetch('/api/v1/products?size=100');
                if (res.success && res.data) {
                    container.innerHTML = `
                        <table class="data-table">
                            <thead><tr><th>ID</th><th>Tên thuốc</th><th>Nhóm</th><th>ĐVT</th><th>Giá bán hiện tại</th><th>Tồn</th><th>Thao tác</th></tr></thead>
                            <tbody>
                                ${res.data.content.map(p => `
                                    <tr>
                                        <td>${p.productId}</td>
                                        <td><strong>${p.productName}</strong></td>
                                        <td>${p.categoryName}</td>
                                        <td>${p.unit}</td>
                                        <td style="font-weight: bold; color: #e11d48;">${app.formatCurrency(p.sellingPrice)} đ</td>
                                        <td>${p.totalStock}</td>
                                        <td>
                                            <button class="btn-secondary" style="padding: 0.2rem 0.5rem; font-size: 0.75rem;" onclick="app.editProduct(${JSON.stringify(p).replace(/"/g, '&quot;')})">Sửa</button>
                                            <button class="btn-danger" style="padding: 0.2rem 0.5rem; font-size: 0.75rem;" onclick="app.deleteProduct(${p.productId})">Xóa</button>
                                        </td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    `;
                }
            } else if (tabName === 'warehouses') {
                const res = await this.apiFetch('/api/v1/warehouses');
                if (res.success) {
                    container.innerHTML = `
                        <table class="data-table">
                            <thead><tr><th>ID</th><th>Tên kho</th><th>Địa chỉ</th><th>Trạng thái</th><th>Đổi trạng thái</th></tr></thead>
                            <tbody>
                                ${res.data.map(w => `
                                    <tr>
                                        <td>${w.warehouseId}</td>
                                        <td><strong>${w.warehouseName}</strong></td>
                                        <td>${w.warehouseAddress}</td>
                                        <td><span class="status-badge ${w.status === 'ACTIVE' ? 'status-active' : 'status-inactive'}">${w.status}</span></td>
                                        <td>
                                            <button class="btn-secondary" style="padding: 0.2rem 0.5rem; font-size: 0.75rem;" onclick="app.toggleWarehouseStatus(${w.warehouseId}, '${w.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'}')">
                                                Chuyển sang ${w.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'}
                                            </button>
                                        </td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    `;
                }
            } else if (tabName === 'suppliers') {
                const res = await this.apiFetch('/api/v1/suppliers');
                if (res.success) {
                    container.innerHTML = `
                        <table class="data-table">
                            <thead><tr><th>ID</th><th>Tên nhà cung cấp</th><th>SĐT</th><th>Mã số thuế</th><th>Người đại diện</th><th>Địa chỉ</th></tr></thead>
                            <tbody>
                                ${res.data.map(s => `
                                    <tr>
                                        <td>${s.supplierId}</td>
                                        <td><strong>${s.supplierName}</strong></td>
                                        <td>${s.phone}</td>
                                        <td>${s.taxCode || '-'}</td>
                                        <td>${s.representativeName || '-'}</td>
                                        <td>${s.address || '-'}</td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    `;
                }
            } else if (tabName === 'employees') {
                const res = await this.apiFetch('/api/v1/employees');
                if (res.success) {
                    container.innerHTML = `
                        <table class="data-table">
                            <thead><tr><th>ID</th><th>Username</th><th>Họ tên</th><th>Chức vụ</th><th>Kho</th><th>Trạng thái</th><th>Số dư Quỹ</th></tr></thead>
                            <tbody>
                                ${res.data.map(e => `
                                    <tr>
                                        <td>${e.employeeId}</td>
                                        <td><strong>${e.username}</strong></td>
                                        <td>${e.fullName}</td>
                                        <td>${e.position}</td>
                                        <td>${e.warehouseName || '-'}</td>
                                        <td><span class="status-badge ${e.workStatus === 'ACTIVE' ? 'status-active' : 'status-inactive'}">${e.workStatus}</span></td>
                                        <td style="font-weight: bold;">${e.balance != null ? app.formatCurrency(e.balance) + ' đ' : '-'}</td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    `;
                }
            }
        } catch (e) {
            container.innerHTML = '<div style="color: var(--danger); padding: 1rem;">Lỗi tải dữ liệu: ' + e.message + '</div>';
        }
    },

    onMgmtAddClick() {
        const tab = this.state.activeMgmtTab;
        if (tab === 'categories') {
            const name = prompt('Nhập tên nhóm thuốc mới:');
            if (name) {
                const desc = prompt('Nhập mô tả (tùy chọn):');
                this.apiFetch('/api/v1/categories', {
                    method: 'POST',
                    body: JSON.stringify({ categoryName: name, description: desc })
                }).then(res => {
                    if (res.success) this.renderMgmtTable('categories');
                    else alert('Lỗi: ' + res.message);
                });
            }
        } else if (tab === 'products') {
            document.getElementById('form-login').reset();
            document.getElementById('prod-id').value = '';
            document.getElementById('modal-prod-title').textContent = 'Thêm thuốc mới';
            this.openModal('modal-new-product');
        } else if (tab === 'warehouses') {
            const name = prompt('Nhập tên kho:');
            const addr = prompt('Nhập địa chỉ kho:');
            if (name && addr) {
                this.apiFetch('/api/v1/warehouses', {
                    method: 'POST',
                    body: JSON.stringify({ warehouseName: name, warehouseAddress: addr, status: 'ACTIVE' })
                }).then(res => {
                    if (res.success) this.renderMgmtTable('warehouses');
                    else alert('Lỗi: ' + res.message);
                });
            }
        } else if (tab === 'suppliers') {
            this.openModal('modal-new-supplier');
        } else if (tab === 'employees') {
            this.openModal('modal-new-employee');
        }
    },

    editProduct(p) {
        document.getElementById('prod-id').value = p.productId;
        document.getElementById('modal-prod-title').textContent = 'Cập nhật thuốc: ' + p.productName;
        document.getElementById('prod-name').value = p.productName;
        document.getElementById('prod-cat-select').value = p.categoryId;
        document.getElementById('prod-unit').value = p.unit;
        document.getElementById('prod-price').value = p.sellingPrice;
        document.getElementById('prod-dosage').value = p.dosage || '';
        document.getElementById('prod-form').value = p.dosageForm || '';
        document.getElementById('prod-uses').value = p.uses || '';
        document.getElementById('prod-img-url').value = p.imageUrl || '';
        this.openModal('modal-new-product');
    },

    async handleSaveProduct(e) {
        e.preventDefault();
        const id = document.getElementById('prod-id').value;
        const payload = {
            productId: id ? parseInt(id) : null,
            productName: document.getElementById('prod-name').value.trim(),
            categoryId: parseInt(document.getElementById('prod-cat-select').value),
            unit: document.getElementById('prod-unit').value.trim(),
            sellingPrice: parseFloat(document.getElementById('prod-price').value),
            dosage: document.getElementById('prod-dosage').value.trim(),
            dosageForm: document.getElementById('prod-form').value.trim(),
            uses: document.getElementById('prod-uses').value.trim(),
            imageUrl: document.getElementById('prod-img-url').value.trim()
        };

        try {
            const res = await this.apiFetch('/api/v1/products', {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            if (res.success) {
                this.closeModal('modal-new-product');
                this.renderMgmtTable('products');
                this.loadPosProducts();
            } else {
                alert('Lỗi lưu thuốc: ' + res.message);
            }
        } catch (err) { alert('Lỗi: ' + err.message); }
    },

    async deleteProduct(id) {
        if (!confirm('Bạn có chắc muốn xóa thuốc này?')) return;
        try {
            const res = await this.apiFetch(`/api/v1/products/${id}`, { method: 'DELETE' });
            if (res.success) {
                this.renderMgmtTable('products');
                this.loadPosProducts();
            } else {
                alert('Lỗi: ' + res.message);
            }
        } catch (e) { alert('Lỗi: ' + e.message); }
    },

    async deleteCategory(id) {
        if (!confirm('Bạn có chắc muốn xóa nhóm thuốc này?')) return;
        try {
            const res = await this.apiFetch(`/api/v1/categories/${id}`, { method: 'DELETE' });
            if (res.success) {
                this.renderMgmtTable('categories');
                this.loadCategories();
            } else {
                alert('Lỗi: ' + res.message);
            }
        } catch (e) { alert('Lỗi: ' + e.message); }
    },

    async toggleWarehouseStatus(wId, targetStatus) {
        if (!confirm(`Chuyển trạng thái kho sang ${targetStatus}? Trigger PostgreSQL sẽ tự động kiểm tra xem kho còn tồn hàng hoặc chứng từ đang chờ xử lý hay không.`)) return;

        try {
            const res = await this.apiFetch('/api/v1/warehouses', {
                method: 'POST',
                body: JSON.stringify({ warehouseId: wId, status: targetStatus, warehouseName: '', warehouseAddress: '' })
            });

            if (res.success) {
                this.renderMgmtTable('warehouses');
            } else {
                alert('Không thể đổi trạng thái kho: ' + res.message);
            }
        } catch (e) {
            alert('Không thể đổi trạng thái kho: ' + e.message);
        }
    },

    async handleSaveEmployee(e) {
        e.preventDefault();
        const payload = {
            username: document.getElementById('emp-username').value.trim(),
            password: document.getElementById('emp-password').value.trim() || '123456',
            fullName: document.getElementById('emp-fullname').value.trim(),
            position: document.getElementById('emp-position').value,
            warehouseId: parseInt(document.getElementById('emp-warehouse-select').value) || 1,
            dateOfBirth: document.getElementById('emp-dob').value,
            gender: document.getElementById('emp-gender').value,
            phone: document.getElementById('emp-phone').value.trim(),
            workStatus: document.getElementById('emp-status').value,
            balance: parseFloat(document.getElementById('emp-balance').value) || 0
        };

        try {
            const res = await this.apiFetch('/api/v1/employees', {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            if (res.success) {
                this.closeModal('modal-new-employee');
                this.renderMgmtTable('employees');
            } else {
                alert('Lỗi: ' + res.message);
            }
        } catch (err) { alert('Lỗi: ' + err.message); }
    },

    // ================= MODAL & TOAST HELPERS =================
    openModal(modalId) {
        const m = document.getElementById(modalId);
        if (m) m.style.display = 'flex';
    },

    closeModal(modalId) {
        const m = document.getElementById(modalId);
        if (m) m.style.display = 'none';
    },

    showCenterToast(message, type = 'green', duration = 2000) {
        const overlay = document.getElementById('center-toast-overlay');
        const card = document.getElementById('center-toast-card');

        card.textContent = message;
        card.className = 'center-toast-card ' + (type === 'red' ? 'center-toast-red' : 'center-toast-green');
        overlay.style.display = 'flex';

        setTimeout(() => {
            overlay.style.display = 'none';
        }, duration);
    },

    initDefaultDateInputs() {
        const now = new Date();
        const monthStr = (now.getMonth() + 1) + '/' + now.getFullYear();
        const repVal = document.getElementById('report-filter-value');
        if (repVal) repVal.value = monthStr;
    },

    // ================= UTILITIES: MONEY TO WORDS & FORMATTERS =================
    formatCurrency(amount) {
        if (amount == null) return '0';
        return new Intl.NumberFormat('vi-VN').format(amount);
    },

    formatDate(dateObj) {
        const d = String(dateObj.getDate()).padStart(2, '0');
        const m = String(dateObj.getMonth() + 1).padStart(2, '0');
        const y = dateObj.getFullYear();
        return `${d}/${m}/${y}`;
    },

    formatDateTime(dateStr) {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        const d = String(date.getDate()).padStart(2, '0');
        const m = String(date.getMonth() + 1).padStart(2, '0');
        const y = date.getFullYear();
        const hr = String(date.getHours()).padStart(2, '0');
        const min = String(date.getMinutes()).padStart(2, '0');
        const sec = String(date.getSeconds()).padStart(2, '0');
        return `${d}/${m}/${y} ${hr}:${min}:${sec}`;
    },

    numberToVietnameseWords(amount) {
        if (!amount || amount === 0) return 'Không đồng chẵn.';

        const digits = ['không', 'một', 'hai', 'ba', 'bốn', 'năm', 'sáu', 'bảy', 'tám', 'chín'];
        const units = ['', 'nghìn', 'triệu', 'tỷ', 'nghìn tỷ', 'triệu tỷ'];

        let num = Math.floor(Math.abs(amount));
        let sNum = String(num);

        while (sNum.length % 3 !== 0) {
            sNum = '0' + sNum;
        }

        const groupCount = sNum.length / 3;
        let result = [];

        for (let i = 0; i < groupCount; i++) {
            const groupIndex = groupCount - 1 - i;
            const h = parseInt(sNum[i * 3]);
            const t = parseInt(sNum[i * 3 + 1]);
            const u = parseInt(sNum[i * 3 + 2]);

            if (h === 0 && t === 0 && u === 0) continue;

            let groupText = [];

            // Hàng trăm
            if (i > 0 || h > 0) {
                groupText.push(digits[h] + ' trăm');
            }

            // Hàng chục
            if (t === 0 && u !== 0) {
                if (i > 0 || h > 0) groupText.push('lẻ');
            } else if (t === 1) {
                groupText.push('mười');
            } else if (t > 1) {
                groupText.push(digits[t] + ' mươi');
            }

            // Hàng đơn vị
            if (u === 1) {
                if (t > 1) groupText.push('mốt');
                else groupText.push(digits[u]);
            } else if (u === 5) {
                if (t > 0) groupText.push('lăm');
                else groupText.push(digits[u]);
            } else if (u > 0) {
                groupText.push(digits[u]);
            }

            // Đơn vị nghìn, triệu, tỷ
            if (groupIndex < units.length && units[groupIndex]) {
                groupText.push(units[groupIndex]);
            }

            result.push(groupText.join(' '));
        }

        let words = result.join(' ').trim().replace(/\s+/g, ' ');
        if (!words) return 'Không đồng chẵn.';

        words = words.charAt(0).toUpperCase() + words.slice(1) + ' đồng chẵn.';
        return words;
    }
};

// Start application when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    app.init();
});
