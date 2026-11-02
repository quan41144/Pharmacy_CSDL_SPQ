package com.pharmacy.medicare.init;

import com.pharmacy.medicare.entity.*;
import com.pharmacy.medicare.entity.enums.*;
import com.pharmacy.medicare.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           BatchRepository batchRepository,
                           WarehouseRepository warehouseRepository,
                           SupplierRepository supplierRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.warehouseRepository = warehouseRepository;
        this.supplierRepository = supplierRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== BẮT ĐẦU KHỞI TẠO DỮ LIỆU HỆ THỐNG HIỆU THUỐC MEDICARE ===");

        // 1. Khởi tạo 4 Roles
        Map<RoleName, Role> roleMap = initRoles();

        // 2. Khởi tạo các tài khoản người dùng mặc định
        initUsers(roleMap);

        // 3. Khởi tạo Kho mẫu
        Map<String, Warehouse> warehouseMap = initWarehouses();

        // 4. Khởi tạo Nhà cung cấp mẫu
        initSuppliers();

        // 5. Khởi tạo các Danh mục thuốc
        Map<String, Category> categoryMap = initCategories();

        // 6. Khởi tạo Sản phẩm và Lô thuốc mẫu
        initProductsAndBatches(categoryMap, warehouseMap);

        log.info("=== KHỞI TẠO DỮ LIỆU BAN ĐẦU HOÀN TẤT THÀNH CÔNG ===");
    }

    private Map<RoleName, Role> initRoles() {
        Map<RoleName, Role> roleMap = new EnumMap<>(RoleName.class);
        for (RoleName roleName : RoleName.values()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseGet(() -> {
                        String desc = switch (roleName) {
                            case ROLE_ADMIN -> "Quản trị viên / Chủ cửa hàng";
                            case ROLE_SALES -> "Nhân viên bán hàng";
                            case ROLE_WAREHOUSE -> "Nhân viên kho";
                            case ROLE_CUSTOMER -> "Khách hàng mua thuốc";
                        };
                        return roleRepository.save(new Role(roleName, desc));
                    });
            roleMap.put(roleName, role);
        }
        return roleMap;
    }

    private void initUsers(Map<RoleName, Role> roleMap) {
        Role adminRole = roleMap.get(RoleName.ROLE_ADMIN);
        Role customerRole = roleMap.get(RoleName.ROLE_CUSTOMER);

        // Tài khoản Admin duy nhất ban đầu: quan41144 / 12345, số dư 100,000,000 VND
        if (!userRepository.existsByUsername("quan41144")) {
            User admin = new User(
                    "quan41144",
                    passwordEncoder.encode("12345"),
                    "Chủ Cửa Hàng Quản",
                    new BigDecimal("100000000.00"),
                    UserStatus.ACTIVE
            );
            admin.setRoles(new HashSet<>(Arrays.asList(adminRole, customerRole)));
            admin.setGender("Nam");
            admin.setPhone("0987654321");
            admin.setAddress("Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội");
            admin.setDateOfBirth(LocalDate.of(1990, 1, 1));
            admin.setPosition("Quản trị viên / Chủ cửa hàng");
            admin.setWorkStatus("ACTIVE");
            admin.setStartDate(LocalDate.now().minusYears(3));
            userRepository.save(admin);
            log.info("-> Đã tạo tài khoản Admin duy nhất ban đầu: quan41144 / 12345 (Số dư: 100,000,000 VND)");
        }
    }

    private Map<String, Warehouse> initWarehouses() {
        Map<String, Warehouse> warehouseMap = new HashMap<>();
        String[][] data = {
                {"WH001", "Kho Dược Phẩm Trung Tâm", "Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội"},
                {"WH002", "Kho Chi Nhánh Cầu Giấy", "Số 126 Cầu Giấy, Cầu Giấy, Hà Nội"}
        };
        for (String[] row : data) {
            String code = row[0];
            String name = row[1];
            String addr = row[2];
            Warehouse wh = warehouseRepository.findByWarehouseName(name)
                    .orElseGet(() -> warehouseRepository.save(new Warehouse(name, addr)));
            warehouseMap.put(code, wh);
        }
        return warehouseMap;
    }

    private void initSuppliers() {
        if (supplierRepository.count() > 0) return;
        String[][] data = {
                {"Công Ty CP Dược Phẩm Trung Ương 1 (CPC1)", "02438452378", "Nguyễn Văn Hùng", "0100109403", "356 Giải Phóng, Phương Liệt, Thanh Xuân, Hà Nội"},
                {"Công Ty CP Dược Hậu Giang (DHG Pharma)", "02923891433", "Đoàn Đình Duy Khương", "1800156801", "288 Nguyễn Văn Cừ, An Hòa, Ninh Kiều, Cần Thơ"},
                {"Công Ty Dược Phẩm Sanofi-Synthelabo VN", "02838298526", "David Nguyễn", "0302061266", "Số 10 Hàm Nghi, Bến Nghé, Quận 1, TP.HCM"}
        };
        for (String[] row : data) {
            supplierRepository.save(new Supplier(row[0], row[1], row[2], row[3], row[4]));
        }
    }

    private Map<String, Category> initCategories() {
        Map<String, Category> categoryMap = new HashMap<>();
        String[][] categoriesData = {
                {"Thực phẩm chức năng", "Hỗ trợ sức khỏe, tăng cường đề kháng và bổ sung vitamin khoáng chất"},
                {"Dược mỹ phẩm", "Sản phẩm chăm sóc da liễu, phục hồi và bảo vệ da chuẩn y tế"},
                {"Thuốc", "Thuốc điều trị theo đơn và không kê đơn đạt chuẩn GPP"},
                {"Chăm sóc cá nhân", "Sản phẩm vệ sinh, khử khuẩn và chăm sóc sức khỏe gia đình"}
        };

        for (String[] cat : categoriesData) {
            String name = cat[0];
            String desc = cat[1];
            Category category = categoryRepository.findByName(name)
                    .orElseGet(() -> categoryRepository.save(new Category(name, desc)));
            categoryMap.put(name, category);
        }
        return categoryMap;
    }

    private void initProductsAndBatches(Map<String, Category> categoryMap, Map<String, Warehouse> warehouseMap) {
        if (productRepository.count() > 0) {
            return;
        }

        Category catTPCN = categoryMap.get("Thực phẩm chức năng");
        Category catDMP = categoryMap.get("Dược mỹ phẩm");
        Category catThuoc = categoryMap.get("Thuốc");
        Category catCSCN = categoryMap.get("Chăm sóc cá nhân");

        // Danh sách sản phẩm mẫu chuẩn y tế
        Object[][] productsData = {
                // Tên, Category, Đơn vị, Giá bán, Mô tả, Công dụng, Thành phần, Số lượng tồn kho ban đầu
                {
                        "Panadol Extra Đỏ Giảm Đau Hạ Sốt",
                        catThuoc,
                        "Hộp",
                        new BigDecimal("45000.00"),
                        "Thuốc giảm đau nhanh các cơn đau đầu, đau cơ, sốt do cảm cúm.",
                        "Hạ sốt nhanh, giảm cơn đau đầu, đau nửa đầu, đau họng, đau răng.",
                        "Paracetamol 500mg, Caffeine 65mg",
                        120
                },
                {
                        "Paracetamol Kháng Viêm Giảm Đau 500mg",
                        catThuoc,
                        "Vỉ",
                        new BigDecimal("15000.00"),
                        "Thuốc giảm đau hạ sốt phổ biến cho người lớn và trẻ em trên 12 tuổi.",
                        "Điều trị sốt, giảm triệu chứng đau nhức cơ bắp, nhức đầu do thời tiết.",
                        "Paracetamol 500mg tá dược vừa đủ 1 viên",
                        200
                },
                {
                        "Vitamin C 1000mg Tăng Sức Đề Kháng",
                        catTPCN,
                        "Tuýp",
                        new BigDecimal("68000.00"),
                        "Viên sủi bổ sung vitamin C hàm lượng cao giúp cơ thể phục hồi thể lực.",
                        "Tăng cường hệ miễn dịch, chống oxy hóa, phòng ngừa cảm cúm.",
                        "Vitamin C (Acid Ascorbic) 1000mg, Kẽm Gluconat 10mg",
                        80
                },
                {
                        "Men Vi Sinh Enterogermina Đường Ruột",
                        catTPCN,
                        "Hộp",
                        new BigDecimal("165000.00"),
                        "Hỗn dịch uống bổ sung bào tử Bacillus clausii kháng đa kháng sinh.",
                        "Hỗ trợ điều trị rối loạn khuẩn chí đường ruột, kém hấp thu vitamin.",
                        "Bào tử Bacillus clausii 2 tỷ bào tử / 5ml",
                        60
                },
                {
                        "Kem Chống Nắng Dược Mỹ Phẩm La Roche-Posay",
                        catDMP,
                        "Tuýp",
                        new BigDecimal("385000.00"),
                        "Kem chống nắng kiểm soát bóng nhờn, bảo vệ quang phổ rộng SPF50+.",
                        "Ngăn ngừa tác hại tia UVA/UVB, chống lão hóa sớm, kiềm dầu tối ưu 8h.",
                        "Màng lọc Mexoryl 400, Nước khoáng La Roche-Posay, Airlicium",
                        40
                },
                {
                        "Sữa Rửa Mặt Cho Da Dầu Mụn Cerave Foaming",
                        catDMP,
                        "Chai",
                        new BigDecimal("320000.00"),
                        "Sữa rửa mặt tạo bọt nhẹ dịu, làm sạch sâu lỗ chân lông không gây khô căng.",
                        "Làm sạch bụi bẩn, bã nhờn, củng cố hàng rào bảo vệ da tự nhiên.",
                        "Ceramides 1, 3, 6-II, Niacinamide, Hyaluronic Acid",
                        35
                },
                {
                        "Nước Muối Sinh Lý Natri Clorid 0.9% 500ml",
                        catCSCN,
                        "Chai",
                        new BigDecimal("10000.00"),
                        "Dung dịch rửa vết thương, súc họng, rửa mũi vô trùng đạt chuẩn y tế.",
                        "Vệ sinh mắt mũi hằng ngày, kháng khuẩn niêm mạc hô hấp, sát trùng nhẹ.",
                        "Natri Clorid 4.5g, Nước tinh khiết vừa đủ 500ml",
                        150
                },
                {
                        "Khẩu Trang Y Tế Kháng Khuẩn 4 Lớp Chuẩn Y Tế",
                        catCSCN,
                        "Hộp",
                        new BigDecimal("35000.00"),
                        "Khẩu trang y tế tiệt trùng 4 lớp vải không dệt và màng lọc kháng khuẩn Meltblown.",
                        "Ngăn giọt bắn, chống bụi mịn PM2.5, bảo vệ hệ hô hấp tối đa.",
                        "Vải không dệt polypropylene, màng vi lọc kháng khuẩn",
                        90
                }
        };

        String defaultSvgImage = "data:image/svg+xml;charset=UTF-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22600%22%20height%3D%22400%22%20viewBox%3D%220%200%20600%20400%22%3E%3Cdefs%3E%3ClinearGradient%20id%3D%22medGrad%22%20x1%3D%220%25%22%20y1%3D%220%25%22%20x2%3D%22100%25%22%20y2%3D%22100%25%22%3E%3Cstop%20offset%3D%220%25%22%20stop-color%3D%22%230d9488%22%2F%3E%3Cstop%20offset%3D%2250%25%22%20stop-color%3D%22%230f766e%22%2F%3E%3Cstop%20offset%3D%22100%25%22%20stop-color%3D%22%230284c7%22%2F%3E%3C%2FlinearGradient%3E%3C%2Fdefs%3E%3Crect%20width%3D%22600%22%20height%3D%22400%22%20fill%3D%22url(%23medGrad)%22%2F%3E%3Ccircle%20cx%3D%22300%22%20cy%3D%22175%22%20r%3D%2278%22%20fill%3D%22%23ffffff%22%20fill-opacity%3D%220.16%22%2F%3E%3Ccircle%20cx%3D%22300%22%20cy%3D%22175%22%20r%3D%2260%22%20fill%3D%22%23ffffff%22%2F%3E%3Crect%20x%3D%22286%22%20y%3D%22137%22%20width%3D%2228%22%20height%3D%2276%22%20rx%3D%226%22%20fill%3D%22%230d9488%22%2F%3E%3Crect%20x%3D%22262%22%20y%3D%22161%22%20width%3D%2276%22%20height%3D%2228%22%20rx%3D%226%22%20fill%3D%22%230d9488%22%2F%3E%3Ctext%20x%3D%22300%22%20y%3D%22295%22%20font-family%3D%22Inter%2C%20Arial%2C%20sans-serif%22%20font-size%3D%2222%22%20font-weight%3D%22bold%22%20fill%3D%22%23ffffff%22%20text-anchor%3D%22middle%22%3EH%C3%8CNH%20%E1%BA%A2NH%20HI%E1%BB%86U%20THU%E1%BB%90C%20CHUNG%3C%2Ftext%3E%3Ctext%20x%3D%22300%22%20y%3D%22325%22%20font-family%3D%22Inter%2C%20Arial%2C%20sans-serif%22%20font-size%3D%2214%22%20fill%3D%22%23ccfbf1%22%20text-anchor%3D%22middle%22%3E(Thay%20th%E1%BA%BF%20bi%E1%BA%BFn%20DEFAULT_IMAGE%20%C4%91%E1%BB%83%20%C4%91%E1%BB%95i%20to%C3%A0n%20b%E1%BB%99%20%E1%BA%A3nh)%3C%2Ftext%3E%3C%2Fsvg%3E";

        int batchCounter = 101;
        int prodCounter = 1;
        Warehouse defaultWh = warehouseMap.get("WH001");

        for (Object[] row : productsData) {
            String name = (String) row[0];
            Category category = (Category) row[1];
            String unit = (String) row[2];
            BigDecimal sellingPrice = (BigDecimal) row[3];
            String desc = (String) row[4];
            String uses = (String) row[5];
            String ingredients = (String) row[6];
            Integer initialStock = (Integer) row[7];
            BigDecimal fixedImportPrice = new BigDecimal("10000.00");

            String dosage = switch (unit.toLowerCase()) {
                case "vỉ" -> "Viên nén";
                case "tuýp" -> name.contains("Vitamin C") ? "Viên sủi" : "Kem bôi ngoài da";
                case "chai" -> name.contains("Sữa") ? "Gel tạo bọt" : "Dung dịch vô trùng";
                case "hộp" -> name.contains("Khẩu") ? "Khẩu trang y tế" : (name.contains("Men") ? "Hỗn dịch uống" : "Viên nén bao phim");
                default -> "Viên nén";
            };

            Product product = new Product(
                    name,
                    category,
                    defaultSvgImage,
                    desc,
                    uses,
                    ingredients,
                    unit,
                    fixedImportPrice,
                    sellingPrice,
                    initialStock
            );
            product.setDosageForm(dosage);
            product.setIsActive(true);
            Product savedProduct = productRepository.save(product);

            // Tạo Lô thuốc tương ứng ở trạng thái AVAILABLE
            Batch batch = new Batch(
                    savedProduct,
                    defaultWh,
                    LocalDate.now().minusMonths(2),
                    LocalDate.now().plusMonths(22),
                    initialStock,
                    fixedImportPrice,
                    BatchStatus.AVAILABLE,
                    false
            );
            batchRepository.save(batch);
        }

        // Tạo thêm 1 Lô thuốc bị hỏng / hết hạn để phục vụ test tính năng Phiếu Hủy (DISPOSAL)
        Optional<Product> sampleProd = productRepository.findAll().stream().findFirst();
        if (sampleProd.isPresent()) {
            Product prod = sampleProd.get();
            Batch damagedBatch = new Batch(
                    prod,
                    defaultWh,
                    LocalDate.now().minusYears(2),
                    LocalDate.now().minusDays(10), // Đã hết hạn
                    10,
                    new BigDecimal("10000.00"),
                    BatchStatus.AVAILABLE,
                    true // Đánh dấu hỏng
            );
            batchRepository.save(damagedBatch);
            log.info("-> Đã tạo 1 Lô thuốc mẫu hết hạn/bị hỏng để test Phiếu Hủy");
        }

        log.info("-> Đã tạo {} sản phẩm mẫu và các lô thuốc khả dụng tương ứng.", productsData.length);
    }
}
