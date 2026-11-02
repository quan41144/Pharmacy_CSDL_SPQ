package com.pharmacy.medicare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.medicare.dto.auth.LoginRequest;
import com.pharmacy.medicare.dto.notification.NotificationActionRequest;
import com.pharmacy.medicare.dto.warehouse.ApproveBatchPriceRequest;
import com.pharmacy.medicare.dto.warehouse.WarehouseSlipRequest;
import com.pharmacy.medicare.entity.Batch;
import com.pharmacy.medicare.entity.Product;
import com.pharmacy.medicare.entity.User;
import com.pharmacy.medicare.entity.WarehouseSlip;
import com.pharmacy.medicare.entity.enums.BatchStatus;
import com.pharmacy.medicare.entity.enums.RoleName;
import com.pharmacy.medicare.entity.enums.SlipStatus;
import com.pharmacy.medicare.entity.enums.UserStatus;
import com.pharmacy.medicare.repository.BatchRepository;
import com.pharmacy.medicare.repository.ProductRepository;
import com.pharmacy.medicare.repository.RoleRepository;
import com.pharmacy.medicare.repository.UserRepository;
import com.pharmacy.medicare.repository.WarehouseSlipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WarehouseAndAdminApprovalWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private WarehouseSlipRepository warehouseSlipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String getAuthToken(String username, String password) throws Exception {
        if (!userRepository.existsByUsername(username)) {
            User u = new User();
            u.setUsername(username);
            u.setPassword(passwordEncoder.encode(password));
            u.setFullName("Test User " + username);
            u.setStatus(UserStatus.ACTIVE);
            u.setBalance(BigDecimal.ZERO);
            u.setRoles(new HashSet<>(Arrays.asList(
                    roleRepository.findByName(RoleName.ROLE_WAREHOUSE).orElseThrow(),
                    roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow()
            )));
            userRepository.save(u);
        }
        LoginRequest req = new LoginRequest(username, password);
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        String json = res.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("data").get("accessToken").asText();
    }

    @Test
    @DisplayName("Test 1: Luồng hoàn chỉnh Phiếu Nhập -> Admin duyệt -> Trừ tiền Admin -> Lô chờ duyệt giá -> Admin duyệt giá & cập nhật tồn kho")
    void testImportApprovalAndPriceApprovalWorkflow() throws Exception {
        String warehouseToken = getAuthToken("kho1", "12345");
        String adminToken = getAuthToken("quan41144", "12345");

        Product sampleProduct = productRepository.findAll().stream().findFirst().orElseThrow();
        int initialTotalStock = sampleProduct.getTotalStock();

        User adminBefore = userRepository.findByUsername("quan41144").orElseThrow();
        BigDecimal adminInitialBalance = adminBefore.getBalance();

        // 1. NV Kho tạo phiếu nhập 30 sản phẩm (giá nhập 10000 -> tổng 300,000 VND)
        WarehouseSlipRequest importReq = new WarehouseSlipRequest();
        importReq.setProductId(sampleProduct.getId());
        importReq.setQuantity(30);
        importReq.setMfgDate(LocalDate.now().minusMonths(1));
        importReq.setExpDate(LocalDate.now().plusMonths(24));

        MvcResult slipResult = mockMvc.perform(post("/api/warehouse/slips/import")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(importReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.resultState", is("PENDING")))
                .andReturn();

        long slipId = objectMapper.readTree(slipResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // 2. Admin lấy danh sách thông báo và duyệt Phiếu Nhập
        MvcResult notifResult = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        var notifArray = objectMapper.readTree(notifResult.getResponse().getContentAsString()).get("data");
        long notifId = -1;
        for (var n : notifArray) {
            if ("IMPORT_CONFIRM".equals(n.get("type").asText()) && n.get("referenceId").asLong() == slipId) {
                notifId = n.get("id").asLong();
                break;
            }
        }
        assertTrue(notifId > 0, "Phải tìm thấy thông báo duyệt phiếu nhập");

        NotificationActionRequest approveAction = new NotificationActionRequest("ACCEPT");
        mockMvc.perform(post("/api/notifications/" + notifId + "/action")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveAction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 3. Kiểm tra số dư Admin đã bị trừ đúng 300,000 VND
        User adminAfter = userRepository.findByUsername("quan41144").orElseThrow();
        assertEquals(adminInitialBalance.subtract(new BigDecimal("300000.00")), adminAfter.getBalance());

        // 4. Admin xem danh sách lô hàng chờ duyệt giá bán (WAITING_FOR_PRICE)
        MvcResult pendingPriceResult = mockMvc.perform(get("/api/admin/batches/pending-price")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andReturn();

        long batchId = objectMapper.readTree(pendingPriceResult.getResponse().getContentAsString())
                .get("data").get(0).get("id").asLong();

        // 5. Admin duyệt bán với giá mới 48,000 VND
        ApproveBatchPriceRequest priceReq = new ApproveBatchPriceRequest(new BigDecimal("48000.00"));
        mockMvc.perform(post("/api/admin/batches/" + batchId + "/approve-price")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(priceReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("AVAILABLE")));

        // 6. Kiểm tra giá bán của sản phẩm đã cập nhật 48,000 VND và tổng tồn kho tăng thêm 30
        Product updatedProduct = productRepository.findById(sampleProduct.getId()).orElseThrow();
        assertEquals(new BigDecimal("48000.00"), updatedProduct.getSellingPrice());
        assertEquals(initialTotalStock + 30, updatedProduct.getTotalStock());
    }

    @Test
    @DisplayName("Test 2: Luồng Phiếu Hủy với Lô bị hỏng/hết hạn")
    void testDisposalWorkflow() throws Exception {
        String warehouseToken = getAuthToken("kho1", "12345");
        String adminToken = getAuthToken("quan41144", "12345");

        Product sampleProduct = productRepository.findAll().stream().findFirst().orElseThrow();
        Batch damagedBatch = new Batch(
                sampleProduct,
                LocalDate.now().minusYears(2),
                LocalDate.now().minusDays(5),
                10,
                new BigDecimal("10000.00"),
                BatchStatus.AVAILABLE,
                true
        );
        damagedBatch = batchRepository.save(damagedBatch);

        // 1. NV Kho tạo phiếu hủy cho lô hỏng này (SL 10)
        WarehouseSlipRequest disposalReq = new WarehouseSlipRequest();
        disposalReq.setBatchId(damagedBatch.getId());
        disposalReq.setQuantity(10);

        MvcResult slipResult = mockMvc.perform(post("/api/warehouse/slips/disposal")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disposalReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andReturn();

        long slipId = objectMapper.readTree(slipResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // 2. Admin duyệt hủy lô thuốc (tìm đúng thông báo của phiếu hủy vừa tạo)
        MvcResult notifResult = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        var notifArray = objectMapper.readTree(notifResult.getResponse().getContentAsString()).get("data");
        long notifId = -1;
        for (var n : notifArray) {
            if ("DISPOSAL_CONFIRM".equals(n.get("type").asText()) && n.get("referenceId").asLong() == slipId) {
                notifId = n.get("id").asLong();
                break;
            }
        }
        assertTrue(notifId > 0, "Phải tìm thấy thông báo duyệt phiếu hủy");

        NotificationActionRequest approveAction = new NotificationActionRequest("ACCEPT");
        mockMvc.perform(post("/api/notifications/" + notifId + "/action")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveAction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("hủy thành công")));

        // 3. Kiểm tra lô thuốc đã được chuyển trạng thái CANCELLED (xóa mềm)
        Batch batchAfter = batchRepository.findById(damagedBatch.getId()).orElseThrow();
        assertEquals(BatchStatus.CANCELLED, batchAfter.getStatus());
        assertEquals(0, batchAfter.getQuantity());
    }

    @Test
    @DisplayName("Test 2b: Hủy lô thuốc bình thường không cần ràng buộc hết hạn hay hỏng, chỉ cần không vượt tồn")
    void testDisposalWithoutExpiryOrDamagedConstraint() throws Exception {
        String warehouseToken = getAuthToken("kho1", "12345");

        Product sampleProduct = productRepository.findAll().stream().findFirst().orElseThrow();
        // Lô bình thường còn hạn 2 năm, không bị hỏng
        Batch normalBatch = new Batch(
                sampleProduct,
                LocalDate.now(),
                LocalDate.now().plusYears(2),
                20,
                new BigDecimal("10000.00"),
                BatchStatus.AVAILABLE,
                false
        );
        normalBatch = batchRepository.save(normalBatch);

        // 1. Tạo phiếu hủy 5 sản phẩm (hợp lệ <= 20) -> Phải thành công trạng thái PENDING
        WarehouseSlipRequest validReq = new WarehouseSlipRequest();
        validReq.setBatchId(normalBatch.getId());
        validReq.setQuantity(5);

        mockMvc.perform(post("/api/warehouse/slips/disposal")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("PENDING")));

        // 2. Tạo phiếu hủy vượt quá số lượng tồn (ví dụ 50 > 20) -> Phải bị từ chối fail
        WarehouseSlipRequest exceedReq = new WarehouseSlipRequest();
        exceedReq.setBatchId(normalBatch.getId());
        exceedReq.setQuantity(50);

        mockMvc.perform(post("/api/warehouse/slips/disposal")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exceedReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Test 3: Thống kê Tài chính & Dòng tiền Admin")
    void testFinancialStatistics() throws Exception {
        String adminToken = getAuthToken("quan41144", "12345");

        mockMvc.perform(get("/api/admin/statistics?period=month")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.period", is("month")))
                .andExpect(jsonPath("$.data.totalRevenue", notNullValue()))
                .andExpect(jsonPath("$.data.totalImportExpense", notNullValue()))
                .andExpect(jsonPath("$.data.netProfit", notNullValue()))
                .andExpect(jsonPath("$.data.adminCurrentBalance", notNullValue()));
    }

    @Test
    @DisplayName("Test 4: Duyệt phiếu nhập bằng decision UI 'APPROVED' và từ chối bằng 'REJECTED'")
    void testImportSlipApprovedWithUIStringAndRejection() throws Exception {
        String warehouseToken = getAuthToken("kho1", "12345");
        String adminToken = getAuthToken("quan41144", "12345");
        Product product = productRepository.findAll().stream().findFirst().orElseThrow();

        // --- 1. Admin duyệt phiếu nhập với 'APPROVED' (chuỗi gửi từ UI web) ---
        WarehouseSlipRequest req1 = new WarehouseSlipRequest();
        req1.setProductId(product.getId());
        req1.setQuantity(10);
        req1.setMfgDate(LocalDate.now());
        req1.setExpDate(LocalDate.now().plusYears(1));

        MvcResult res1 = mockMvc.perform(post("/api/warehouse/slips/import")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andReturn();
        long slipId1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("data").get("id").asLong();

        // Lấy thông báo duyệt
        MvcResult notifRes1 = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        var notifs1 = objectMapper.readTree(notifRes1.getResponse().getContentAsString()).get("data");
        long notifId1 = -1;
        for (var n : notifs1) {
            if ("IMPORT_CONFIRM".equals(n.get("type").asText()) && n.get("referenceId").asLong() == slipId1) {
                notifId1 = n.get("id").asLong();
                break;
            }
        }
        assertTrue(notifId1 > 0);

        // Bấm 'APPROVED'
        mockMvc.perform(post("/api/notifications/" + notifId1 + "/action")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NotificationActionRequest("APPROVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Đã duyệt phiếu nhập")));

        WarehouseSlip approvedSlip = warehouseSlipRepository.findById(slipId1).orElseThrow();
        assertEquals(SlipStatus.APPROVED, approvedSlip.getStatus());
        assertEquals("true", approvedSlip.getResultState());
        assertEquals(BatchStatus.WAITING_FOR_PRICE, approvedSlip.getBatch().getStatus());

        // --- 2. Admin từ chối phiếu nhập với 'REJECTED' (chuỗi gửi từ UI web) ---
        WarehouseSlipRequest req2 = new WarehouseSlipRequest();
        req2.setProductId(product.getId());
        req2.setQuantity(5);
        req2.setMfgDate(LocalDate.now());
        req2.setExpDate(LocalDate.now().plusYears(1));

        MvcResult res2 = mockMvc.perform(post("/api/warehouse/slips/import")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andReturn();
        long slipId2 = objectMapper.readTree(res2.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult notifRes2 = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        var notifs2 = objectMapper.readTree(notifRes2.getResponse().getContentAsString()).get("data");
        long notifId2 = -1;
        for (var n : notifs2) {
            if ("IMPORT_CONFIRM".equals(n.get("type").asText()) && n.get("referenceId").asLong() == slipId2) {
                notifId2 = n.get("id").asLong();
                break;
            }
        }
        assertTrue(notifId2 > 0);

        // Bấm 'REJECTED'
        mockMvc.perform(post("/api/notifications/" + notifId2 + "/action")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NotificationActionRequest("REJECTED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("từ chối")));

        WarehouseSlip rejectedSlip = warehouseSlipRepository.findById(slipId2).orElseThrow();
        assertEquals(SlipStatus.REJECTED, rejectedSlip.getStatus());
        assertEquals("fail", rejectedSlip.getResultState());
        assertEquals(BatchStatus.REJECTED, rejectedSlip.getBatch().getStatus());
    }

    @Test
    @DisplayName("Test 5: Duyệt phiếu hủy bằng decision UI 'APPROVED' và từ chối bằng 'REJECTED'")
    void testDisposalSlipApprovedWithUIStringAndRejection() throws Exception {
        String warehouseToken = getAuthToken("kho1", "12345");
        String adminToken = getAuthToken("quan41144", "12345");
        Product product = productRepository.findAll().stream().findFirst().orElseThrow();

        // Tạo 2 lô hàng có sẵn
        Batch b1 = batchRepository.save(new Batch(product, LocalDate.now(), LocalDate.now().plusYears(1), 15, new BigDecimal("10000"), BatchStatus.AVAILABLE, false));
        Batch b2 = batchRepository.save(new Batch(product, LocalDate.now(), LocalDate.now().plusYears(1), 15, new BigDecimal("10000"), BatchStatus.AVAILABLE, false));

        // 1. Duyệt hủy b1 với 'APPROVED'
        WarehouseSlipRequest req1 = new WarehouseSlipRequest();
        req1.setBatchId(b1.getId());
        req1.setQuantity(5);
        MvcResult res1 = mockMvc.perform(post("/api/warehouse/slips/disposal")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andReturn();
        long slipId1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult notifRes1 = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        var notifs1 = objectMapper.readTree(notifRes1.getResponse().getContentAsString()).get("data");
        long notifId1 = -1;
        for (var n : notifs1) {
            if ("DISPOSAL_CONFIRM".equals(n.get("type").asText()) && n.get("referenceId").asLong() == slipId1) {
                notifId1 = n.get("id").asLong();
                break;
            }
        }
        assertTrue(notifId1 > 0);

        mockMvc.perform(post("/api/notifications/" + notifId1 + "/action")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NotificationActionRequest("APPROVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("hủy thành công")));

        Batch updatedB1 = batchRepository.findById(b1.getId()).orElseThrow();
        assertEquals(10, updatedB1.getQuantity());

        // 2. Từ chối hủy b2 với 'REJECTED'
        WarehouseSlipRequest req2 = new WarehouseSlipRequest();
        req2.setBatchId(b2.getId());
        req2.setQuantity(5);
        MvcResult res2 = mockMvc.perform(post("/api/warehouse/slips/disposal")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andReturn();
        long slipId2 = objectMapper.readTree(res2.getResponse().getContentAsString()).get("data").get("id").asLong();

        MvcResult notifRes2 = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        var notifs2 = objectMapper.readTree(notifRes2.getResponse().getContentAsString()).get("data");
        long notifId2 = -1;
        for (var n : notifs2) {
            if ("DISPOSAL_CONFIRM".equals(n.get("type").asText()) && n.get("referenceId").asLong() == slipId2) {
                notifId2 = n.get("id").asLong();
                break;
            }
        }
        assertTrue(notifId2 > 0);

        mockMvc.perform(post("/api/notifications/" + notifId2 + "/action")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NotificationActionRequest("REJECTED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("từ chối")));

        WarehouseSlip rejectedSlip = warehouseSlipRepository.findById(slipId2).orElseThrow();
        assertEquals(SlipStatus.REJECTED, rejectedSlip.getStatus());
        Batch updatedB2 = batchRepository.findById(b2.getId()).orElseThrow();
        assertEquals(15, updatedB2.getQuantity()); // Số lượng nguyên vẹn
    }

    @Test
    @DisplayName("Test 6: Duyệt phiếu trực tiếp qua endpoint /api/warehouse/slips/{id}/action")
    void testDirectSlipActionEndpoint() throws Exception {
        String warehouseToken = getAuthToken("kho1", "12345");
        String adminToken = getAuthToken("quan41144", "12345");
        Product product = productRepository.findAll().stream().findFirst().orElseThrow();

        WarehouseSlipRequest req = new WarehouseSlipRequest();
        req.setProductId(product.getId());
        req.setQuantity(8);
        req.setMfgDate(LocalDate.now());
        req.setExpDate(LocalDate.now().plusYears(1));

        MvcResult res = mockMvc.perform(post("/api/warehouse/slips/import")
                        .header("Authorization", "Bearer " + warehouseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        long slipId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();

        // Admin trực tiếp duyệt slip
        mockMvc.perform(post("/api/warehouse/slips/" + slipId + "/action")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NotificationActionRequest("APPROVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        WarehouseSlip directApprovedSlip = warehouseSlipRepository.findById(slipId).orElseThrow();
        assertEquals(SlipStatus.APPROVED, directApprovedSlip.getStatus());
    }
}
