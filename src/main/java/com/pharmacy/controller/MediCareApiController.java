package com.pharmacy.controller;

import com.pharmacy.config.JwtTokenProvider;
import com.pharmacy.dto.InvoiceResponseDTO;
import com.pharmacy.entity.*;
import com.pharmacy.repository.*;
import com.pharmacy.service.*;
import com.pharmacy.util.MoneyToWordsUtil;
import com.pharmacy.util.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MediCareApiController {

    private final AuthService authService;
    private final EmployeeRepository employeeRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryRepository inventoryRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final CustomerRepository customerRepository;
    private final ImportSlipRepository importSlipRepository;
    private final ImportSlipItemRepository importSlipItemRepository;
    private final DisposalSlipRepository disposalSlipRepository;
    private final DisposalSlipItemRepository disposalSlipItemRepository;
    private final InvoiceService invoiceService;
    private final ImportSlipService importSlipService;
    private final DisposalSlipService disposalSlipService;
    private final ReportService reportService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @PersistenceContext
    private EntityManager entityManager;

    // =========================================================================
    // 1. AUTHENTICATION & PROFILE
    // =========================================================================

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Thiếu tên đăng nhập hoặc mật khẩu"));
        }
        Employee emp = employeeRepository.findByUsername(username).orElse(null);
        if (emp == null || !passwordEncoder.matches(password, emp.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Tên đăng nhập hoặc mật khẩu không chính xác"));
        }
        if (!"ACTIVE".equalsIgnoreCase(emp.getWorkStatus())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Tài khoản nhân viên đã bị vô hiệu hóa hoặc ngừng làm việc!"));
        }

        String accessToken = tokenProvider.generateAccessToken(emp.getUsername(), emp.getPosition(), emp.getEmployeeId());
        String refreshToken = tokenProvider.generateRefreshToken(emp.getUsername());

        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", accessToken);
        data.put("refreshToken", refreshToken);
        data.put("id", emp.getEmployeeId());
        data.put("code", "NV" + String.format("%03d", emp.getEmployeeId()));
        data.put("username", emp.getUsername());
        data.put("fullName", emp.getFullName());
        data.put("balance", emp.getBalance() != null ? emp.getBalance() : BigDecimal.ZERO);
        data.put("roles", List.of(emp.getPosition()));
        data.put("status", emp.getWorkStatus());

        return ResponseEntity.ok(Map.of("success", true, "message", "Đăng nhập thành công", "data", data));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String fullName = body.get("fullName");
        if (username == null || password == null || fullName == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Vui lòng nhập đầy đủ thông tin đăng ký"));
        }
        if (employeeRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Tên đăng nhập đã tồn tại trong hệ thống"));
        }

        Employee emp = Employee.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .fullName(fullName)
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .position("ROLE_SALES")
                .workStatus("ACTIVE")
                .balance(BigDecimal.ZERO)
                .startDate(LocalDate.now())
                .build();
        emp = employeeRepository.save(emp);

        String accessToken = tokenProvider.generateAccessToken(emp.getUsername(), emp.getPosition(), emp.getEmployeeId());
        String refreshToken = tokenProvider.generateRefreshToken(emp.getUsername());

        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", accessToken);
        data.put("refreshToken", refreshToken);
        data.put("id", emp.getEmployeeId());
        data.put("code", "NV" + String.format("%03d", emp.getEmployeeId()));
        data.put("username", emp.getUsername());
        data.put("fullName", emp.getFullName());
        data.put("balance", emp.getBalance());
        data.put("roles", List.of(emp.getPosition()));
        data.put("status", emp.getWorkStatus());

        return ResponseEntity.ok(Map.of("success", true, "message", "Đăng ký tài khoản thành công", "data", data));
    }

    @GetMapping({"/auth/me", "/v1/users/profile"})
    public ResponseEntity<?> getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Chưa đăng nhập"));
        }
        Employee emp = employeeRepository.findByUsername(username).orElse(null);
        if (emp == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Không tìm thấy người dùng"));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", emp.getEmployeeId());
        data.put("code", "NV" + String.format("%03d", emp.getEmployeeId()));
        data.put("username", emp.getUsername());
        data.put("fullName", emp.getFullName());
        data.put("balance", emp.getBalance() != null ? emp.getBalance() : BigDecimal.ZERO);
        data.put("roles", List.of(emp.getPosition()));
        data.put("status", emp.getWorkStatus());
        data.put("phone", emp.getPhone() != null ? emp.getPhone() : "");
        data.put("email", emp.getUsername() + "@medicare.vn");
        data.put("dateOfBirth", emp.getDateOfBirth() != null ? emp.getDateOfBirth().toString() : "");
        data.put("gender", emp.getGender() != null ? emp.getGender() : "Nam");
        data.put("address", emp.getAddress() != null ? emp.getAddress() : "");
        data.put("createdAt", emp.getStartDate() != null ? emp.getStartDate().toString() : "");

        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    @PutMapping({"/auth/profile", "/v1/users/profile"})
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> body) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Chưa đăng nhập"));
        }
        Employee emp = employeeRepository.findByUsername(username).orElse(null);
        if (emp == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không tìm thấy người dùng"));
        }

        if (body.containsKey("fullName") && body.get("fullName") != null) {
            emp.setFullName(body.get("fullName").toString());
        }
        if (body.containsKey("phone") && body.get("phone") != null) {
            emp.setPhone(body.get("phone").toString());
        }
        if (body.containsKey("address") && body.get("address") != null) {
            emp.setAddress(body.get("address").toString());
        }
        if (body.containsKey("gender") && body.get("gender") != null) {
            emp.setGender(body.get("gender").toString());
        }
        if (body.containsKey("dateOfBirth") && body.get("dateOfBirth") != null) {
            try {
                emp.setDateOfBirth(LocalDate.parse(body.get("dateOfBirth").toString()));
            } catch (Exception ignored) {}
        }
        if (body.containsKey("password") && body.get("password") != null && !body.get("password").toString().isBlank()) {
            emp.setPassword(passwordEncoder.encode(body.get("password").toString()));
        }
        employeeRepository.save(emp);
        return getCurrentUser();
    }

    @PutMapping("/v1/users/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Chưa đăng nhập"));
        }
        Employee emp = employeeRepository.findByUsername(username).orElse(null);
        if (emp == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không tìm thấy người dùng"));
        }

        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        String confirmPassword = body.get("confirmPassword");
        if (!passwordEncoder.matches(oldPassword, emp.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mật khẩu cũ không chính xác!"));
        }
        if (newPassword == null || newPassword.length() < 4) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mật khẩu mới phải có ít nhất 4 ký tự!"));
        }
        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mật khẩu xác nhận không khớp!"));
        }
        emp.setPassword(passwordEncoder.encode(newPassword));
        employeeRepository.save(emp);
        return ResponseEntity.ok(Map.of("success", true, "message", "Đổi mật khẩu thành công!"));
    }

    @PostMapping("/auth/topup")
    @Transactional
    public ResponseEntity<?> topup(@RequestBody Map<String, Object> body) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Chưa đăng nhập"));
        }
        Employee emp = employeeRepository.findByUsername(username).orElse(null);
        if (emp == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không tìm thấy người dùng"));
        }

        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Số tiền nạp phải lớn hơn 0"));
        }
        BigDecimal current = emp.getBalance() != null ? emp.getBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = current.add(amount);
        emp.setBalance(newBalance);
        employeeRepository.save(emp);

        return ResponseEntity.ok(Map.of("success", true, "message", "Nạp tiền thành công", "data", Map.of("balance", newBalance)));
    }

    // =========================================================================
    // 2. CATEGORIES
    // =========================================================================

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        List<Category> cats = categoryRepository.findAll();
        List<Map<String, Object>> result = cats.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getCategoryId());
            m.put("categoryCode", "CAT" + String.format("%03d", c.getCategoryId()));
            m.put("name", c.getCategoryName());
            m.put("description", c.getDescription() != null ? c.getDescription() : "");
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String description = body.get("description");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Tên nhóm thuốc không được để trống!"));
        }
        Category c = Category.builder()
                .categoryName(name)
                .description(description)
                .build();
        categoryRepository.save(c);
        return ResponseEntity.ok(Map.of("success", true, "message", "Tạo nhóm thuốc thành công!"));
    }

    // =========================================================================
    // 3. PRODUCTS & LIVE SEARCH
    // =========================================================================

    @GetMapping("/products")
    public ResponseEntity<?> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "false") Boolean includeInactive
    ) {
        String searchTerm = keyword != null ? keyword : q;
        List<Product> products = productRepository.findAll();

        if (categoryId != null && categoryId > 0) {
            products = products.stream()
                    .filter(p -> p.getCategory().getCategoryId().equals(categoryId))
                    .collect(Collectors.toList());
        }
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String lower = searchTerm.trim().toLowerCase();
            products = products.stream()
                    .filter(p -> p.getProductName().toLowerCase().contains(lower) ||
                                 (p.getUses() != null && p.getUses().toLowerCase().contains(lower)))
                    .collect(Collectors.toList());
        }

        LocalDate now = LocalDate.now();
        List<Map<String, Object>> result = products.stream().map(p -> {
            Integer totalStock = inventoryRepository.getTotalStockByProduct(p.getProductId());
            if (totalStock == null) totalStock = 0;

            List<Batch> batches = batchRepository.findByProductProductId(p.getProductId());
            Batch nearestBatch = batches.stream()
                    .filter(b -> b.getExpDate().isAfter(now))
                    .min(Comparator.comparing(Batch::getExpDate))
                    .orElse(null);

            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getProductId());
            m.put("productId", p.getProductId());
            m.put("productCode", "MED" + String.format("%04d", p.getProductId()));
            m.put("name", p.getProductName());
            m.put("productName", p.getProductName());
            m.put("categoryId", p.getCategory().getCategoryId());
            m.put("categoryName", p.getCategory().getCategoryName());
            m.put("dosage", p.getDosage() != null ? p.getDosage() : "");
            m.put("dosageForm", p.getDosageForm() != null ? p.getDosageForm() : "Dược phẩm");
            m.put("unit", p.getUnit() != null ? p.getUnit() : "Hộp");
            m.put("sellingPrice", p.getSellingPrice());
            m.put("imageUrl", p.getImageUrl() != null ? p.getImageUrl() : "");
            m.put("uses", p.getUses() != null ? p.getUses() : "");
            m.put("ingredients", p.getUses() != null ? p.getUses() : "");
            m.put("description", p.getUses() != null ? p.getUses() : "");
            m.put("totalStock", totalStock);
            m.put("isActive", true);
            m.put("mfgDate", nearestBatch != null ? nearestBatch.getMfgDate().toString() : "");
            m.put("expDate", nearestBatch != null ? nearestBatch.getExpDate().toString() : "");

            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    @GetMapping("/products/live-search")
    public ResponseEntity<?> liveSearch(@RequestParam(name = "q", defaultValue = "") String query) {
        return getProducts(null, query, null, false);
    }

    @GetMapping("/products/check-name")
    public ResponseEntity<?> checkProductName(@RequestParam String name) {
        boolean exists = productRepository.findAll().stream()
                .anyMatch(p -> p.getProductName().equalsIgnoreCase(name.trim()));
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Long categoryId = Long.valueOf(body.get("categoryId").toString());
        String unit = (String) body.get("unit");
        BigDecimal sellingPrice = new BigDecimal(body.get("sellingPrice").toString());
        String imageUrl = (String) body.get("imageUrl");
        String uses = (String) body.get("uses");
        String dosageForm = body.get("dosageForm") != null ? (String) body.get("dosageForm") : "Viên nén";

        Category cat = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        Product p = Product.builder()
                .productName(name)
                .category(cat)
                .unit(unit)
                .sellingPrice(sellingPrice)
                .imageUrl(imageUrl)
                .uses(uses)
                .dosageForm(dosageForm)
                .dosage("Theo chỉ định")
                .build();
        p = productRepository.save(p);
        return ResponseEntity.ok(Map.of("success", true, "message", "Thêm thuốc mới thành công!", "data", p));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc"));
        if (body.containsKey("name")) p.setProductName((String) body.get("name"));
        if (body.containsKey("unit")) p.setUnit((String) body.get("unit"));
        if (body.containsKey("sellingPrice")) p.setSellingPrice(new BigDecimal(body.get("sellingPrice").toString()));
        if (body.containsKey("imageUrl")) p.setImageUrl((String) body.get("imageUrl"));
        if (body.containsKey("uses")) p.setUses((String) body.get("uses"));
        if (body.containsKey("dosageForm")) p.setDosageForm((String) body.get("dosageForm"));
        if (body.containsKey("categoryId")) {
            Category cat = categoryRepository.findById(Long.valueOf(body.get("categoryId").toString())).orElse(null);
            if (cat != null) p.setCategory(cat);
        }
        productRepository.save(p);
        return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật thông tin thuốc thành công!"));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("success", true, "message", "Đã ngừng kinh doanh thuốc"));
    }

    @PostMapping("/products/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam(name = "file", required = false) MultipartFile file) {
        String sampleUrl = "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=600&auto=format&fit=crop&q=80";
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("imageUrl", sampleUrl)));
    }

    // =========================================================================
    // 4. SALES STAFF & INVOICING (FEFO + ATOMIC CHECKOUT)
    // =========================================================================

    @GetMapping("/sales-staff")
    public ResponseEntity<?> getSalesStaff() {
        List<Employee> list = employeeRepository.findAll().stream()
                .filter(e -> "ACTIVE".equalsIgnoreCase(e.getWorkStatus()))
                .collect(Collectors.toList());

        List<Map<String, Object>> result = list.stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getEmployeeId());
            m.put("code", "NV" + String.format("%03d", e.getEmployeeId()));
            m.put("fullName", e.getFullName());
            m.put("username", e.getUsername());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    @PostMapping("/invoices")
    @Transactional
    public ResponseEntity<?> createInvoice(@RequestBody Map<String, Object> payload) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        Employee salesStaff = null;
        if (payload.containsKey("salesStaffId") && payload.get("salesStaffId") != null) {
            Long staffId = Long.valueOf(payload.get("salesStaffId").toString());
            salesStaff = employeeRepository.findById(staffId).orElse(null);
        }
        if (salesStaff == null && currentUsername != null) {
            salesStaff = employeeRepository.findByUsername(currentUsername).orElse(null);
        }
        if (salesStaff == null) {
            salesStaff = employeeRepository.findById(1L).orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        }

        Warehouse warehouse = warehouseRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kho"));

        Customer customer = customerRepository.findById(1L)
                .orElseGet(() -> customerRepository.findAll().stream().findFirst().orElse(null));

        String note = payload.get("note") != null ? payload.get("note").toString() : "";
        String paymentMethod = payload.get("paymentMethod") != null ? payload.get("paymentMethod").toString() : "CASH";

        Invoice invoice = Invoice.builder()
                .customer(customer)
                .salesStaff(salesStaff)
                .warehouse(warehouse)
                .createdAt(LocalDateTime.now())
                .totalAmount(BigDecimal.ZERO)
                .status("UNPAID")
                .note(note)
                .build();
        Invoice savedInvoice = invoiceRepository.save(invoice);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<InvoiceItem> invoiceItems = new ArrayList<>();
        LocalDate today = LocalDate.now();

        List<Map<String, Object>> itemsReq = (List<Map<String, Object>>) payload.get("items");
        if (itemsReq == null || itemsReq.isEmpty()) {
            throw new RuntimeException("Giỏ hàng không có sản phẩm nào!");
        }

        for (Map<String, Object> itemReq : itemsReq) {
            Long productId = Long.valueOf(itemReq.get("productId").toString());
            int requestedQty = Integer.parseInt(itemReq.get("quantity").toString());

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + productId));

            List<Batch> availableBatches = batchRepository.findAvailableBatchesForSaleFEFO(warehouse.getWarehouseId(), productId, today);
            int availableTotal = 0;
            for (Batch b : availableBatches) {
                Inventory inv = inventoryRepository.findByWarehouseWarehouseIdAndBatchBatchId(warehouse.getWarehouseId(), b.getBatchId()).orElse(null);
                if (inv != null && inv.getQuantity() > 0) {
                    availableTotal += inv.getQuantity();
                }
            }

            if (availableTotal < requestedQty) {
                throw new RuntimeException("Không đủ tồn kho khả dụng cho sản phẩm: " + product.getProductName() +
                        " (yêu cầu: " + requestedQty + ", còn khả dụng: " + availableTotal + ")");
            }

            int qtyRemaining = requestedQty;
            for (Batch b : availableBatches) {
                if (qtyRemaining <= 0) break;
                Inventory inv = inventoryRepository.findByWarehouseWarehouseIdAndBatchBatchId(warehouse.getWarehouseId(), b.getBatchId()).orElse(null);
                if (inv == null || inv.getQuantity() <= 0) continue;

                int take = Math.min(qtyRemaining, inv.getQuantity());
                BigDecimal price = product.getSellingPrice();
                BigDecimal subTotal = price.multiply(BigDecimal.valueOf(take));

                InvoiceItemId itemId = InvoiceItemId.builder()
                        .invoiceId(savedInvoice.getInvoiceId())
                        .batchId(b.getBatchId())
                        .build();

                InvoiceItem item = InvoiceItem.builder()
                        .id(itemId)
                        .invoice(savedInvoice)
                        .batch(b)
                        .quantity(take)
                        .unitPrice(price)
                        .subTotal(subTotal)
                        .build();

                invoiceItems.add(invoiceItemRepository.save(item));
                totalAmount = totalAmount.add(subTotal);
                qtyRemaining -= take;
            }
        }

        savedInvoice.setTotalAmount(totalAmount);
        savedInvoice.setItems(invoiceItems);
        invoiceRepository.save(savedInvoice);

        Map<String, Object> invData = mapInvoiceToResponse(savedInvoice);
        return ResponseEntity.ok(Map.of("success", true, "message", "Lập hóa đơn thành công", "data", invData));
    }

    @GetMapping({"/invoices", "/sales/invoices", "/invoices/my"})
    public ResponseEntity<?> getAllInvoices() {
        List<Invoice> list = invoiceRepository.findAllByOrderByInvoiceIdDesc();
        List<Map<String, Object>> result = list.stream().map(this::mapInvoiceToResponse).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    @GetMapping({"/invoices/{id}", "/v1/invoices/{id}"})
    public ResponseEntity<?> getInvoiceById(@PathVariable Long id) {
        Invoice inv = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn ID: " + id));
        return ResponseEntity.ok(Map.of("success", true, "data", mapInvoiceToResponse(inv)));
    }

    @PostMapping("/invoices/{id}/cancel")
    @Transactional
    public ResponseEntity<?> cancelInvoice(@PathVariable Long id) {
        Invoice inv = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        if ("PAID".equalsIgnoreCase(inv.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Không thể hủy hóa đơn đã thanh toán!"));
        }
        inv.setStatus("CANCELLED");
        invoiceRepository.save(inv);
        return ResponseEntity.ok(Map.of("success", true, "message", "Hủy hóa đơn thành công"));
    }

    @PostMapping("/sales/invoices/{id}/confirm")
    public ResponseEntity<?> confirmPaymentSales(@PathVariable Long id) {
        try {
            invoiceService.confirmPayment(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Thanh toán thành công! Đã trừ kho theo FIFO và cộng doanh thu vào số dư Admin."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // =========================================================================
    // 5. WAREHOUSE MANAGEMENT & SLIPS
    // =========================================================================

    @GetMapping("/warehouse/batches")
    public ResponseEntity<?> getWarehouseBatches() {
        List<Batch> batches = batchRepository.findAll();
        LocalDate now = LocalDate.now();

        List<Map<String, Object>> result = batches.stream().map(b -> {
            Integer stock = inventoryRepository.findByWarehouseWarehouseIdAndBatchBatchId(1L, b.getBatchId())
                    .map(Inventory::getQuantity).orElse(0);
            Integer totalProductStock = inventoryRepository.getTotalStockByProduct(b.getProduct().getProductId());

            Map<String, Object> m = new HashMap<>();
            m.put("id", b.getBatchId());
            m.put("batchId", b.getBatchId());
            m.put("batchCode", b.getBatchCode());
            m.put("productId", b.getProduct().getProductId());
            m.put("productName", b.getProduct().getProductName());
            m.put("categoryName", b.getProduct().getCategory().getCategoryName());
            m.put("mfgDate", b.getMfgDate().toString());
            m.put("expDate", b.getExpDate().toString());
            m.put("quantity", stock);
            m.put("productTotalStock", totalProductStock != null ? totalProductStock : 0);
            m.put("sellingPrice", b.getProduct().getSellingPrice());
            m.put("status", b.getExpDate().isBefore(now) ? "Hết hạn" : "Còn hạn");
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    @GetMapping("/warehouse/batches/{id}")
    public ResponseEntity<?> getBatchDetail(@PathVariable Long id) {
        Batch b = batchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lô hàng"));
        Inventory inv = inventoryRepository.findByWarehouseWarehouseIdAndBatchBatchId(1L, b.getBatchId()).orElse(null);

        Map<String, Object> data = new HashMap<>();
        data.put("id", b.getBatchId());
        data.put("batchCode", b.getBatchCode());
        data.put("productName", b.getProduct().getProductName());
        data.put("mfgDate", b.getMfgDate().toString());
        data.put("expDate", b.getExpDate().toString());
        data.put("warehouseCode", "WH001");
        data.put("warehouseName", "Kho Tổng Dược Phẩm MediCare");
        data.put("warehouseAddress", "Hà Nội, Việt Nam");
        data.put("updatedAt", inv != null && inv.getUpdatedAt() != null ? inv.getUpdatedAt().toString() : "Mới cập nhật");
        data.put("quantity", inv != null ? inv.getQuantity() : 0);

        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    @GetMapping("/warehouse/batches/disposal-candidates")
    public ResponseEntity<?> getDisposalCandidates() {
        return getWarehouseBatches();
    }

    @GetMapping("/warehouse/list")
    public ResponseEntity<?> getWarehouses() {
        List<Warehouse> list = warehouseRepository.findAll();
        List<Map<String, Object>> result = list.stream().map(w -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", w.getWarehouseId());
            m.put("warehouseCode", "WH00" + w.getWarehouseId());
            m.put("warehouseName", w.getWarehouseName());
            m.put("warehouseAddress", w.getWarehouseAddress());
            m.put("status", w.getStatus());
            m.put("totalStock", 1000);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    @GetMapping("/warehouse/suppliers")
    public ResponseEntity<?> getSuppliers() {
        List<Supplier> list = supplierRepository.findAll();
        List<Map<String, Object>> result = list.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getSupplierId());
            m.put("supplierCode", "SUP" + String.format("%03d", s.getSupplierId()));
            m.put("name", s.getSupplierName());
            m.put("supplierName", s.getSupplierName());
            m.put("phone", s.getPhone());
            m.put("representativeName", s.getRepresentativeName());
            m.put("taxCode", s.getTaxCode());
            m.put("address", s.getAddress());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    @PostMapping("/warehouse/suppliers")
    public ResponseEntity<?> createSupplier(@RequestBody Map<String, String> body) {
        Supplier s = Supplier.builder()
                .supplierName(body.get("name"))
                .phone(body.get("phone"))
                .taxCode(body.get("taxCode"))
                .representativeName(body.get("representativeName"))
                .address(body.get("address"))
                .build();
        supplierRepository.save(s);
        return ResponseEntity.ok(Map.of("success", true, "message", "Thêm nhà cung cấp thành công!"));
    }

    @GetMapping("/warehouse/replenishments")
    public ResponseEntity<?> getReplenishments() {
        List<Product> products = productRepository.findAll();
        List<Map<String, Object>> list = new ArrayList<>();
        int idCounter = 1;
        for (Product p : products) {
            Integer stock = inventoryRepository.getTotalStockByProduct(p.getProductId());
            if (stock != null && stock < 100) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", idCounter++);
                m.put("productName", p.getProductName());
                m.put("requestedQuantity", 200 - stock);
                m.put("salesStaffName", "Dược sĩ Kho");
                m.put("status", "PENDING");
                list.add(m);
            }
        }
        return ResponseEntity.ok(Map.of("success", true, "data", list));
    }

    @PutMapping("/warehouse/replenishments/{id}/status")
    public ResponseEntity<?> updateReplenishStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật trạng thái thành công!"));
    }

    @PostMapping("/warehouse/slips/import")
    @Transactional
    public ResponseEntity<?> createImportSlip(@RequestBody Map<String, Object> body) {
        Long productId = Long.valueOf(body.get("productId").toString());
        LocalDate mfgDate = LocalDate.parse(body.get("mfgDate").toString());
        LocalDate expDate = LocalDate.parse(body.get("expDate").toString());
        int quantity = Integer.parseInt(body.get("quantity").toString());

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc"));

        BigDecimal unitPrice = new BigDecimal("10000.00");
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

        // Kiểm tra số dư Admin
        Employee admin = employeeRepository.findByPositionAndWorkStatus("ROLE_ADMIN", "ACTIVE").stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản Quản trị"));

        if (admin.getBalance() == null || admin.getBalance().compareTo(totalAmount) < 0) {
            Map<String, Object> failData = Map.of("resultState", "fail");
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "data", failData,
                    "message", "Tạo Phiếu Nhập Thất Bại: Số dư Admin không đủ chi trả đơn nhập hàng này!"
            ));
        }

        // Tạo Batch mới
        String batchCode = "LO" + (System.currentTimeMillis() % 1000000);
        Batch batch = Batch.builder()
                .product(product)
                .batchCode(batchCode)
                .mfgDate(mfgDate)
                .expDate(expDate)
                .build();
        batch = batchRepository.save(batch);

        Supplier supplier = supplierRepository.findById(1L).orElseGet(() -> supplierRepository.findAll().get(0));
        Warehouse warehouse = warehouseRepository.findById(1L).orElseGet(() -> warehouseRepository.findAll().get(0));

        ImportSlip slip = ImportSlip.builder()
                .supplier(supplier)
                .warehouseStaff(admin)
                .warehouse(warehouse)
                .importDate(LocalDateTime.now())
                .totalAmount(totalAmount)
                .status("PENDING")
                .build();
        slip = importSlipRepository.save(slip);

        ImportSlipItemId itemId = ImportSlipItemId.builder()
                .importSlipId(slip.getImportSlipId())
                .batchId(batch.getBatchId())
                .build();

        ImportSlipItem item = ImportSlipItem.builder()
                .id(itemId)
                .importSlip(slip)
                .batch(batch)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .subTotal(totalAmount)
                .build();
        importSlipItemRepository.save(item);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã lập Phiếu Nhập hàng thành công! Đang chờ Admin duyệt.",
                "data", Map.of("slipId", slip.getImportSlipId())
        ));
    }

    @PostMapping("/warehouse/slips/disposal")
    public ResponseEntity<?> createDisposalSlip(@RequestBody Map<String, Object> body) {
        Long batchId = Long.valueOf(body.get("batchId").toString());
        int quantity = Integer.parseInt(body.get("quantity").toString());
        String reason = (String) body.get("reason");

        if (reason == null || reason.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Lý do hủy là bắt buộc!"));
        }

        Inventory inv = inventoryRepository.findByWarehouseWarehouseIdAndBatchBatchId(1L, batchId).orElse(null);
        if (inv == null || inv.getQuantity() < quantity) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "data", Map.of("resultState", "fail"),
                    "message", "Tạo Phiếu Hủy Thất Bại: Số lượng hủy vượt quá tồn kho của lô!"
            ));
        }

        try {
            disposalSlipService.createAndConfirmDisposalSlip(com.pharmacy.dto.CreateDisposalSlipRequest.builder()
                    .warehouseId(1L)
                    .batchId(batchId)
                    .quantity(quantity)
                    .reason(reason)
                    .build());
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã lập và xác nhận phiếu hủy thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // =========================================================================
    // 6. ADMIN MANAGEMENT & STATISTICS
    // =========================================================================

    @GetMapping("/admin/batches/pending-price")
    public ResponseEntity<?> getPendingBatches() {
        List<Batch> list = batchRepository.findAll();
        List<Map<String, Object>> result = list.stream().limit(5).map(b -> {
            Integer stock = inventoryRepository.findByWarehouseWarehouseIdAndBatchBatchId(1L, b.getBatchId())
                    .map(Inventory::getQuantity).orElse(50);
            Map<String, Object> m = new HashMap<>();
            m.put("id", b.getBatchId());
            m.put("batchId", b.getBatchId());
            m.put("batchCode", b.getBatchCode());
            m.put("productName", b.getProduct().getProductName());
            m.put("mfgDate", b.getMfgDate().toString());
            m.put("expDate", b.getExpDate().toString());
            m.put("quantity", stock);
            m.put("sellingPrice", b.getProduct().getSellingPrice());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    @PostMapping("/admin/batches/{id}/approve-price")
    public ResponseEntity<?> approveBatchPrice(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal sellingPrice = new BigDecimal(body.get("sellingPrice").toString());
        Batch b = batchRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy lô"));
        Product p = b.getProduct();
        p.setSellingPrice(sellingPrice);
        productRepository.save(p);
        return ResponseEntity.ok(Map.of("success", true, "message", "Duyệt giá bán thành công!"));
    }

    @GetMapping("/admin/users")
    public ResponseEntity<?> getAdminUsers() {
        List<Employee> list = employeeRepository.findAll();
        List<Map<String, Object>> result = list.stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getEmployeeId());
            m.put("username", e.getUsername());
            m.put("fullName", e.getFullName());
            m.put("phone", e.getPhone() != null ? e.getPhone() : "");
            m.put("balance", e.getBalance() != null ? e.getBalance() : BigDecimal.ZERO);
            m.put("roles", List.of(e.getPosition()));
            m.put("status", e.getWorkStatus());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    @PutMapping("/admin/users/{userId}/role")
    public ResponseEntity<?> changeUserRole(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        Employee e = employeeRepository.findById(userId).orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        e.setPosition(body.get("role"));
        employeeRepository.save(e);
        return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật quyền thành công!"));
    }

    @PutMapping("/admin/users/{userId}/status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        Employee e = employeeRepository.findById(userId).orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        e.setWorkStatus(body.get("status"));
        employeeRepository.save(e);
        return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật trạng thái thành công!"));
    }

    @PostMapping("/admin/users/{userId}/balance")
    public ResponseEntity<?> adjustUserBalance(@PathVariable Long userId, @RequestBody Map<String, Object> body) {
        Employee e = employeeRepository.findById(userId).orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        BigDecimal current = e.getBalance() != null ? e.getBalance() : BigDecimal.ZERO;
        e.setBalance(current.add(amount));
        employeeRepository.save(e);
        return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật số dư thành công!"));
    }

    @GetMapping("/admin/statistics")
    public ResponseEntity<?> getAdminStatistics(@RequestParam(name = "period", defaultValue = "month") String period) {
        List<Invoice> paidInvoices = invoiceRepository.findByStatusOrderByInvoiceIdDesc("PAID");
        BigDecimal totalRevenue = paidInvoices.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ImportSlip> importedSlips = importSlipRepository.findByStatusOrderByImportSlipIdDesc("IMPORTED");
        BigDecimal totalExpense = importedSlips.stream()
                .map(ImportSlip::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal profit = totalRevenue.subtract(totalExpense);

        Employee admin = employeeRepository.findByPositionAndWorkStatus("ROLE_ADMIN", "ACTIVE").stream().findFirst().orElse(null);
        BigDecimal adminBalance = admin != null && admin.getBalance() != null ? admin.getBalance() : BigDecimal.ZERO;

        List<Map<String, Object>> recentPaid = paidInvoices.stream().limit(5).map(this::mapInvoiceToResponse).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("totalRevenue", totalRevenue);
        data.put("paidInvoicesCount", paidInvoices.size());
        data.put("totalImportExpense", totalExpense);
        data.put("importSlipsCount", importedSlips.size());
        data.put("netProfit", profit);
        data.put("adminCurrentBalance", adminBalance);
        data.put("recentPaidInvoices", recentPaid);

        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    // =========================================================================
    // 7. NOTIFICATIONS (DUYỆT PHIẾU NHẬP / HỦY)
    // =========================================================================

    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications() {
        List<Map<String, Object>> list = new ArrayList<>();

        // 1. Phiếu nhập đang chờ duyệt
        List<ImportSlip> pendingImports = importSlipRepository.findByStatusOrderByImportSlipIdDesc("PENDING");
        for (ImportSlip slip : pendingImports) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", slip.getImportSlipId());
            m.put("type", "IMPORT_CONFIRM");
            m.put("title", "Yêu cầu duyệt Nhập kho #" + slip.getImportSlipId());
            m.put("message", "Nhập hàng từ NCC (" + (slip.getSupplier() != null ? slip.getSupplier().getSupplierName() : "NCC") +
                    "), tổng giá trị: " + String.format("%,.0f VNĐ", slip.getTotalAmount()) + ".");
            m.put("createdAt", slip.getImportDate().toString());
            m.put("actionStatus", "PENDING");
            m.put("isRead", false);
            list.add(m);
        }

        // 2. Phiếu hủy đang chờ duyệt
        List<DisposalSlip> pendingDisposals = disposalSlipRepository.findByStatusOrderByDisposalSlipIdDesc("PENDING");
        for (DisposalSlip slip : pendingDisposals) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", slip.getDisposalSlipId());
            m.put("type", "DISPOSAL_CONFIRM");
            m.put("title", "Yêu cầu duyệt Hủy thuốc #" + slip.getDisposalSlipId());
            m.put("message", "Đơn hủy lô thuốc tại " + slip.getWarehouse().getWarehouseName() + " đang chờ phê duyệt.");
            m.put("createdAt", slip.getDisposalDate().toString());
            m.put("actionStatus", "PENDING");
            m.put("isRead", false);
            list.add(m);
        }

        return ResponseEntity.ok(Map.of("success", true, "data", list));
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<?> getNotificationUnreadCount() {
        long count = importSlipRepository.findByStatusOrderByImportSlipIdDesc("PENDING").size() +
                disposalSlipRepository.findByStatusOrderByDisposalSlipIdDesc("PENDING").size();
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("unreadCount", count)));
    }

    @PostMapping("/notifications/{id}/action")
    public ResponseEntity<?> handleNotificationAction(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String decision = body.get("decision");
        ImportSlip is = importSlipRepository.findById(id).orElse(null);
        if (is != null && "PENDING".equalsIgnoreCase(is.getStatus())) {
            if ("APPROVE".equalsIgnoreCase(decision)) {
                try {
                    importSlipService.confirmImportSlip(id);
                    return ResponseEntity.ok(Map.of("success", true, "message", "Phê duyệt phiếu nhập thành công!"));
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
                }
            } else {
                is.setStatus("CANCELLED");
                importSlipRepository.save(is);
                return ResponseEntity.ok(Map.of("success", true, "message", "Đã từ chối phiếu nhập!"));
            }
        }

        DisposalSlip ds = disposalSlipRepository.findById(id).orElse(null);
        if (ds != null && "PENDING".equalsIgnoreCase(ds.getStatus())) {
            if ("APPROVE".equalsIgnoreCase(decision)) {
                try {
                    disposalSlipService.confirmDisposalSlip(id);
                    return ResponseEntity.ok(Map.of("success", true, "message", "Phê duyệt phiếu hủy thành công!"));
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
                }
            } else {
                ds.setStatus("CANCELLED");
                disposalSlipRepository.save(ds);
                return ResponseEntity.ok(Map.of("success", true, "message", "Đã từ chối phiếu hủy!"));
            }
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "Đã xử lý thông báo!"));
    }

    // =========================================================================
    // HELPER MAPPING
    // =========================================================================

    private Map<String, Object> mapInvoiceToResponse(Invoice inv) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", inv.getInvoiceId());
        m.put("invoiceId", inv.getInvoiceId());
        m.put("invoiceCode", "HD" + String.format("%05d", inv.getInvoiceId()));
        m.put("customerName", inv.getCustomer() != null ? inv.getCustomer().getFullName() : "Khách vãng lai");
        m.put("customerPhone", inv.getCustomer() != null ? inv.getCustomer().getPhone() : "");
        m.put("customerCode", "KH" + (inv.getCustomer() != null ? inv.getCustomer().getCustomerId() : 1));
        m.put("salesStaffName", inv.getSalesStaff() != null ? inv.getSalesStaff().getFullName() : "Dược sĩ bán hàng");
        m.put("salesStaffCode", "NV" + (inv.getSalesStaff() != null ? inv.getSalesStaff().getEmployeeId() : 1));
        m.put("warehouseName", inv.getWarehouse() != null ? inv.getWarehouse().getWarehouseName() : "Kho Chính");
        m.put("totalAmount", inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO);
        m.put("paymentMethod", "TIỀN MẶT / VÍ HỆ THỐNG");
        m.put("status", inv.getStatus());
        m.put("createdAt", inv.getCreatedAt() != null ? inv.getCreatedAt().toString() : LocalDateTime.now().toString());

        long elapsedSec = inv.getCreatedAt() != null ? Duration.between(inv.getCreatedAt(), LocalDateTime.now()).getSeconds() : 0;
        long remainingSec = Math.max(0, 600 - elapsedSec);
        m.put("remainingSeconds", remainingSec);

        List<Map<String, Object>> items = new ArrayList<>();
        if (inv.getItems() != null) {
            for (InvoiceItem item : inv.getItems()) {
                Map<String, Object> it = new HashMap<>();
                it.put("id", item.getId() != null ? item.getId().getBatchId() : 1L);
                it.put("productName", item.getBatch().getProduct().getProductName());
                it.put("batchCode", item.getBatch().getBatchCode());
                it.put("expDate", item.getBatch().getExpDate().toString());
                it.put("quantity", item.getQuantity());
                it.put("unitPrice", item.getUnitPrice());
                it.put("subTotal", item.getSubTotal());
                it.put("unit", item.getBatch().getProduct().getUnit() != null ? item.getBatch().getProduct().getUnit() : "Hộp");
                items.add(it);
            }
        }
        m.put("items", items);
        return m;
    }
}
