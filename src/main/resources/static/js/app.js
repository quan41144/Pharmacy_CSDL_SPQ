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
        invSearchTimer: null,
        // Selected items for slips
        selectedImportSupplier: null,
        selectedImportProduct: null,
        importSlipItems: [],
        selectedDisposalBatch: null,
        disposalSlipItems: [],
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
        document.querySelectorAll('.view-section').forEach(el => el.classList.remove('active'));
        this.updateHeaderUserInfo();

        try {
            await this.loadWarehouses();
            await this.loadCategories();
        } catch (e) {
            console.error('Preload warning', e);
        }

        const role = this.state.user ? this.state.user.position : '';
        if (role === 'ROLE_WAREHOUSE') {
            this.navigate('import');
        } else {
            this.navigate('pos');
        }
        this.initDefaultDateInputs();
    },

    updateHeaderUserInfo() {
        const u = this.state.user;
        if (!u) return;

        document.getElementById('header-user-name').textContent = u.fullName || u.username;
        document.getElementById('header-user-role').textContent = u.position ? u.position.replace('ROLE_', '') : 'STAFF';
        document.getElementById('header-warehouse-name').textContent = u.warehouseName || 'Kho Tổng';

        const role = u.position || '';
        const isAdmin = role === 'ROLE_ADMIN';
        const isWarehouse = role === 'ROLE_WAREHOUSE';
        const isSales = role === 'ROLE_SALES';

        // 1. Quản trị viên (Báo cáo & Thống kê, Quản trị Danh mục)
        document.querySelectorAll('.admin-only').forEach(el => {
            el.style.display = isAdmin ? 'inline-flex' : 'none';
        });

        // 2. Nghiệp vụ: Nhân viên kho KHÔNG ĐƯỢC bán hàng -> Ẩn menu Bán hàng (POS)
        const navPos = document.getElementById('nav-pos');
        if (navPos) {
            navPos.style.display = isWarehouse ? 'none' : 'inline-flex';
        }

        // 3. Nghiệp vụ: Nhân viên bán hàng KHÔNG ĐƯỢC nhập/hủy hàng -> Ẩn menu Nhập kho và Hủy hàng
        const navImport = document.getElementById('nav-import');
        if (navImport) {
            navImport.style.display = isSales ? 'none' : 'inline-flex';
        }
        const navDisposal = document.getElementById('nav-disposal');
        if (navDisposal) {
            navDisposal.style.display = isSales ? 'none' : 'inline-flex';
        }

        if (isAdmin) {
            this.refreshAdminBalance();
        }
    },

    async refreshAdminBalance() {
        // Quỹ đã được loại bỏ theo yêu cầu nghiệp vụ
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
        const submitBtn = document.getElementById('btn-login-submit');
        errEl.style.display = 'none';

        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Đang đăng nhập...';
        }

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

                await this.showAppView();
            } else {
                errEl.textContent = data.message || 'Đăng nhập không thành công';
                errEl.style.display = 'block';
            }
        } catch (err) {
            errEl.textContent = 'Lỗi kết nối máy chủ: ' + err.message;
            errEl.style.display = 'block';
        } finally {
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<i class="fa-solid fa-right-to-bracket"></i> Đăng nhập';
            }
        }
    },

    quickFillLogin(user, pass) {
        document.getElementById('login-username').value = user;
        document.getElementById('login-password').value = pass;
        const form = document.getElementById('form-login');
        if (form) form.requestSubmit();
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
        const role = this.state.user ? this.state.user.position : '';
        // Route Guards: Ràng buộc nghiệp vụ theo quyền tài khoản
        if (role === 'ROLE_WAREHOUSE' && viewName === 'pos') {
            this.showCenterToast('⚠️ Nhân viên kho không được phép bán hàng!', 'red', 2500);
            return;
        }
        if (role === 'ROLE_SALES' && (viewName === 'import' || viewName === 'disposal')) {
            this.showCenterToast('⚠️ Nhân viên bán hàng không được phép nhập/hủy hàng!', 'red', 2500);
            return;
        }

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
            const createContainer = document.getElementById('import-create-container');
            if (createContainer) createContainer.style.display = 'grid';
            this.initImportView();
            this.loadImportSlipsList();
        } else if (viewName === 'disposal') {
            const createContainer = document.getElementById('disposal-create-container');
            if (createContainer) createContainer.style.display = 'grid';
            this.initDisposalView();
            this.loadDisposalSlipsList();
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
                const selectDis = document.getElementById('disposal-warehouse-select');

                const optionsHtml = res.data.map(w => `<option value="${w.warehouseId}">${w.warehouseName}</option>`).join('');

                if (selectPos) selectPos.innerHTML = optionsHtml;
                if (selectImp) selectImp.innerHTML = optionsHtml;
                if (selectEmp) selectEmp.innerHTML = optionsHtml;
                if (selectDis) selectDis.innerHTML = optionsHtml;

                if (this.state.user && this.state.user.warehouseId) {
                    this.state.activeWarehouseId = this.state.user.warehouseId;
                    if (selectPos) selectPos.value = this.state.user.warehouseId;
                    if (selectImp) selectImp.value = this.state.user.warehouseId;
                    if (selectDis) selectDis.value = this.state.user.warehouseId;
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
                    const isOutOfStock = stock <= 0;
                    const stockBadge = isOutOfStock
                        ? '<span class="status-badge status-cancelled" style="padding: 0.15rem 0.45rem; font-size: 0.72rem;"><i class="fa-solid fa-circle-xmark"></i> Hết hàng</span>'
                        : `<span style="color: var(--success-dark); font-weight: 700;">Tồn: ${stock}</span>`;

                    html += `
                        <div class="product-card ${isOutOfStock ? 'out-of-stock' : ''}">
                            ${isOutOfStock ? '<span class="badge-stock-out"><i class="fa-solid fa-ban"></i> HẾT HÀNG</span>' : ''}
                            <img class="product-card-img" src="${img}" alt="${p.productName}" onerror="this.src='${fallbackImg}'" onclick="app.showProductDetail(${p.productId})">
                            <div class="product-card-body" onclick="app.showProductDetail(${p.productId})">
                                <span class="product-card-cat">${p.categoryName || 'Thuốc'}</span>
                                <h4 class="product-card-title">${p.productName}</h4>
                                <p class="product-card-uses" title="${p.uses || ''}">${p.uses || 'Chưa có thông tin công dụng'}</p>
                                <div class="product-card-price-row">
                                    <span class="product-card-price">${app.formatCurrency(p.sellingPrice)} đ</span>
                                    <span class="product-card-unit">/ ${p.unit}</span>
                                </div>
                                <div style="display: flex; justify-content: space-between; align-items: center; font-size: 0.75rem; margin-top: 0.35rem;">
                                    <span>Hàm lượng: ${p.dosage || '-'}</span>
                                    ${stockBadge}
                                </div>
                            </div>
                            <div class="product-card-actions">
                                <input type="number" id="qty-input-${p.productId}" value="1" min="1" max="${Math.max(1, stock)}" ${isOutOfStock ? 'disabled style="background: var(--gray-100); cursor: not-allowed;"' : ''}>
                                <button type="button" onclick="app.addToCartFEFO(${p.productId})" ${isOutOfStock ? 'disabled style="background: var(--gray-400); cursor: not-allowed;"' : ''}>
                                    <i class="fa-solid ${isOutOfStock ? 'fa-ban' : 'fa-cart-plus'}"></i> ${isOutOfStock ? 'Hết hàng' : 'Thêm vào giỏ'}
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
                const pStock = p.totalStock != null ? p.totalStock : 0;
                const pOut = pStock <= 0;
                document.getElementById('modal-detail-stock').innerHTML = `<strong>Tồn kho hiện tại:</strong> <span class="status-badge ${pOut ? 'status-cancelled' : 'status-paid'}" style="margin-left: 0.35rem; font-size: 0.82rem;">${pOut ? 'Hết hàng (0 ' + p.unit + ')' : 'Còn hàng (' + pStock + ' ' + p.unit + ')'}</span>`;
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
                alert(`✅ Xác nhận thanh toán thành công hóa đơn ${res.data.invoiceCode}! Tồn kho đã được trừ theo FEFO.`);
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

    // ================= RETAIL INVOICE FORM MODAL =================
    openK80Modal(inv = null) {
        const invoice = inv || this.state.activeInvoice;
        if (!invoice) return;

        if (invoice.status === 'CANCELLED') {
            this.showCenterToast('⚠️ Hóa đơn đã hủy, không thể in!', 'red', 2500);
            return;
        }

        this.state.activeInvoice = invoice;

        // Populate Warehouse & General Info
        const whAddr = invoice.warehouseAddress || (this.state.currentWarehouse ? this.state.currentWarehouse.warehouseAddress : '') || 'Số 123 Đường Giải Phóng, Hai Bà Trưng, Hà Nội';
        const whEl = document.getElementById('k80-warehouse-address');
        if (whEl) whEl.textContent = whAddr;

        const codeEl = document.getElementById('k80-code');
        if (codeEl) codeEl.textContent = invoice.invoiceCode || 'PT00001';

        // Populate Customer Info
        const custEl = document.getElementById('k80-customer');
        if (custEl) custEl.textContent = invoice.customerName || 'Khách lẻ';

        const phoneEl = document.getElementById('k80-customer-phone');
        if (phoneEl) phoneEl.textContent = invoice.customerPhone || '..........................';

        const addrEl = document.getElementById('k80-customer-address');
        if (addrEl) addrEl.textContent = invoice.customerAddress || '....................................................................';

        // Populate Goods Table
        let html = '';
        let count = 0;
        if (invoice.items) {
            invoice.items.forEach((it, idx) => {
                count++;
                const prodLabel = it.productName + (it.unit ? ` (${it.unit})` : '');
                html += `
                    <tr>
                        <td style="text-align: center;">${idx + 1}</td>
                        <td style="text-align: left; font-weight: 600; padding-left: 10px;">${prodLabel}</td>
                        <td style="text-align: center;">${it.quantity}</td>
                        <td style="text-align: right; padding-right: 8px;">${app.formatCurrency(it.unitPrice)}</td>
                        <td style="text-align: right; padding-right: 10px; font-weight: 600;">${app.formatCurrency(it.subTotal)}</td>
                    </tr>
                `;
            });
        }

        // Pad empty rows up to 12 rows for the authentic A4 voucher pad look
        for (let i = count + 1; i <= 12; i++) {
            html += `
                <tr style="height: 28px;">
                    <td style="text-align: center; color: #9ca3af;">${i}</td>
                    <td></td>
                    <td></td>
                    <td></td>
                    <td></td>
                </tr>
            `;
        }
        document.getElementById('k80-items-body').innerHTML = html;

        // Subtotal & Words
        const totalEl = document.getElementById('k80-total-amount');
        if (totalEl) totalEl.textContent = this.formatCurrency(invoice.totalAmount);

        const wordsEl = document.getElementById('k80-words-val');
        if (wordsEl) wordsEl.textContent = invoice.totalAmountInWords || this.numberToVietnameseWords(invoice.totalAmount);

        // Note
        const noteEl = document.getElementById('k80-note');
        if (noteEl) noteEl.textContent = (invoice.note && invoice.note.trim()) ? invoice.note : 'Hàng mua theo đơn, bảo quản nơi khô ráo thoáng mát.';

        // Date breakdown & Pharmacist (Dược sỹ là tên người bán)
        const d = invoice.createdAt ? new Date(invoice.createdAt) : new Date();
        const dayEl = document.getElementById('k80-day');
        if (dayEl) dayEl.textContent = String(d.getDate()).padStart(2, '0');

        const monthEl = document.getElementById('k80-month');
        if (monthEl) monthEl.textContent = String(d.getMonth() + 1).padStart(2, '0');

        const yearEl = document.getElementById('k80-year');
        if (yearEl) yearEl.textContent = String(d.getFullYear()).slice(-2);

        const staffEl = document.getElementById('k80-staff');
        if (staffEl) staffEl.textContent = invoice.salesStaffName || 'Dược sỹ bán hàng';

        this.openModal('modal-k80');
    },

    printK80Direct() {
        if (!this.state.activeInvoice || this.state.activeInvoice.status === 'CANCELLED') {
            this.showCenterToast('⚠️ Hóa đơn đã hủy, không thể in!', 'red', 2500);
            return;
        }
        document.body.classList.add('printing-k80');
        window.print();
        setTimeout(() => {
            document.body.classList.remove('printing-k80');
        }, 1000);
    },

    downloadK80Pdf() {
        if (!this.state.activeInvoice) return;
        if (this.state.activeInvoice.status === 'CANCELLED') {
            this.showCenterToast('⚠️ Hóa đơn đã hủy, không thể xuất PDF!', 'red', 2500);
            return;
        }
        window.open(`/api/v1/invoices/${this.state.activeInvoice.invoiceId}/pdf`, '_blank');
    },

    // ================= IMPORT SLIP VIEW =================
    generateRandomBatchCode(productName) {
        let prefix = 'MED';
        const p = this.state.selectedImportProduct;
        const name = productName || (p ? p.productName : '');

        if (name) {
            const clean = name.normalize("NFD").replace(/[\u0300-\u036f]/g, "")
                              .replace(/[^a-zA-Z]/g, "")
                              .toUpperCase();
            if (clean.length >= 3) {
                prefix = clean.substring(0, 3);
            } else if (clean.length > 0) {
                prefix = clean.padEnd(3, 'X');
            }
        }

        const year = new Date().getFullYear();
        const letters = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
        const randomLetter = letters.charAt(Math.floor(Math.random() * letters.length));
        const code = `LOT-${prefix}-${year}${randomLetter}`;
        const input = document.getElementById('import-batch-code');
        if (input) {
            input.value = code;
        }
        return code;
    },

    initImportView() {
        if (!this.state.importSlipItems) {
            this.state.importSlipItems = [];
        }
        this.renderImportItemsTable();
        this.calcImportTotal();
    },

    onSupplierSearchFocus() {
        const input = document.getElementById('import-supplier-input');
        this.onSupplierSearchInput(input ? input.value : '');
    },

    onSupplierSearchKeydown(event) {
        if (event.key === 'Enter') {
            event.preventDefault();
            clearTimeout(this.state.supSearchTimer);
            const val = document.getElementById('import-supplier-input').value;
            this.searchSuppliersNow(val);
        }
    },

    onSupplierSearchInput(val) {
        clearTimeout(this.state.supSearchTimer);
        this.state.supSearchTimer = setTimeout(() => {
            this.searchSuppliersNow(val);
        }, 200);
    },

    async searchSuppliersNow(q = '') {
        const dropdown = document.getElementById('import-supplier-dropdown');
        if (!dropdown) return;
        try {
            const res = await this.apiFetch('/api/v1/suppliers/live-search?keyword=' + encodeURIComponent(q.trim()));
            if (res.success && res.data && res.data.length > 0) {
                dropdown.innerHTML = `
                    <div style="padding: 0.35rem 0.75rem; background: var(--gray-100); font-size: 0.75rem; font-weight: 700; color: var(--gray-600); border-bottom: 1px solid var(--gray-200);">
                        KẾT QUẢ TÌM KIẾM NHÀ CUNG CẤP (${res.data.length})
                    </div>
                    ${res.data.map(s => `
                        <div class="live-dropdown-item" onclick="app.selectImportSupplier(${JSON.stringify(s).replace(/"/g, '&quot;')})">
                            <div style="display: flex; justify-content: space-between; align-items: center;">
                                <strong><i class="fa-solid fa-truck-field"></i> NCC#${s.supplierId} - ${s.supplierName}</strong>
                                <span style="font-size: 0.75rem; color: var(--primary); font-weight: 600;"><i class="fa-solid fa-phone"></i> ${s.phone || '-'}</span>
                            </div>
                            <div style="font-size: 0.75rem; color: var(--gray-500); margin-top: 0.2rem;">
                                ${s.taxCode ? 'MST: ' + s.taxCode + ' | ' : ''}${s.representativeName ? 'Đại diện: ' + s.representativeName + ' | ' : ''}Địa chỉ: ${s.address || '-'}
                            </div>
                        </div>
                    `).join('')}
                `;
                dropdown.style.display = 'block';
            } else {
                dropdown.innerHTML = '<div class="live-dropdown-item" style="color: var(--gray-400); text-align: center; padding: 1rem;"><i class="fa-solid fa-circle-question"></i> Không tìm thấy nhà cung cấp nào. Bấm "+ Thêm NCC" để thêm mới.</div>';
                dropdown.style.display = 'block';
            }
        } catch (e) { console.error('Supplier search error', e); }
    },

    selectImportSupplier(s) {
        this.state.selectedImportSupplier = s;
        const dropdown = document.getElementById('import-supplier-dropdown');
        if (dropdown) dropdown.style.display = 'none';
        document.getElementById('import-supplier-input').value = s.supplierName;
        document.getElementById('import-selected-supplier').innerHTML = `
            <div class="selected-customer-card" style="background: #f0fdf4; border-color: #86efac; margin-top: 0.35rem;">
                <div>
                    <strong style="color: #166534;"><i class="fa-solid fa-circle-check"></i> NCC#${s.supplierId}: ${s.supplierName}</strong>
                    <div style="font-size: 0.78rem; color: #15803d;">SĐT: ${s.phone || '-'} | Đại diện: ${s.representativeName || '-'}</div>
                </div>
                <button type="button" class="btn-secondary" style="padding: 0.15rem 0.5rem; font-size: 0.75rem;" onclick="app.clearImportSupplier()">✕ Đổi</button>
            </div>
        `;
    },

    clearImportSupplier() {
        this.state.selectedImportSupplier = null;
        document.getElementById('import-supplier-input').value = '';
        document.getElementById('import-selected-supplier').innerHTML = '';
        document.getElementById('import-supplier-input').focus();
    },

    onImportProductSearchFocus() {
        const input = document.getElementById('import-product-input');
        this.onImportProductSearchInput(input ? input.value : '');
    },

    onImportProductSearchKeydown(event) {
        if (event.key === 'Enter') {
            event.preventDefault();
            clearTimeout(this.state.importProdSearchTimer);
            const val = document.getElementById('import-product-input').value;
            this.searchImportProductsNow(val);
        }
    },

    onImportProductSearchInput(val) {
        clearTimeout(this.state.importProdSearchTimer);
        this.state.importProdSearchTimer = setTimeout(() => {
            this.searchImportProductsNow(val);
        }, 200);
    },

    async searchImportProductsNow(q = '') {
        const dropdown = document.getElementById('import-product-dropdown');
        if (!dropdown) return;
        try {
            const res = await this.apiFetch('/api/v1/products/live-search?keyword=' + encodeURIComponent(q.trim()));
            if (res.success && res.data && res.data.length > 0) {
                dropdown.innerHTML = `
                    <div style="padding: 0.35rem 0.75rem; background: var(--gray-100); font-size: 0.75rem; font-weight: 700; color: var(--gray-600); border-bottom: 1px solid var(--gray-200);">
                        KẾT QUẢ TÌM KIẾM THUỐC (${res.data.length}) - Tên, Hoạt chất, Công dụng
                    </div>
                    ${res.data.map(p => `
                        <div class="live-dropdown-item" onclick="app.selectImportProduct(${JSON.stringify(p).replace(/"/g, '&quot;')})">
                            <div style="display: flex; align-items: center; gap: 0.65rem;">
                                <img src="${p.imageUrl || 'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=300'}" style="width: 38px; height: 38px; object-fit: cover; border-radius: 4px; border: 1px solid var(--gray-200);">
                                <div style="flex: 1;">
                                    <div style="display: flex; justify-content: space-between; align-items: center;">
                                        <strong>#${p.productId} - ${p.productName}</strong>
                                        <span class="status-badge status-paid" style="font-size: 0.7rem;">${p.categoryName || 'Thuốc'}</span>
                                    </div>
                                    <div style="font-size: 0.75rem; color: var(--gray-500); line-height: 1.3;">
                                        ĐVT: <strong>${p.unit}</strong> | Giá bán niêm yết: ${app.formatCurrency(p.sellingPrice)} đ
                                    </div>
                                    <div style="font-size: 0.72rem; color: var(--primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 360px;">
                                        ${p.uses ? '<i class="fa-solid fa-stethoscope"></i> ' + p.uses : ''}
                                    </div>
                                </div>
                            </div>
                        </div>
                    `).join('')}
                `;
                dropdown.style.display = 'block';
            } else {
                dropdown.innerHTML = '<div class="live-dropdown-item" style="color: var(--gray-400); text-align: center; padding: 1rem;"><i class="fa-solid fa-circle-question"></i> Không tìm thấy thuốc phù hợp. Bấm "+ Thêm thuốc" để tạo mới.</div>';
                dropdown.style.display = 'block';
            }
        } catch (e) { console.error('Product search error', e); }
    },

    selectImportProduct(p) {
        this.state.selectedImportProduct = p;
        const dropdown = document.getElementById('import-product-dropdown');
        if (dropdown) dropdown.style.display = 'none';
        const input = document.getElementById('import-product-input');
        if (input) input.value = '';

        document.getElementById('import-selected-product').innerHTML = `
            <div class="selected-customer-card" style="background: #eff6ff; border-color: #93c5fd; margin-top: 0.35rem;">
                <div style="display: flex; align-items: center; gap: 0.5rem;">
                    <img src="${p.imageUrl || 'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=300'}" style="width: 28px; height: 28px; object-fit: cover; border-radius: 4px;">
                    <div>
                        <strong style="color: #1e40af;"><i class="fa-solid fa-circle-check"></i> Đang chọn: ${p.productName}</strong> (${p.unit})
                        <div style="font-size: 0.78rem; color: #2563eb;">Nhấn "+ Thêm lô thuốc" để thêm tiếp lô mới cho thuốc này</div>
                    </div>
                </div>
                <button type="button" class="btn-secondary" style="padding: 0.15rem 0.5rem; font-size: 0.75rem;" onclick="app.clearImportProduct()">✕ Bỏ chọn</button>
            </div>
        `;

        // Automatically add as a new batch in the excel-like list
        this.addImportItem(p);
    },

    clearImportProduct() {
        this.state.selectedImportProduct = null;
        const input = document.getElementById('import-product-input');
        if (input) input.value = '';
        const disp = document.getElementById('import-selected-product');
        if (disp) disp.innerHTML = '';
        if (input) input.focus();
    },

    triggerAddBatchClick() {
        if (this.state.selectedImportProduct) {
            this.addImportItem(this.state.selectedImportProduct);
        } else {
            this.showCenterToast('🔍 Vui lòng tìm kiếm & chọn một thuốc ở mục 2 trước khi bấm Thêm lô!', 'blue', 2500);
            const input = document.getElementById('import-product-input');
            if (input) {
                input.focus();
                this.onImportProductSearchFocus();
            }
        }
    },

    addImportItem(p) {
        if (!p) return;
        const today = new Date();
        const mfg = new Date(today);
        mfg.setDate(mfg.getDate() - 3);

        const exp = new Date(today);
        exp.setFullYear(exp.getFullYear() + 1);

        const mfgStr = mfg.toISOString().split('T')[0];
        const expStr = exp.toISOString().split('T')[0];
        const batchCode = this.generateRandomBatchCode(p.productName);

        // Suggested purchase price from selling price or default
        const suggestedPrice = (p.sellingPrice && p.sellingPrice > 0) ? Math.round(p.sellingPrice * 0.7) : 10000;

        if (!this.state.importSlipItems) {
            this.state.importSlipItems = [];
        }

        this.state.importSlipItems.push({
            productId: p.productId,
            productName: p.productName,
            unit: p.unit || 'Đơn vị',
            categoryName: p.categoryName || '',
            batchCode: batchCode,
            mfgDate: mfgStr,
            expDate: expStr,
            quantity: 100,
            unitPrice: suggestedPrice
        });

        this.renderImportItemsTable();
        this.calcImportTotal();
        this.showCenterToast(`✅ Đã thêm lô vào bảng: ${p.productName}`, 'green', 1800);
    },

    renderImportItemsTable() {
        const tbody = document.getElementById('import-items-tbody');
        if (!tbody) return;

        const items = this.state.importSlipItems || [];
        if (items.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="9" style="text-align: center; color: var(--gray-500); padding: 2.25rem 1rem;">
                        <i class="fa-solid fa-boxes-stacked" style="font-size: 2rem; margin-bottom: 0.65rem; color: #94a3b8; display: block;"></i>
                        <span style="font-weight: 600; font-size: 0.95rem; color: var(--gray-700);">Chưa có lô thuốc nào trong phiếu nhập.</span><br>
                        <span style="font-size: 0.85rem; color: var(--gray-500);">Tìm kiếm và chọn thuốc ở ô trên, hoặc nhấn nút <strong>"+ Thêm lô thuốc"</strong> để tạo danh sách nhập hàng.</span>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = items.map((item, idx) => {
            const subTotal = (item.quantity || 0) * (item.unitPrice || 0);
            return `
                <tr style="border-bottom: 1px solid var(--gray-200); background: white;">
                    <td style="text-align: center; font-weight: 700; color: var(--gray-500);">${idx + 1}</td>
                    <td>
                        <strong style="color: var(--primary-dark); font-size: 0.9rem;">${item.productName}</strong>
                        <div style="font-size: 0.75rem; color: var(--gray-500);">ĐVT: <strong>${item.unit}</strong> ${item.categoryName ? '| ' + item.categoryName : ''}</div>
                    </td>
                    <td>
                        <input type="text" class="form-control" style="padding: 0.35rem 0.5rem; font-size: 0.85rem; font-family: monospace; font-weight: 600;" 
                               value="${item.batchCode || ''}" 
                               placeholder="VD: LOT-AUG-2026C"
                               oninput="app.updateImportItemBatchCode(${idx}, this.value)" required>
                    </td>
                    <td>
                        <input type="date" class="form-control" style="padding: 0.35rem 0.4rem; font-size: 0.82rem;" 
                               value="${item.mfgDate || ''}" 
                               onchange="app.updateImportItemMfgDate(${idx}, this.value)" required>
                    </td>
                    <td>
                        <input type="date" class="form-control" style="padding: 0.35rem 0.4rem; font-size: 0.82rem;" 
                               value="${item.expDate || ''}" 
                               onchange="app.updateImportItemExpDate(${idx}, this.value)" required>
                    </td>
                    <td>
                        <input type="number" class="form-control" style="padding: 0.35rem 0.5rem; font-size: 0.85rem; text-align: right; width: 85px; font-weight: 600;" 
                               min="1" value="${item.quantity}" 
                               oninput="app.updateImportItemQuantity(${idx}, this.value)" required>
                    </td>
                    <td>
                        <input type="number" class="form-control" style="padding: 0.35rem 0.5rem; font-size: 0.85rem; text-align: right; width: 120px; font-weight: 600;" 
                               min="0" step="500" value="${item.unitPrice}" 
                               oninput="app.updateImportItemUnitPrice(${idx}, this.value)" required>
                    </td>
                    <td style="text-align: right; font-weight: 700; color: #15803d; font-size: 0.95rem; white-space: nowrap;" id="import-item-subtotal-${idx}">
                        ${this.formatCurrency(subTotal)} đ
                    </td>
                    <td style="text-align: center;">
                        <button type="button" class="btn-danger" style="padding: 0.3rem 0.55rem; font-size: 0.8rem; border-radius: 4px;" 
                                title="Xóa lô này khỏi phiếu" onclick="app.removeImportItem(${idx})">
                            <i class="fa-solid fa-trash-can"></i>
                        </button>
                    </td>
                </tr>
            `;
        }).join('');
    },

    updateImportItemBatchCode(idx, val) {
        if (this.state.importSlipItems && this.state.importSlipItems[idx]) {
            this.state.importSlipItems[idx].batchCode = val.trim();
        }
    },

    updateImportItemMfgDate(idx, val) {
        if (this.state.importSlipItems && this.state.importSlipItems[idx]) {
            this.state.importSlipItems[idx].mfgDate = val;
        }
    },

    updateImportItemExpDate(idx, val) {
        if (this.state.importSlipItems && this.state.importSlipItems[idx]) {
            this.state.importSlipItems[idx].expDate = val;
        }
    },

    updateImportItemQuantity(idx, val) {
        if (this.state.importSlipItems && this.state.importSlipItems[idx]) {
            const qty = parseInt(val) || 0;
            this.state.importSlipItems[idx].quantity = qty;
            const subTotal = qty * (this.state.importSlipItems[idx].unitPrice || 0);
            const cell = document.getElementById(`import-item-subtotal-${idx}`);
            if (cell) cell.textContent = `${this.formatCurrency(subTotal)} đ`;
            this.calcImportTotal();
        }
    },

    updateImportItemUnitPrice(idx, val) {
        if (this.state.importSlipItems && this.state.importSlipItems[idx]) {
            const price = parseFloat(val) || 0;
            this.state.importSlipItems[idx].unitPrice = price;
            const subTotal = (this.state.importSlipItems[idx].quantity || 0) * price;
            const cell = document.getElementById(`import-item-subtotal-${idx}`);
            if (cell) cell.textContent = `${this.formatCurrency(subTotal)} đ`;
            this.calcImportTotal();
        }
    },

    removeImportItem(idx) {
        if (this.state.importSlipItems && this.state.importSlipItems[idx]) {
            const name = this.state.importSlipItems[idx].productName;
            this.state.importSlipItems.splice(idx, 1);
            this.renderImportItemsTable();
            this.calcImportTotal();
            this.showCenterToast(`Đã xóa lô của thuốc: ${name}`, 'red', 1500);
        }
    },

    calcImportTotal() {
        const items = this.state.importSlipItems || [];
        const total = items.reduce((sum, it) => sum + ((it.quantity || 0) * (it.unitPrice || 0)), 0);
        
        const totalEl = document.getElementById('import-total-amount-display');
        if (totalEl) {
            totalEl.textContent = `${this.formatCurrency(total)} VNĐ`;
        }
        const countEl = document.getElementById('import-total-batches-count');
        if (countEl) {
            countEl.textContent = items.length;
        }
    },

    async handleCreateImportSlip(e) {
        e.preventDefault();


        if (!this.state.selectedImportSupplier) {
            alert('Vui lòng chọn Nhà cung cấp!');
            return;
        }

        const warehouseSelect = document.getElementById('import-warehouse-select');
        const warehouseId = warehouseSelect ? parseInt(warehouseSelect.value) : null;
        if (!warehouseId) {
            alert('Vui lòng chọn Kho nhập hàng!');
            return;
        }

        const items = this.state.importSlipItems || [];
        if (items.length === 0) {
            alert('Vui lòng thêm ít nhất một lô thuốc vào danh sách nhập!');
            return;
        }

        // Validate each item
        for (let i = 0; i < items.length; i++) {
            const it = items[i];
            if (!it.batchCode || !it.batchCode.trim()) {
                alert(`Dòng ${i + 1}: Vui lòng nhập số lô sản xuất cho thuốc ${it.productName}!`);
                return;
            }
            if (!it.mfgDate || !it.expDate) {
                alert(`Dòng ${i + 1}: Vui lòng chọn ngày sản xuất và hạn sử dụng cho lô ${it.batchCode}!`);
                return;
            }
            if (new Date(it.expDate) <= new Date(it.mfgDate)) {
                alert(`Dòng ${i + 1}: Hạn sử dụng (${it.expDate}) phải lớn hơn Ngày sản xuất (${it.mfgDate}) cho lô ${it.batchCode}!`);
                return;
            }
            if (!it.quantity || it.quantity <= 0) {
                alert(`Dòng ${i + 1}: Số lượng nhập của lô ${it.batchCode} phải lớn hơn 0!`);
                return;
            }
            if (it.unitPrice === undefined || it.unitPrice === null || it.unitPrice < 0) {
                alert(`Dòng ${i + 1}: Đơn giá nhập của lô ${it.batchCode} phải lớn hơn hoặc bằng 0!`);
                return;
            }
        }

        const payload = {
            supplierId: this.state.selectedImportSupplier.supplierId,
            warehouseId: warehouseId,
            items: items.map(it => ({
                productId: it.productId,
                batchCode: it.batchCode.trim(),
                mfgDate: it.mfgDate,
                expDate: it.expDate,
                quantity: parseInt(it.quantity),
                unitPrice: parseFloat(it.unitPrice)
            }))
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
                this.showCenterToast('✅ Đã lập phiếu nhập thành công (Trạng thái: Chờ Admin phê duyệt)', 'green', 2500);
                this.state.importSlipItems = [];
                this.state.selectedImportSupplier = null;
                this.state.selectedImportProduct = null;
                const selSup = document.getElementById('import-selected-supplier');
                if (selSup) selSup.innerHTML = '';
                const selProd = document.getElementById('import-selected-product');
                if (selProd) selProd.innerHTML = '';
                const supInp = document.getElementById('import-supplier-input');
                if (supInp) supInp.value = '';
                const prodInp = document.getElementById('import-product-input');
                if (prodInp) prodInp.value = '';

                this.renderImportItemsTable();
                this.calcImportTotal();
                this.loadImportSlipsList();
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
            btn.innerHTML = '<i class="fa-solid fa-paper-plane"></i> Lập phiếu nhập (Gửi Admin duyệt)';
        }
    },
    // ================= DISPOSAL SLIP VIEW =================
    initDisposalView() {
        if (!this.state.disposalSlipItems) {
            this.state.disposalSlipItems = [];
        }
        this.renderDisposalItemsTable();
        this.calcDisposalTotal();
    },

    onDisposalWarehouseChange(whId) {
        this.state.disposalSlipItems = [];
        this.renderDisposalItemsTable();
        this.calcDisposalTotal();
        const input = document.getElementById('disposal-batch-input');
        if (input) input.value = '';
        const card = document.getElementById('disposal-selected-batch-card');
        if (card) card.innerHTML = '';
        this.showCenterToast('Đã đổi kho xuất hủy. Danh sách lô đã được làm mới.', 'blue', 1800);
    },

    onDisposalBatchSearchFocus() {
        const input = document.getElementById('disposal-batch-input');
        this.onDisposalBatchSearchInput(input ? input.value : '');
    },

    onDisposalBatchSearchKeydown(event) {
        if (event.key === 'Enter') {
            event.preventDefault();
            clearTimeout(this.state.dispBatchSearchTimer);
            const val = document.getElementById('disposal-batch-input').value;
            this.searchDisposalBatchesNow(val);
        }
    },

    onDisposalBatchSearchInput(val) {
        clearTimeout(this.state.dispBatchSearchTimer);
        this.state.dispBatchSearchTimer = setTimeout(() => {
            this.searchDisposalBatchesNow(val);
        }, 200);
    },

    async searchDisposalBatchesNow(q = '') {
        const dropdown = document.getElementById('disposal-batch-dropdown');
        if (!dropdown) return;

        try {
            const selectDis = document.getElementById('disposal-warehouse-select');
            const whId = (selectDis && selectDis.value) 
                ? selectDis.value 
                : (this.state.user?.warehouseId || this.state.activeWarehouseId || '');
            const res = await this.apiFetch(`/api/v1/batches/search-disposal?keyword=${encodeURIComponent(q.trim())}&warehouseId=${whId}`);
            if (res.success && res.data && res.data.length > 0) {
                dropdown.innerHTML = `
                    <div style="padding: 0.4rem 0.75rem; background: var(--gray-100); font-size: 0.75rem; font-weight: 700; color: var(--gray-600); border-bottom: 1px solid var(--gray-200); display: flex; justify-content: space-between; align-items: center;">
                        <span><i class="fa-solid fa-list-check"></i> KẾT QUẢ TÌM KIẾM LÔ THUỐC (${res.data.length})</span>
                        <span style="color: #b45309; font-size: 0.72rem; font-weight: 600;"><i class="fa-solid fa-arrow-down-short-wide"></i> Ưu tiên cận/hết hạn</span>
                    </div>
                    ${res.data.map(b => {
                        let badgeColor = '#16a34a';
                        let badgeBg = '#dcfce7';
                        let badgeIcon = 'fa-check-circle';
                        if (b.status === 'Hết hạn') {
                            badgeColor = '#dc2626';
                            badgeBg = '#fee2e2';
                            badgeIcon = 'fa-skull-crossbones';
                        } else if (b.status === 'Gần hết hạn') {
                            badgeColor = '#d97706';
                            badgeBg = '#fef3c7';
                            badgeIcon = 'fa-triangle-exclamation';
                        }
                        const prodTitle = b.productName ? `${b.productName} (#${b.productId})` : `Thuốc #${b.productId}`;
                        const unitStr = b.unit ? ` ${b.unit}` : '';
                        return `
                        <div class="live-dropdown-item" onclick="app.selectDisposalBatch(${JSON.stringify(b).replace(/"/g, '&quot;')})">
                            <div style="display: flex; justify-content: space-between; align-items: center;">
                                <strong style="font-size: 0.88rem; color: #1e293b;">
                                    <i class="fa-solid fa-box-archive" style="color: #b45309; margin-right: 4px;"></i> Lô: ${b.batchCode} - ${prodTitle}
                                </strong>
                                <span style="font-size: 0.75rem; font-weight: 700; padding: 2px 8px; border-radius: 9999px; background: ${badgeBg}; color: ${badgeColor}; white-space: nowrap;">
                                    <i class="fa-solid ${badgeIcon}"></i> ${b.status}
                                </span>
                            </div>
                            <div style="font-size: 0.76rem; color: var(--gray-600); margin-top: 0.3rem; display: flex; justify-content: space-between; align-items: center;">
                                <span><i class="fa-solid fa-warehouse"></i> ${b.warehouseName || 'Kho'} | NSX: ${b.mfgDate} | <strong style="color: ${badgeColor};">HSD: ${b.expDate}</strong></span>
                                <span style="font-weight: 700; color: var(--danger); font-size: 0.8rem;">Tồn: ${b.quantity}${unitStr}</span>
                            </div>
                        </div>
                        `;
                    }).join('')}
                `;
                dropdown.style.display = 'block';
            } else {
                dropdown.innerHTML = '<div class="live-dropdown-item" style="color: var(--gray-400); text-align: center; padding: 1rem;"><i class="fa-solid fa-circle-question"></i> Không tìm thấy lô hàng có tồn kho phù hợp.</div>';
                dropdown.style.display = 'block';
            }
        } catch (e) { console.error('Disposal batch search error', e); }
    },

    selectDisposalBatch(b) {
        this.state.selectedDisposalBatch = b;
        const dropdown = document.getElementById('disposal-batch-dropdown');
        if (dropdown) dropdown.style.display = 'none';

        const input = document.getElementById('disposal-batch-input');
        if (input) input.value = '';

        const selectDis = document.getElementById('disposal-warehouse-select');
        if (selectDis && b.warehouseId) {
            selectDis.value = b.warehouseId;
        }

        const card = document.getElementById('disposal-selected-batch-card');
        if (card) {
            let badgeColor = '#16a34a';
            let badgeBg = '#dcfce7';
            if (b.status === 'Hết hạn') {
                badgeColor = '#dc2626';
                badgeBg = '#fee2e2';
            } else if (b.status === 'Gần hết hạn') {
                badgeColor = '#d97706';
                badgeBg = '#fef3c7';
            }
            card.innerHTML = `
                <div class="selected-customer-card" style="background: #fff7ed; border: 1px solid #fdba74; margin-top: 0.4rem; padding: 0.6rem 0.8rem; border-radius: 6px; display: flex; justify-content: space-between; align-items: center;">
                    <div>
                        <strong style="color: #9a3412;"><i class="fa-solid fa-box-archive"></i> Đang chọn: Lô ${b.batchCode}</strong> - ${b.productName}
                        <div style="font-size: 0.78rem; color: #b45309; margin-top: 2px;">
                            Kho: <strong>${b.warehouseName || ''}</strong> | Tồn: <strong>${b.quantity} ${b.unit || ''}</strong> | HSD: <strong>${b.expDate}</strong>
                        </div>
                    </div>
                    <button type="button" class="btn-secondary" style="padding: 0.2rem 0.5rem; font-size: 0.75rem;" onclick="app.clearDisposalBatch()">✕ Bỏ chọn</button>
                </div>
            `;
        }

        // Add to items list
        this.addDisposalItem(b);
    },

    clearDisposalBatch(focus = true) {
        this.state.selectedDisposalBatch = null;
        const input = document.getElementById('disposal-batch-input');
        if (input) input.value = '';
        const card = document.getElementById('disposal-selected-batch-card');
        if (card) card.innerHTML = '';
        if (focus && input) input.focus();
    },

    triggerAddDisposalBatchClick() {
        if (this.state.selectedDisposalBatch) {
            this.addDisposalItem(this.state.selectedDisposalBatch);
        } else {
            this.showCenterToast('🔍 Vui lòng tìm kiếm & chọn một lô hàng ở mục 1 trước khi bấm Thêm!', 'blue', 2500);
            const input = document.getElementById('disposal-batch-input');
            if (input) {
                input.focus();
                this.onDisposalBatchSearchFocus();
            }
        }
    },

    addDisposalItem(b, reRender = true) {
        if (!b) return;

        if (!this.state.disposalSlipItems) {
            this.state.disposalSlipItems = [];
        }

        const selectDis = document.getElementById('disposal-warehouse-select');
        const selectedWhId = selectDis ? parseInt(selectDis.value) : null;
        if (selectedWhId && b.warehouseId && selectedWhId !== b.warehouseId) {
            this.showCenterToast(`⚠️ Lô này thuộc ${b.warehouseName || 'kho khác'}. Vui lòng chọn cùng kho xuất hủy!`, 'red', 2500);
            return;
        }

        const existingIdx = this.state.disposalSlipItems.findIndex(it => it.batchId === b.batchId);
        if (existingIdx !== -1) {
            if (reRender) this.showCenterToast(`💡 Lô ${b.batchCode} đã có trong danh sách hủy!`, 'blue', 2000);
            return;
        }

        let defaultReason = 'Hết hạn sử dụng';
        if (b.status === 'Gần hết hạn') defaultReason = 'Cận date, thu hồi tiêu hủy';
        else if (b.status === 'Còn hạn') defaultReason = 'Hư hỏng bao bì / biến chất';

        this.state.disposalSlipItems.push({
            batchId: b.batchId,
            batchCode: b.batchCode,
            productId: b.productId,
            productName: b.productName || `Thuốc #${b.productId}`,
            unit: b.unit || 'Đơn vị',
            warehouseId: b.warehouseId,
            warehouseName: b.warehouseName || '',
            expDate: b.expDate,
            status: b.status,
            currentStock: b.quantity,
            quantity: b.quantity,
            reason: defaultReason
        });

        if (reRender) {
            this.renderDisposalItemsTable();
            this.calcDisposalTotal();
            this.showCenterToast(`✅ Đã thêm lô vào danh sách hủy: ${b.batchCode}`, 'green', 1800);
        }
    },

    renderDisposalItemsTable() {
        const tbody = document.getElementById('disposal-items-tbody');
        if (!tbody) return;

        const items = this.state.disposalSlipItems || [];
        if (items.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" style="text-align: center; color: var(--gray-500); padding: 2.25rem 1rem;">
                        <i class="fa-solid fa-boxes-stacked" style="font-size: 2rem; margin-bottom: 0.65rem; color: #fdba74; display: block;"></i>
                        <span style="font-weight: 600; font-size: 0.95rem; color: var(--gray-700);">Chưa có lô thuốc nào trong phiếu hủy.</span><br>
                        <span style="font-size: 0.85rem; color: var(--gray-500);">Tìm kiếm và chọn lô hàng ở ô trên để thêm vào danh sách tiêu hủy.</span>
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = items.map((item, idx) => {
            let badgeBg = '#fee2e2';
            let badgeColor = '#dc2626';
            if (item.status === 'Gần hết hạn') {
                badgeBg = '#fef3c7';
                badgeColor = '#d97706';
            } else if (item.status === 'Còn hạn') {
                badgeBg = '#dcfce7';
                badgeColor = '#16a34a';
            }

            return `
                <tr style="border-bottom: 1px solid var(--gray-200); background: white;">
                    <td style="text-align: center; font-weight: 700; color: var(--gray-500);">${idx + 1}</td>
                    <td>
                        <strong style="color: #1e293b; font-size: 0.9rem;">${item.productName}</strong>
                        <div style="font-size: 0.75rem; color: var(--gray-500);">ĐVT: <strong>${item.unit}</strong> | Kho: ${item.warehouseName || ''}</div>
                    </td>
                    <td>
                        <span style="font-family: monospace; font-weight: 700; color: #b45309;">${item.batchCode}</span>
                    </td>
                    <td>
                        <div style="font-size: 0.82rem; font-weight: 600;">${item.expDate}</div>
                        <span style="font-size: 0.7rem; font-weight: 700; padding: 1px 6px; border-radius: 9999px; background: ${badgeBg}; color: ${badgeColor};">
                            ${item.status}
                        </span>
                    </td>
                    <td style="text-align: right; font-weight: 700; color: #dc2626;">
                        ${item.currentStock} ${item.unit}
                    </td>
                    <td>
                        <input type="number" class="form-control" style="padding: 0.35rem 0.5rem; font-size: 0.85rem; text-align: right; width: 95px; font-weight: 700;" 
                               min="1" max="${item.currentStock}" value="${item.quantity}" 
                               oninput="app.updateDisposalItemQuantity(${idx}, this.value)" required>
                    </td>
                    <td>
                        <input type="text" class="form-control" style="padding: 0.35rem 0.5rem; font-size: 0.85rem;" 
                               value="${item.reason || ''}" 
                               placeholder="Lý do hủy (bắt buộc)..."
                               oninput="app.updateDisposalItemReason(${idx}, this.value)" required>
                    </td>
                    <td style="text-align: center;">
                        <button type="button" class="btn-danger" style="padding: 0.3rem 0.55rem; font-size: 0.8rem; border-radius: 4px;" 
                                title="Xóa lô này khỏi phiếu hủy" onclick="app.removeDisposalItem(${idx})">
                            <i class="fa-solid fa-trash-can"></i>
                        </button>
                    </td>
                </tr>
            `;
        }).join('');
    },

    updateDisposalItemQuantity(idx, val) {
        if (this.state.disposalSlipItems && this.state.disposalSlipItems[idx]) {
            const it = this.state.disposalSlipItems[idx];
            let qty = parseInt(val) || 0;
            if (qty > it.currentStock) {
                this.showCenterToast(`⚠️ Số lượng hủy (${qty}) vượt quá tồn kho (${it.currentStock})!`, 'red', 2000);
            }
            it.quantity = qty;
            this.calcDisposalTotal();
        }
    },

    updateDisposalItemReason(idx, val) {
        if (this.state.disposalSlipItems && this.state.disposalSlipItems[idx]) {
            this.state.disposalSlipItems[idx].reason = val;
        }
    },

    removeDisposalItem(idx) {
        if (this.state.disposalSlipItems && this.state.disposalSlipItems[idx]) {
            const code = this.state.disposalSlipItems[idx].batchCode;
            this.state.disposalSlipItems.splice(idx, 1);
            this.renderDisposalItemsTable();
            this.calcDisposalTotal();
            this.showCenterToast(`Đã xóa lô ${code} khỏi danh sách hủy`, 'red', 1500);
        }
    },

    calcDisposalTotal() {
        const items = this.state.disposalSlipItems || [];
        const totalQty = items.reduce((sum, it) => sum + (parseInt(it.quantity) || 0), 0);

        const countEl = document.getElementById('disposal-total-batches-count');
        if (countEl) countEl.textContent = items.length;

        const qtyEl = document.getElementById('disposal-total-qty-display');
        if (qtyEl) qtyEl.textContent = totalQty;
    },

    async handleCreateDisposalSlip(e) {
        e.preventDefault();


        const selectWh = document.getElementById('disposal-warehouse-select');
        const warehouseId = selectWh ? parseInt(selectWh.value) : null;
        if (!warehouseId) {
            alert('Vui lòng chọn Kho xuất hủy!');
            return;
        }

        const items = this.state.disposalSlipItems || [];
        if (items.length === 0) {
            alert('Vui lòng thêm ít nhất một lô hàng vào danh sách hủy!');
            return;
        }

        for (let i = 0; i < items.length; i++) {
            const it = items[i];
            if (!it.quantity || it.quantity <= 0) {
                alert(`Dòng ${i + 1} (Lô ${it.batchCode}): Số lượng hủy phải lớn hơn 0!`);
                return;
            }
            if (it.quantity > it.currentStock) {
                alert(`Dòng ${i + 1} (Lô ${it.batchCode}): Số lượng hủy (${it.quantity}) vượt quá tồn kho (${it.currentStock})!`);
                return;
            }
            if (!it.reason || !it.reason.trim()) {
                alert(`Dòng ${i + 1} (Lô ${it.batchCode}): Vui lòng nhập lý do hủy thuốc!`);
                return;
            }
        }

        const payload = {
            warehouseId: warehouseId,
            items: items.map(it => ({
                batchId: it.batchId,
                quantity: parseInt(it.quantity),
                reason: it.reason.trim()
            }))
        };

        const btn = document.getElementById('btn-submit-disposal');
        btn.disabled = true;
        btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Đang xử lý...';

        try {
            const res = await this.apiFetch('/api/v1/disposal-slips', {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            if (res.success) {
                this.showCenterToast('✅ Đã lập phiếu hủy thành công (Trạng thái: Chờ Admin phê duyệt)', 'green', 2500);
                this.state.disposalSlipItems = [];
                this.state.selectedDisposalBatch = null;
                const batchInp = document.getElementById('disposal-batch-input');
                if (batchInp) batchInp.value = '';
                const card = document.getElementById('disposal-selected-batch-card');
                if (card) card.innerHTML = '';

                this.renderDisposalItemsTable();
                this.calcDisposalTotal();
                this.loadDisposalSlipsList();
                if (document.getElementById('view-reports')?.classList.contains('active')) {
                    this.loadReportDashboard();
                }
            } else {
                alert('Lỗi tạo phiếu hủy: ' + res.message);
            }
        } catch (err) {
            alert('Lỗi tạo phiếu hủy: ' + err.message);
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<i class="fa-solid fa-triangle-exclamation"></i> Lập phiếu hủy (Gửi Admin duyệt)';
        }
    },

    // ================= INVOICES MANAGEMENT VIEW =================
    onInvoiceSearchInput(val) {
        clearTimeout(this.state.invSearchTimer);
        this.state.invSearchTimer = setTimeout(() => {
            this.loadInvoicesList(val);
        }, 200);
    },

    clearInvoiceSearch() {
        const inp = document.getElementById('invoice-search-input');
        if (inp) inp.value = '';
        this.loadInvoicesList('');
    },

    async loadInvoicesList(keyword = '') {
        const tbody = document.getElementById('invoices-table-body');
        tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; padding: 2rem;">Đang tải danh sách hóa đơn...</td></tr>';

        try {
            const kw = (keyword !== undefined && keyword !== null) ? keyword : (document.getElementById('invoice-search-input')?.value || '');
            const url = kw.trim() ? `/api/v1/invoices?keyword=${encodeURIComponent(kw.trim())}` : '/api/v1/invoices';
            const res = await this.apiFetch(url);
            if (res.success && res.data) {
                if (res.data.length === 0) {
                    tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--gray-400); padding: 2rem;"><i class="fa-solid fa-receipt"></i> Không tìm thấy hóa đơn nào${kw ? ' phù hợp với từ khóa "' + kw + '"' : ''}.</td></tr>`;
                    return;
                }

                tbody.innerHTML = res.data.map(inv => {
                    let badge = '<span class="status-badge status-unpaid">Chưa thanh toán</span>';
                    if (inv.status === 'PAID') badge = '<span class="status-badge status-paid">Đã thanh toán</span>';
                    else if (inv.status === 'CANCELLED') badge = '<span class="status-badge status-cancelled"><i class="fa-solid fa-ban"></i> Đã hủy</span>';

                    let actions = '';
                    if (inv.status === 'CANCELLED') {
                        actions = '<button class="btn-secondary" style="padding: 0.25rem 0.5rem; font-size: 0.75rem; opacity: 0.5; cursor: not-allowed;" disabled title="Hóa đơn đã hủy, không thể in"><i class="fa-solid fa-ban"></i> Không thể in</button>';
                    } else {
                        actions = `<button class="btn-secondary" style="padding: 0.25rem 0.5rem; font-size: 0.75rem;" onclick="app.openK80Modal(${JSON.stringify(inv).replace(/"/g, '&quot;')})"><i class="fa-solid fa-print"></i> In HĐ</button>`;
                        if (inv.status === 'UNPAID') {
                            actions += ` <button class="btn-success" style="padding: 0.25rem 0.5rem; font-size: 0.75rem;" onclick="app.confirmPaymentById(${inv.invoiceId})">Thanh toán</button>`;
                            actions += ` <button class="btn-danger" style="padding: 0.25rem 0.5rem; font-size: 0.75rem;" onclick="app.cancelInvoiceById(${inv.invoiceId})">Hủy</button>`;
                        }
                    }

                    return `
                        <tr>
                            <td><strong>${inv.invoiceCode}</strong></td>
                            <td><strong>${inv.customerName || 'Khách vãng lai'}</strong></td>
                            <td>${inv.salesStaffName || '-'}</td>
                            <td>${inv.warehouseName || '-'}</td>
                            <td>${app.formatDateTime(inv.createdAt)}</td>
                            <td style="text-align: right; font-weight: bold; color: var(--primary);">${app.formatCurrency(inv.totalAmount)} đ</td>
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

                // A4 report sync
                document.getElementById('a4-val-import').textContent = this.formatCurrency(k.totalImportAmount) + ' đ';
                document.getElementById('a4-val-revenue').textContent = this.formatCurrency(k.totalRevenue) + ' đ';
                document.getElementById('a4-val-profit').textContent = this.formatCurrency(k.grossProfit) + ' đ';
                document.getElementById('a4-report-title').textContent = 'BÁO CÁO DOANH THU & TỒN KHO - ' + k.filterPeriod.toUpperCase();
            }

            // Load Invoices Table in period
            const resInv = await this.apiFetch('/api/v1/reports/invoices?' + params.toString());
            if (resInv.success && resInv.data) {
                const tbody = document.getElementById('report-invoices-body');
                const a4body = document.getElementById('a4-invoices-body');

                tbody.innerHTML = resInv.data.map(inv => {
                    let badge = '<span class="status-badge status-paid"><i class="fa-solid fa-check"></i> Đã thanh toán</span>';
                    if (inv.status === 'CANCELLED') {
                        badge = '<span class="status-badge status-cancelled"><i class="fa-solid fa-ban"></i> Đã hủy</span>';
                    } else if (inv.status === 'UNPAID') {
                        badge = '<span class="status-badge status-unpaid"><i class="fa-solid fa-clock"></i> Chưa thanh toán</span>';
                    }

                    return `
                    <tr>
                        <td><strong>${inv.invoiceCode}</strong></td>
                        <td>${inv.customerName}</td>
                        <td>${inv.salesStaffName}</td>
                        <td>${app.formatDateTime(inv.createdAt)}</td>
                        <td style="text-align: right; font-weight: bold;">${app.formatCurrency(inv.totalAmount)} đ</td>
                        <td style="text-align: center;">${badge}</td>
                        <td style="text-align: center;">
                            ${inv.status === 'CANCELLED' 
                                ? '<button class="btn-secondary" style="padding: 0.25rem 0.5rem; font-size: 0.75rem; opacity: 0.5; cursor: not-allowed;" disabled title="Hóa đơn đã hủy, không thể in"><i class="fa-solid fa-ban"></i> Không thể in</button>'
                                : `<button class="btn-secondary" style="padding: 0.25rem 0.5rem; font-size: 0.75rem;" onclick="app.openK80Modal(${JSON.stringify(inv).replace(/"/g, '&quot;')})"><i class="fa-solid fa-print"></i> In HĐ</button>`}
                        </td>
                    </tr>
                    `;
                }).join('');

                // Chỉ in các hóa đơn có trạng thái ĐÃ HOÀN THÀNH (PAID) vào báo cáo
                const completedInvoices = resInv.data.filter(inv => inv.status === 'PAID');
                if (completedInvoices.length === 0) {
                    a4body.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--gray-500); padding: 1.5rem;">Không có hóa đơn hoàn thành nào trong kỳ này.</td></tr>';
                } else {
                    const totalCompleted = completedInvoices.reduce((sum, inv) => sum + (inv.totalAmount || 0), 0);
                    a4body.innerHTML = completedInvoices.map(inv => `
                    <tr>
                        <td style="text-align: center;"><strong>${inv.invoiceCode}</strong></td>
                        <td>${inv.customerName || 'Khách vãng lai'}</td>
                        <td>${inv.salesStaffName || '-'}</td>
                        <td style="text-align: center;">${app.formatDateTime(inv.createdAt)}</td>
                        <td style="text-align: right; font-weight: 600;">${app.formatCurrency(inv.totalAmount)} đ</td>
                        <td style="text-align: center; color: #15803d; font-weight: 600;">Đã hoàn thành</td>
                    </tr>
                    `).join('') + `
                    <tr style="background: #f8fafc; font-weight: bold; border-top: 2px solid #cbd5e1;">
                        <td colspan="4" style="text-align: right;">TỔNG DOANH THU HOÀN THÀNH (${completedInvoices.length} HÓA ĐƠN):</td>
                        <td style="text-align: right; color: #15803d; font-size: 1rem;">${app.formatCurrency(totalCompleted)} đ</td>
                        <td style="text-align: center; color: #15803d;">HOÀN TẤT</td>
                    </tr>
                    `;
                }
            }

            // Load Imports & Disposals Tables in period
            this.loadImportSlipsList();
            this.loadDisposalSlipsList();

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
        document.querySelectorAll('#rep-tab-invoices, #rep-tab-imports, #rep-tab-disposals, #rep-tab-stock').forEach(el => el.style.display = 'none');
        document.querySelectorAll('#tab-btn-rep-inv, #tab-btn-rep-imp, #tab-btn-rep-disp, #tab-btn-rep-stock').forEach(el => el.classList.remove('active'));

        const target = document.getElementById('rep-tab-' + tabName);
        if (target) target.style.display = 'block';

        if (tabName === 'invoices') document.getElementById('tab-btn-rep-inv')?.classList.add('active');
        if (tabName === 'imports') {
            document.getElementById('tab-btn-rep-imp')?.classList.add('active');
            this.loadImportSlipsList();
        }
        if (tabName === 'disposals') {
            document.getElementById('tab-btn-rep-disp')?.classList.add('active');
            this.loadDisposalSlipsList();
        }
        if (tabName === 'stock') document.getElementById('tab-btn-rep-stock')?.classList.add('active');
    },

    async loadImportSlipsList() {
        const tbody1 = document.getElementById('import-slips-list-tbody');
        const tbody2 = document.getElementById('report-imports-body');
        if (!tbody1 && !tbody2) return;

        try {
            const res = await this.apiFetch('/api/v1/import-slips');
            if (res.success && res.data) {
                const isAdmin = this.state.user && this.state.user.position === 'ROLE_ADMIN';
                const rowsHtml = res.data.length === 0
                    ? '<tr><td colspan="10" style="text-align: center; color: var(--gray-500); padding: 1.5rem;">Chưa có phiếu nhập hàng nào.</td></tr>'
                    : res.data.map(imp => {
                        let badge = '<span class="status-badge status-unpaid"><i class="fa-solid fa-clock"></i> Chờ xác nhận</span>';
                        if (imp.status === 'IMPORTED') badge = '<span class="status-badge status-paid"><i class="fa-solid fa-check"></i> Đã nhập</span>';
                        else if (imp.status === 'CANCELLED') badge = '<span class="status-badge status-cancelled"><i class="fa-solid fa-ban"></i> Đã hủy</span>';

                        let itemDesc = '-';
                        let itemQty = '-';
                        let prodLabel = '';
                        if (imp.items && imp.items.length > 0) {
                            if (imp.items.length === 1) {
                                const it = imp.items[0];
                                itemDesc = `<strong>${it.productName}</strong><br><small style="color: var(--gray-600);">Lô: ${it.batchCode || '-'}</small>`;
                                itemQty = `<span style="font-weight: 700; color: #0284c7;">${it.quantity}</span> ${it.unit || ''}`;
                                prodLabel = `${it.productName} (${it.quantity} ${it.unit || ''})`;
                            } else {
                                itemDesc = `<strong>${imp.items.length} lô thuốc:</strong><div style="font-size: 0.76rem; color: var(--gray-600); margin-top: 2px; line-height: 1.3;">` +
                                    imp.items.slice(0, 3).map(it => `• ${it.productName} (${it.batchCode})`).join('<br>') +
                                    (imp.items.length > 3 ? `<br><em>...và ${imp.items.length - 3} lô khác</em>` : '') +
                                    `</div>`;
                                const totalQty = imp.items.reduce((s, it) => s + it.quantity, 0);
                                itemQty = `<span style="font-weight: 700; color: #0284c7;">${totalQty}</span> <small style="color: var(--gray-500);">(Tổng)</small>`;
                                prodLabel = `${imp.items.length} lô hàng (Tổng ${totalQty} sp)`;
                            }
                        }
                        const escapedLabel = prodLabel.replace(/"/g, '&quot;').replace(/'/g, "\\'");

                        let actions = '<span style="color: var(--gray-400); font-size: 0.82rem;">-</span>';
                        if (isAdmin && imp.status === 'PENDING') {
                            actions = `
                                <div style="display: flex; gap: 0.35rem; justify-content: center;">
                                    <button class="btn-success" style="padding: 0.25rem 0.55rem; font-size: 0.78rem; white-space: nowrap;" onclick="app.approveImportSlip(${imp.importSlipId}, '${escapedLabel}')"><i class="fa-solid fa-check"></i> Xác nhận (Duyệt)</button>
                                    <button class="btn-danger" style="padding: 0.25rem 0.55rem; font-size: 0.78rem; white-space: nowrap;" onclick="app.rejectImportSlip(${imp.importSlipId}, '${escapedLabel}')"><i class="fa-solid fa-xmark"></i> Từ chối</button>
                                </div>
                            `;
                        } else if (imp.status === 'IMPORTED') {
                            actions = '<span style="color: #15803d; font-size: 0.82rem; font-weight: 600;"><i class="fa-solid fa-check"></i> Đã duyệt</span>';
                        } else if (imp.status === 'CANCELLED') {
                            actions = '<span style="color: #b91c1c; font-size: 0.82rem; font-weight: 600;"><i class="fa-solid fa-ban"></i> Đã từ chối</span>';
                        }

                        return `
                            <tr>
                                <td><strong>#PN${String(imp.importSlipId).padStart(4, '0')}</strong></td>
                                <td>${imp.supplierName || '-'}</td>
                                <td>${imp.warehouseStaffName || '-'}</td>
                                <td>${imp.warehouseName || '-'}</td>
                                <td>${itemDesc}</td>
                                <td style="text-align: right;">${itemQty}</td>
                                <td style="text-align: right; font-weight: 700; color: var(--primary);">${this.formatCurrency(imp.totalAmount)} đ</td>
                                <td>${this.formatDateTime(imp.importDate)}</td>
                                <td style="text-align: center;">${badge}</td>
                                <td style="text-align: center;">${actions}</td>
                            </tr>
                        `;
                    }).join('');

                if (tbody1) tbody1.innerHTML = rowsHtml;
                if (tbody2) tbody2.innerHTML = rowsHtml;
            }
        } catch (e) {
            console.error('Error loading import slips:', e);
            if (tbody1) tbody1.innerHTML = '<tr><td colspan="10" style="text-align: center; color: var(--danger);">Lỗi: ' + e.message + '</td></tr>';
            if (tbody2) tbody2.innerHTML = '<tr><td colspan="10" style="text-align: center; color: var(--danger);">Lỗi: ' + e.message + '</td></tr>';
        }
    },

    async approveImportSlip(id, prodLabel = '') {
        const detailInfo = prodLabel ? ` [${prodLabel}]` : '';
        if (!confirm(`Bạn có chắc muốn PHÊ DUYỆT phiếu nhập #${id}${detailInfo}? (Hàng hóa sẽ được nhập và cộng vào tồn kho).`)) return;
        try {
            const res = await this.apiFetch(`/api/v1/import-slips/${id}/confirm`, { method: 'POST' });
            if (res.success) {
                this.showCenterToast('✅ Đã phê duyệt phiếu nhập thành công (Đã cộng kho)', 'green', 2500);
                this.loadImportSlipsList();
                if (document.getElementById('view-reports')?.classList.contains('active')) {
                    this.loadReportDashboard();
                }
            } else {
                alert('Lỗi phê duyệt phiếu nhập: ' + res.message);
            }
        } catch (e) {
            alert('Lỗi phê duyệt: ' + e.message);
        }
    },

    async rejectImportSlip(id, prodLabel = '') {
        const detailInfo = prodLabel ? ` [${prodLabel}]` : '';
        if (!confirm(`Từ chối / Hủy phiếu nhập #${id}${detailInfo}?`)) return;
        try {
            const res = await this.apiFetch(`/api/v1/import-slips/${id}/cancel`, { method: 'POST' });
            if (res.success) {
                this.showCenterToast('✅ Đã từ chối phiếu nhập #' + id, 'green', 2000);
                this.loadImportSlipsList();
                if (document.getElementById('view-reports')?.classList.contains('active')) {
                    this.loadReportDashboard();
                }
            } else {
                alert('Lỗi từ chối phiếu nhập: ' + res.message);
            }
        } catch (e) {
            alert('Lỗi: ' + e.message);
        }
    },

    // ================= MODAL: SELECT MULTIPLE DISPOSAL BATCHES =================
    async openSelectDisposalBatchesModal() {
        const selectDis = document.getElementById('disposal-warehouse-select');
        const whId = (selectDis && selectDis.value) 
            ? selectDis.value 
            : (this.state.user?.warehouseId || this.state.activeWarehouseId || '1');

        const whNameEl = document.getElementById('modal-disposal-warehouse-name');
        if (whNameEl && selectDis && selectDis.options && selectDis.selectedIndex >= 0) {
            whNameEl.textContent = selectDis.options[selectDis.selectedIndex].text;
        }

        const tbody = document.getElementById('modal-disposal-batches-tbody');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 2rem;"><i class="fa-solid fa-spinner fa-spin"></i> Đang tải danh sách tồn kho...</td></tr>';
        }

        this.openModal('modal-select-disposal-batches');
        const filterInput = document.getElementById('modal-disposal-search-filter');
        if (filterInput) filterInput.value = '';

        try {
            const res = await this.apiFetch(`/api/v1/batches/search-disposal?keyword=&warehouseId=${whId}`);
            if (res.success && res.data) {
                this.state.modalDisposalBatchesCache = res.data;
                this.renderDisposalBatchesModalTable(res.data);
            } else {
                if (tbody) tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--gray-500); padding: 1.5rem;">Kho hiện không có lô hàng nào khả dụng.</td></tr>';
            }
        } catch (e) {
            if (tbody) tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--danger); padding: 1.5rem;">Lỗi tải dữ liệu: ' + e.message + '</td></tr>';
        }
    },

    renderDisposalBatchesModalTable(batches) {
        const tbody = document.getElementById('modal-disposal-batches-tbody');
        if (!tbody) return;

        if (!batches || batches.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--gray-500); padding: 1.5rem;">Không tìm thấy lô hàng nào phù hợp trong kho.</td></tr>';
            this.updateDisposalModalSelectedCount();
            return;
        }

        const existingBatchIds = new Set((this.state.disposalSlipItems || []).map(it => it.batchId));

        tbody.innerHTML = batches.map(b => {
            const isAlreadyAdded = existingBatchIds.has(b.batchId);
            let badgeBg = '#fee2e2';
            let badgeColor = '#dc2626';
            if (b.status === 'Gần hết hạn') {
                badgeBg = '#fef3c7';
                badgeColor = '#d97706';
            } else if (b.status === 'Còn hạn') {
                badgeBg = '#dcfce7';
                badgeColor = '#16a34a';
            }

            return `
                <tr style="background: ${isAlreadyAdded ? '#f8fafc' : 'white'};">
                    <td style="text-align: center;">
                        <input type="checkbox" class="modal-disp-item-checkbox" 
                               value="${b.batchId}" 
                               data-batch='${JSON.stringify(b).replace(/'/g, "&#39;")}'
                               ${isAlreadyAdded ? 'checked disabled' : ''} 
                               onchange="app.updateDisposalModalSelectedCount()">
                    </td>
                    <td>
                        <strong style="color: #1e293b; font-size: 0.9rem;">${b.productName}</strong>
                        <div style="font-size: 0.75rem; color: var(--gray-500);">ĐVT: <strong>${b.unit || 'Đơn vị'}</strong></div>
                    </td>
                    <td>
                        <span style="font-family: monospace; font-weight: 700; color: #b45309;">${b.batchCode}</span>
                    </td>
                    <td>
                        <div style="font-size: 0.85rem; font-weight: 600;">${b.expDate}</div>
                        <div style="font-size: 0.72rem; color: var(--gray-500);">NSX: ${b.mfgDate}</div>
                    </td>
                    <td style="text-align: center;">
                        <span style="font-size: 0.72rem; font-weight: 700; padding: 2px 8px; border-radius: 9999px; background: ${badgeBg}; color: ${badgeColor}; white-space: nowrap;">
                            ${b.status}
                        </span>
                        ${isAlreadyAdded ? '<br><small style="color: #2563eb; font-weight: 600;">(Đã có trong bảng)</small>' : ''}
                    </td>
                    <td style="text-align: right; font-weight: 700; color: #dc2626; font-size: 0.92rem;">
                        ${b.quantity} ${b.unit || ''}
                    </td>
                </tr>
            `;
        }).join('');

        this.updateDisposalModalSelectedCount();
    },

    filterDisposalBatchesModal(q = '') {
        const batches = this.state.modalDisposalBatchesCache || [];
        const term = q.trim().toLowerCase();
        if (!term) {
            this.renderDisposalBatchesModalTable(batches);
            return;
        }
        const filtered = batches.filter(b => 
            (b.batchCode && b.batchCode.toLowerCase().includes(term)) ||
            (b.productName && b.productName.toLowerCase().includes(term))
        );
        this.renderDisposalBatchesModalTable(filtered);
    },

    toggleAllDisposalModalBatches(checked) {
        document.querySelectorAll('.modal-disp-item-checkbox:not(:disabled)').forEach(cb => {
            cb.checked = checked;
        });
        this.updateDisposalModalSelectedCount();
    },

    updateDisposalModalSelectedCount() {
        const count = document.querySelectorAll('.modal-disp-item-checkbox:checked:not(:disabled)').length;
        const countEl = document.getElementById('modal-disposal-selected-count');
        if (countEl) countEl.textContent = count;
    },

    confirmAddModalDisposalBatches() {
        const checkboxes = document.querySelectorAll('.modal-disp-item-checkbox:checked:not(:disabled)');
        if (checkboxes.length === 0) {
            alert('Vui lòng tích chọn ít nhất 1 lô thuốc cần tiêu hủy!');
            return;
        }

        let addedCount = 0;
        checkboxes.forEach(cb => {
            try {
                const b = JSON.parse(cb.getAttribute('data-batch'));
                if (b) {
                    this.addDisposalItem(b, false);
                    addedCount++;
                }
            } catch (err) {
                console.error('Parse batch error', err);
            }
        });

        this.renderDisposalItemsTable();
        this.calcDisposalTotal();
        this.closeModal('modal-select-disposal-batches');
        this.showCenterToast(`✅ Đã thêm ${addedCount} lô thuốc vào danh sách hủy!`, 'green', 2200);
    },

    async loadDisposalSlipsList() {
        const tbody1 = document.getElementById('disposal-slips-list-tbody');
        const tbody2 = document.getElementById('report-disposals-body');
        if (!tbody1 && !tbody2) return;

        try {
            const res = await this.apiFetch('/api/v1/disposal-slips');
            if (res.success && res.data) {
                const isAdmin = this.state.user && this.state.user.position === 'ROLE_ADMIN';
                const rowsHtml = res.data.length === 0
                    ? '<tr><td colspan="9" style="text-align: center; color: var(--gray-500); padding: 1.5rem;">Chưa có phiếu hủy hàng nào.</td></tr>'
                    : res.data.map(d => {
                        let badge = '<span class="status-badge status-unpaid"><i class="fa-solid fa-clock"></i> Chờ xác nhận</span>';
                        if (d.status === 'DISPOSED') badge = '<span class="status-badge status-paid"><i class="fa-solid fa-check"></i> Đã hủy</span>';
                        else if (d.status === 'CANCELLED') badge = '<span class="status-badge status-cancelled"><i class="fa-solid fa-ban"></i> Đã từ chối</span>';

                        let itemDesc = '-';
                        let itemQty = '-';
                        let itemReason = '-';
                        let prodLabel = '';
                        if (d.items && d.items.length > 0) {
                            if (d.items.length === 1) {
                                const it = d.items[0];
                                itemDesc = `<strong>${it.productName}</strong><br><small style="color: var(--gray-600);">Lô: ${it.batchCode || '-'}</small>`;
                                itemQty = `<span style="font-weight: 700; color: #b45309;">${it.quantity}</span> ${it.unit || ''}`;
                                itemReason = it.reason || '-';
                                prodLabel = `${it.productName} (${it.quantity} ${it.unit || ''})`;
                            } else {
                                itemDesc = `<strong>${d.items.length} lô thuốc:</strong><div style="font-size: 0.76rem; color: var(--gray-600); margin-top: 2px; line-height: 1.3;">` +
                                    d.items.slice(0, 3).map(it => `• ${it.productName} (Lô: ${it.batchCode})`).join('<br>') +
                                    (d.items.length > 3 ? `<br><em>...và ${d.items.length - 3} lô khác</em>` : '') +
                                    `</div>`;
                                const totalQty = d.items.reduce((s, it) => s + it.quantity, 0);
                                itemQty = `<span style="font-weight: 700; color: #b45309;">${totalQty}</span> <small style="color: var(--gray-500);">(Tổng)</small>`;
                                itemReason = d.items.map(it => it.reason).filter(Boolean).join('; ');
                                prodLabel = `${d.items.length} lô thuốc (Tổng ${totalQty} sp)`;
                            }
                        }
                        const escapedLabel = prodLabel.replace(/"/g, '&quot;').replace(/'/g, "\\'");

                        let actions = '<span style="color: var(--gray-400); font-size: 0.82rem;">-</span>';
                        if (isAdmin && d.status === 'PENDING') {
                            actions = `
                                <div style="display: flex; gap: 0.35rem; justify-content: center;">
                                    <button class="btn-success" style="padding: 0.25rem 0.55rem; font-size: 0.78rem; white-space: nowrap;" onclick="app.approveDisposalSlip(${d.disposalSlipId}, '${escapedLabel}')"><i class="fa-solid fa-check"></i> Xác nhận (Duyệt)</button>
                                    <button class="btn-danger" style="padding: 0.25rem 0.55rem; font-size: 0.78rem; white-space: nowrap;" onclick="app.rejectDisposalSlip(${d.disposalSlipId}, '${escapedLabel}')"><i class="fa-solid fa-xmark"></i> Từ chối</button>
                                </div>
                            `;
                        } else if (d.status === 'DISPOSED') {
                            actions = '<span style="color: #15803d; font-size: 0.82rem; font-weight: 600;"><i class="fa-solid fa-check"></i> Đã duyệt</span>';
                        } else if (d.status === 'CANCELLED') {
                            actions = '<span style="color: #b91c1c; font-size: 0.82rem; font-weight: 600;"><i class="fa-solid fa-ban"></i> Đã từ chối</span>';
                        }

                        return `
                            <tr>
                                <td><strong>#PH${String(d.disposalSlipId).padStart(4, '0')}</strong></td>
                                <td>${d.warehouseStaffName || '-'}</td>
                                <td>${d.warehouseName || '-'}</td>
                                <td>${itemDesc}</td>
                                <td style="text-align: right;">${itemQty}</td>
                                <td style="max-width: 200px; white-space: normal; font-size: 0.85rem;">${itemReason}</td>
                                <td>${this.formatDateTime(d.disposalDate)}</td>
                                <td style="text-align: center;">${badge}</td>
                                <td style="text-align: center;">${actions}</td>
                            </tr>
                        `;
                    }).join('');

                if (tbody1) tbody1.innerHTML = rowsHtml;
                if (tbody2) tbody2.innerHTML = rowsHtml;
            }
        } catch (e) {
            console.error('Error loading disposal slips:', e);
            if (tbody1) tbody1.innerHTML = '<tr><td colspan="9" style="text-align: center; color: var(--danger);">Lỗi: ' + e.message + '</td></tr>';
            if (tbody2) tbody2.innerHTML = '<tr><td colspan="9" style="text-align: center; color: var(--danger);">Lỗi: ' + e.message + '</td></tr>';
        }
    },

    async approveDisposalSlip(id, prodLabel = '') {
        const detailInfo = prodLabel ? ` [${prodLabel}]` : '';
        if (!confirm(`Bạn có chắc muốn PHÊ DUYỆT phiếu hủy #${id}${detailInfo}? (Hàng hóa sẽ bị trừ khỏi tồn kho).`)) return;
        try {
            const res = await this.apiFetch(`/api/v1/disposal-slips/${id}/confirm`, { method: 'POST' });
            if (res.success) {
                this.showCenterToast('✅ Đã phê duyệt phiếu hủy thành công (Đã trừ kho)', 'green', 2500);
                this.loadDisposalSlipsList();
                if (document.getElementById('view-reports')?.classList.contains('active')) {
                    this.loadReportDashboard();
                }
            } else {
                alert('Lỗi phê duyệt phiếu hủy: ' + res.message);
            }
        } catch (e) {
            alert('Lỗi phê duyệt: ' + e.message);
        }
    },

    async rejectDisposalSlip(id, prodLabel = '') {
        const detailInfo = prodLabel ? ` [${prodLabel}]` : '';
        if (!confirm(`Từ chối phiếu hủy #${id}${detailInfo}?`)) return;
        try {
            const res = await this.apiFetch(`/api/v1/disposal-slips/${id}/cancel`, { method: 'POST' });
            if (res.success) {
                this.showCenterToast('✅ Đã từ chối phiếu hủy #' + id, 'green', 2000);
                this.loadDisposalSlipsList();
                if (document.getElementById('view-reports')?.classList.contains('active')) {
                    this.loadReportDashboard();
                }
            } else {
                alert('Lỗi: ' + res.message);
            }
        } catch (e) {
            alert('Lỗi: ' + e.message);
        }
    },

    printA4Report() {
        const now = new Date();
        const dEl = document.getElementById('a4-sign-date');
        if (dEl) {
            dEl.innerHTML = `Hà Nội, Ngày ${String(now.getDate()).padStart(2, '0')} tháng ${String(now.getMonth() + 1).padStart(2, '0')} năm ${now.getFullYear()}<br>Chủ cửa hàng / Giám đốc`;
        }
        document.body.classList.add('printing-a4');
        window.print();
        setTimeout(() => {
            document.body.classList.remove('printing-a4');
        }, 1000);
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
                } else {
                    container.innerHTML = `<div style="color: var(--danger); padding: 1.5rem; text-align: center;">
                        <i class="fa-solid fa-triangle-exclamation"></i> Không thể tải danh sách nhân viên: ${res?.message || 'Bạn không có quyền truy cập hoặc phiên làm việc đã hết hạn.'}
                    </div>`;
                }
            } else if (tabName === 'products') {
                const res = await this.apiFetch('/api/v1/products?size=100');
                if (res.success && res.data) {
                    container.innerHTML = `
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Tên thuốc</th>
                                    <th>Nhóm</th>
                                    <th>ĐVT</th>
                                    <th>Giá bán hiện tại</th>
                                    <th>Tồn kho</th>
                                    <th style="text-align: center;">Trạng thái</th>
                                    <th style="text-align: center;">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${res.data.content.map(p => {
                                    const st = p.totalStock != null ? p.totalStock : 0;
                                    const out = st <= 0;
                                    return `
                                    <tr>
                                        <td><strong>#${p.productId}</strong></td>
                                        <td><strong>${p.productName}</strong></td>
                                        <td>${p.categoryName || '-'}</td>
                                        <td>${p.unit}</td>
                                        <td style="font-weight: bold; color: #e11d48;">${app.formatCurrency(p.sellingPrice)} đ</td>
                                        <td style="font-weight: bold; color: ${out ? 'var(--danger)' : 'var(--success-dark)'};">${st}</td>
                                        <td style="text-align: center;">
                                            <span class="status-badge ${out ? 'status-cancelled' : 'status-paid'}">
                                                ${out ? 'Hết hàng' : 'Còn hàng'}
                                            </span>
                                        </td>
                                        <td style="text-align: center; white-space: nowrap;">
                                            <button class="btn-secondary" style="padding: 0.25rem 0.5rem; font-size: 0.78rem; margin-right: 0.25rem;" onclick="app.editProduct(${JSON.stringify(p).replace(/"/g, '&quot;')})">
                                                <i class="fa-solid fa-pen-to-square"></i> Sửa
                                            </button>
                                            <button class="btn-danger" style="padding: 0.25rem 0.5rem; font-size: 0.78rem;" onclick="app.deleteProduct(${p.productId})">
                                                <i class="fa-solid fa-trash"></i> Xóa
                                            </button>
                                        </td>
                                    </tr>
                                    `;
                                }).join('')}
                            </tbody>
                        </table>
                    `;
                }
            } else if (tabName === 'warehouses') {
                const res = await this.apiFetch('/api/v1/warehouses');
                if (res.success) {
                    container.innerHTML = `
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Tên kho</th>
                                    <th>Địa chỉ</th>
                                    <th style="text-align: center;">Trạng thái</th>
                                    <th style="text-align: center;">Đổi trạng thái</th>
                                    <th style="text-align: center;">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${res.data.map(w => `
                                    <tr>
                                        <td><strong>#${w.warehouseId}</strong></td>
                                        <td><strong>${w.warehouseName}</strong></td>
                                        <td>${w.warehouseAddress}</td>
                                        <td style="text-align: center;"><span class="status-badge ${w.status === 'ACTIVE' ? 'status-active' : 'status-inactive'}">${w.status}</span></td>
                                        <td style="text-align: center;">
                                            <button class="btn-secondary" style="padding: 0.2rem 0.55rem; font-size: 0.75rem;" onclick="app.toggleWarehouseStatus(${w.warehouseId}, '${w.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'}')">
                                                <i class="fa-solid fa-arrows-rotate"></i> Chuyển sang ${w.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'}
                                            </button>
                                        </td>
                                        <td style="text-align: center; white-space: nowrap;">
                                            <button class="btn-secondary" style="padding: 0.2rem 0.5rem; font-size: 0.75rem; margin-right: 0.25rem;" onclick="app.editWarehouse(${JSON.stringify(w).replace(/"/g, '&quot;')})">
                                                <i class="fa-solid fa-pen-to-square"></i> Sửa
                                            </button>
                                            <button class="btn-danger" style="padding: 0.2rem 0.5rem; font-size: 0.75rem;" onclick="app.deleteWarehouse(${w.warehouseId})">
                                                <i class="fa-solid fa-trash"></i> Xóa
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
                if (res && res.success && Array.isArray(res.data)) {
                    this.state.employeesCache = res.data;
                    container.innerHTML = `
                        <div style="margin-bottom: 0.75rem; font-size: 0.88rem; color: var(--gray-600);">
                            <i class="fa-solid fa-users-gear"></i> Tổng cộng <strong>${res.data.length}</strong> nhân sự trong hệ thống.
                        </div>
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Mã NV</th>
                                    <th>Tài khoản</th>
                                    <th>Họ và tên</th>
                                    <th>Ngày sinh & Tuổi</th>
                                    <th>Giới tính</th>
                                    <th>SĐT & Địa chỉ</th>
                                    <th>Chức vụ</th>
                                    <th>Kho phụ trách</th>
                                    <th>Trạng thái</th>
                                    <th style="text-align: center;">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${res.data.map(e => `
                                    <tr>
                                        <td><strong>#${e.employeeId}</strong></td>
                                        <td><code>${e.username}</code></td>
                                        <td><strong>${e.fullName}</strong></td>
                                        <td>${this.formatDate(e.dateOfBirth)} <span style="color: var(--primary); font-size: 0.82rem; font-weight: 600;">(${e.age != null ? e.age : '-'} tuổi)</span></td>
                                        <td>${e.gender || '-'}</td>
                                        <td>
                                            <div><i class="fa-solid fa-phone" style="font-size: 0.75rem;"></i> ${e.phone || '-'}</div>
                                            <div style="font-size: 0.78rem; color: var(--gray-500);">${e.address || ''}</div>
                                        </td>
                                        <td>
                                            <span class="badge-pill ${e.position === 'ROLE_ADMIN' ? 'badge-role' : (e.position === 'ROLE_SALES' ? 'badge-warehouse' : 'badge-balance')}">
                                                ${e.position === 'ROLE_ADMIN' ? '👑 Quản trị viên' : (e.position === 'ROLE_SALES' ? '🛒 Bán hàng' : '📦 Thủ kho')}
                                            </span>
                                        </td>
                                        <td>${e.warehouseName || '-'}</td>
                                        <td>
                                            <span class="status-badge ${e.workStatus === 'ACTIVE' ? 'status-active' : 'status-inactive'}">
                                                ${e.workStatus === 'ACTIVE' ? 'Đang làm việc' : 'Đã nghỉ việc'}
                                            </span>
                                        </td>
                                        <td style="text-align: center; white-space: nowrap;">
                                            <button class="btn-secondary" style="padding: 0.25rem 0.5rem; font-size: 0.8rem; margin-right: 0.25rem;" onclick="app.editEmployee(${e.employeeId})" title="Chỉnh sửa thông tin">
                                                <i class="fa-solid fa-pen-to-square"></i> Sửa
                                            </button>
                                            <button class="btn-secondary" style="padding: 0.25rem 0.5rem; font-size: 0.8rem; color: ${e.workStatus === 'ACTIVE' ? 'var(--danger)' : 'var(--success-dark)'};" onclick="app.toggleEmployeeStatus(${e.employeeId})" title="${e.workStatus === 'ACTIVE' ? 'Khóa nhân viên' : 'Kích hoạt lại'}">
                                                <i class="fa-solid ${e.workStatus === 'ACTIVE' ? 'fa-user-lock' : 'fa-user-check'}"></i> ${e.workStatus === 'ACTIVE' ? 'Nghỉ việc' : 'Đi làm lại'}
                                            </button>
                                        </td>
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
            if (name === null) return;
            const addr = prompt('Nhập địa chỉ kho:');
            if (addr === null) return;
            if (name.trim() && addr.trim()) {
                this.apiFetch('/api/v1/warehouses', {
                    method: 'POST',
                    body: JSON.stringify({ warehouseName: name.trim(), warehouseAddress: addr.trim(), status: 'ACTIVE' })
                }).then(res => {
                    if (res.success) {
                        this.showCenterToast('✓ Thêm kho mới thành công!', 'green');
                        this.renderMgmtTable('warehouses');
                        this.loadWarehouses();
                    } else alert('Lỗi: ' + res.message);
                });
            } else {
                alert('Tên kho và địa chỉ không được để trống!');
            }
        } else if (tab === 'suppliers') {
            this.openModal('modal-new-supplier');
        } else if (tab === 'employees') {
            document.getElementById('emp-id').value = '';
            document.getElementById('modal-emp-title').textContent = 'Tạo tài khoản nhân viên mới';
            const uField = document.getElementById('emp-username');
            uField.value = '';
            uField.readOnly = false;
            uField.style.background = 'white';
            document.getElementById('emp-password').value = '';
            document.getElementById('emp-password').placeholder = 'Mật khẩu khởi tạo (mặc định: 123456)...';
            document.getElementById('emp-fullname').value = '';
            document.getElementById('emp-dob').value = '1995-01-01';
            document.getElementById('emp-gender').value = 'Nam';
            document.getElementById('emp-phone').value = '';
            const addrField = document.getElementById('emp-address');
            if (addrField) addrField.value = '';
            document.getElementById('emp-position').value = 'ROLE_SALES';
            document.getElementById('emp-status').value = 'ACTIVE';
            const empBalInput = document.getElementById('emp-balance'); if (empBalInput) empBalInput.value = '0';
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
            const res = await this.apiFetch(`/api/v1/warehouses/${wId}/toggle-status`, {
                method: 'PATCH'
            });

            if (res.success) {
                this.showCenterToast(`✓ Đã đổi trạng thái kho sang ${res.data.status}!`, 'green');
                this.renderMgmtTable('warehouses');
                this.loadWarehouses();
            } else {
                alert('Không thể đổi trạng thái kho: ' + res.message);
            }
        } catch (e) {
            alert('Không thể đổi trạng thái kho: ' + e.message);
        }
    },

    editWarehouse(w) {
        const newName = prompt('Nhập tên kho mới:', w.warehouseName);
        if (newName === null) return;
        const newAddr = prompt('Nhập địa chỉ kho mới:', w.warehouseAddress);
        if (newAddr === null) return;
        if (!newName.trim() || !newAddr.trim()) {
            alert('Tên kho và địa chỉ không được để trống!');
            return;
        }

        this.apiFetch('/api/v1/warehouses', {
            method: 'POST',
            body: JSON.stringify({
                warehouseId: w.warehouseId,
                warehouseName: newName.trim(),
                warehouseAddress: newAddr.trim(),
                status: w.status
            })
        }).then(res => {
            if (res.success) {
                this.showCenterToast('✓ Cập nhật thông tin kho thành công!', 'green');
                this.renderMgmtTable('warehouses');
                this.loadWarehouses();
            } else {
                alert('Lỗi cập nhật kho: ' + res.message);
            }
        }).catch(e => alert('Lỗi: ' + e.message));
    },

    async deleteWarehouse(id) {
        if (!confirm('Bạn có chắc chắn muốn xóa kho hàng này? Lưu ý: Không thể xóa kho nếu đã có hóa đơn, phiếu nhập/hủy, nhân viên phụ trách hoặc còn hàng tồn.')) return;

        try {
            const res = await this.apiFetch(`/api/v1/warehouses/${id}`, {
                method: 'DELETE'
            });

            if (res.success) {
                this.showCenterToast('✓ Xóa kho hàng thành công!', 'green');
                this.renderMgmtTable('warehouses');
                this.loadWarehouses();
            } else {
                alert('Không thể xóa kho hàng: ' + res.message);
            }
        } catch (e) {
            alert('Lỗi: ' + e.message);
        }
    },

    editEmployee(empId) {
        const emp = (this.state.employeesCache || []).find(x => x.employeeId === empId);
        if (!emp) return;

        document.getElementById('emp-id').value = emp.employeeId;
        document.getElementById('modal-emp-title').textContent = 'Cập nhật nhân viên: ' + emp.fullName;
        
        const uField = document.getElementById('emp-username');
        uField.value = emp.username;
        uField.readOnly = true;
        uField.style.background = 'var(--gray-100)';

        document.getElementById('emp-password').value = '';
        document.getElementById('emp-password').placeholder = 'Bỏ trống nếu giữ nguyên mật khẩu cũ';

        document.getElementById('emp-fullname').value = emp.fullName || '';
        document.getElementById('emp-position').value = emp.position || 'ROLE_SALES';
        if (emp.warehouseId && document.getElementById('emp-warehouse-select')) {
            document.getElementById('emp-warehouse-select').value = emp.warehouseId;
        }
        document.getElementById('emp-dob').value = emp.dateOfBirth || '1995-01-01';
        document.getElementById('emp-gender').value = emp.gender || 'Nam';
        document.getElementById('emp-phone').value = emp.phone || '';
        const addrField = document.getElementById('emp-address');
        if (addrField) addrField.value = emp.address || '';
        document.getElementById('emp-status').value = emp.workStatus || 'ACTIVE';
        const empBalEdit = document.getElementById('emp-balance'); if (empBalEdit) empBalEdit.value = emp.balance != null ? emp.balance : 0;

        this.openModal('modal-new-employee');
    },

    async handleSaveEmployee(e) {
        e.preventDefault();
        const idVal = document.getElementById('emp-id').value;
        const pwdVal = document.getElementById('emp-password').value.trim();
        const addrField = document.getElementById('emp-address');

        const payload = {
            employeeId: idVal ? parseInt(idVal) : null,
            username: document.getElementById('emp-username').value.trim(),
            fullName: document.getElementById('emp-fullname').value.trim(),
            position: document.getElementById('emp-position').value,
            warehouseId: parseInt(document.getElementById('emp-warehouse-select').value) || 1,
            dateOfBirth: document.getElementById('emp-dob').value,
            gender: document.getElementById('emp-gender').value,
            phone: document.getElementById('emp-phone').value.trim(),
            address: addrField ? addrField.value.trim() : '',
            workStatus: document.getElementById('emp-status').value,
            balance: document.getElementById('emp-balance') ? (parseFloat(document.getElementById('emp-balance').value) || 0) : 0
        };

        if (pwdVal) {
            payload.password = pwdVal;
        } else if (!idVal) {
            payload.password = '123456';
        }

        try {
            const res = await this.apiFetch('/api/v1/employees', {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            if (res.success) {
                this.closeModal('modal-new-employee');
                this.showCenterToast('✅ ' + (idVal ? 'Cập nhật' : 'Tạo mới') + ' nhân viên thành công!', 'green');
                this.renderMgmtTable('employees');
            } else {
                alert('Lỗi: ' + res.message);
            }
        } catch (err) { alert('Lỗi: ' + err.message); }
    },

    async toggleEmployeeStatus(empId) {
        if (!confirm('Bạn có chắc muốn đổi trạng thái làm việc của nhân sự này?')) return;
        try {
            const res = await this.apiFetch(`/api/v1/employees/${empId}/toggle-status`, { method: 'PATCH' });
            if (res.success) {
                this.showCenterToast('✅ ' + res.message, 'green');
                this.renderMgmtTable('employees');
            } else {
                alert('Lỗi: ' + res.message);
            }
        } catch (err) { alert('Lỗi: ' + err.message); }
    },

    // ================= USER PROFILE (TẤT CẢ TÀI KHOẢN) =================
    async openProfileModal() {
        try {
            const res = await this.apiFetch('/api/v1/employees/profile');
            if (res.success && res.data) {
                const p = res.data;
                document.getElementById('prof-emp-id').textContent = '#' + p.employeeId;
                document.getElementById('prof-username').textContent = p.username;
                document.getElementById('prof-position').textContent = p.position;
                document.getElementById('prof-warehouse').textContent = p.warehouseName || 'Chưa gán';
                document.getElementById('prof-start-date').textContent = p.startDate ? this.formatDate(p.startDate) : '-';
                document.getElementById('prof-age').textContent = p.age != null ? p.age + ' tuổi' : '-';

                document.getElementById('prof-fullname').value = p.fullName || '';
                document.getElementById('prof-dob').value = p.dateOfBirth || '';
                document.getElementById('prof-gender').value = p.gender || 'Nam';
                document.getElementById('prof-phone').value = p.phone || '';
                document.getElementById('prof-address').value = p.address || '';

                document.getElementById('prof-current-pwd').value = '';
                document.getElementById('prof-new-pwd').value = '';
                document.getElementById('prof-confirm-pwd').value = '';

                this.openModal('modal-user-profile');
            } else {
                alert('Không thể tải thông tin cá nhân: ' + (res.message || 'Lỗi'));
            }
        } catch (err) {
            alert('Lỗi: ' + err.message);
        }
    },

    calcProfileAgePreview(dobStr) {
        if (!dobStr) return;
        const dob = new Date(dobStr);
        const today = new Date();
        let age = today.getFullYear() - dob.getFullYear();
        const m = today.getMonth() - dob.getMonth();
        if (m < 0 || (m === 0 && today.getDate() < dob.getDate())) {
            age--;
        }
        document.getElementById('prof-age').textContent = (age >= 0 ? age : 0) + ' tuổi (dẫn xuất)';
    },

    async handleUpdateProfile(e) {
        e.preventDefault();
        const fullName = document.getElementById('prof-fullname').value.trim();
        const dob = document.getElementById('prof-dob').value;
        const gender = document.getElementById('prof-gender').value;
        const phone = document.getElementById('prof-phone').value.trim();
        const address = document.getElementById('prof-address').value.trim();

        const curPwd = document.getElementById('prof-current-pwd').value.trim();
        const newPwd = document.getElementById('prof-new-pwd').value.trim();
        const confirmPwd = document.getElementById('prof-confirm-pwd').value.trim();

        const payload = {
            fullName,
            dateOfBirth: dob,
            gender,
            phone,
            address
        };

        if (newPwd) {
            if (newPwd !== confirmPwd) {
                alert('Mật khẩu mới và xác nhận mật khẩu không trùng khớp!');
                return;
            }
            if (newPwd.length < 6) {
                alert('Mật khẩu mới phải có tối thiểu 6 ký tự!');
                return;
            }
            payload.currentPassword = curPwd;
            payload.newPassword = newPwd;
        }

        try {
            const res = await this.apiFetch('/api/v1/employees/profile', {
                method: 'PUT',
                body: JSON.stringify(payload)
            });

            if (res.success) {
                this.closeModal('modal-user-profile');
                this.showCenterToast('✅ Cập nhật thông tin cá nhân thành công!', 'green');
                const headerName = document.getElementById('header-user-name');
                if (headerName) headerName.textContent = res.data.fullName;
                if (this.state.user) this.state.user.fullName = res.data.fullName;
            } else {
                alert('Lỗi cập nhật: ' + res.message);
            }
        } catch (err) {
            alert('Lỗi: ' + err.message);
        }
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

    formatDate(dateInput) {
        if (!dateInput) return '-';
        if (typeof dateInput === 'string') {
            const cleanStr = dateInput.split('T')[0];
            if (cleanStr.includes('-')) {
                const parts = cleanStr.split('-');
                if (parts.length === 3) {
                    return `${parts[2]}/${parts[1]}/${parts[0]}`;
                }
            }
        }
        try {
            const date = (dateInput instanceof Date) ? dateInput : new Date(dateInput);
            if (isNaN(date.getTime())) return String(dateInput);
            const d = String(date.getDate()).padStart(2, '0');
            const m = String(date.getMonth() + 1).padStart(2, '0');
            const y = date.getFullYear();
            return `${d}/${m}/${y}`;
        } catch (_) {
            return String(dateInput);
        }
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

window.app = app;

// Start application when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    app.init();
});

// Global click listener to close import and customer dropdowns
document.addEventListener('click', (e) => {
    const supDropdown = document.getElementById('import-supplier-dropdown');
    const supInput = document.getElementById('import-supplier-input');
    if (supDropdown && !supDropdown.contains(e.target) && e.target !== supInput) {
        supDropdown.style.display = 'none';
    }

    const prodDropdown = document.getElementById('import-product-dropdown');
    const prodInput = document.getElementById('import-product-input');
    if (prodDropdown && !prodDropdown.contains(e.target) && e.target !== prodInput) {
        prodDropdown.style.display = 'none';
    }

    const dispDropdown = document.getElementById('disposal-batch-dropdown');
    const dispInput = document.getElementById('disposal-batch-input');
    if (dispDropdown && !dispDropdown.contains(e.target) && e.target !== dispInput) {
        dispDropdown.style.display = 'none';
    }
});
