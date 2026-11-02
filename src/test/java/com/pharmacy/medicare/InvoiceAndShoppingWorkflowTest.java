package com.pharmacy.medicare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.medicare.dto.auth.LoginRequest;
import com.pharmacy.medicare.dto.invoice.CreateInvoiceRequest;
import com.pharmacy.medicare.dto.invoice.InvoiceItemRequest;
import com.pharmacy.medicare.entity.Product;
import com.pharmacy.medicare.entity.User;
import com.pharmacy.medicare.entity.enums.PaymentMethod;
import com.pharmacy.medicare.entity.enums.RoleName;
import com.pharmacy.medicare.entity.enums.UserStatus;
import com.pharmacy.medicare.repository.ProductRepository;
import com.pharmacy.medicare.repository.RoleRepository;
import com.pharmacy.medicare.repository.UserRepository;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InvoiceAndShoppingWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

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
            if ("khachhang1".equals(username)) {
                u.setBalance(new BigDecimal("5000000.00"));
                u.setRoles(new HashSet<>(Collections.singletonList(roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow())));
            } else if ("banhang1".equals(username)) {
                u.setBalance(BigDecimal.ZERO);
                u.setRoles(new HashSet<>(Arrays.asList(
                        roleRepository.findByName(RoleName.ROLE_SALES).orElseThrow(),
                        roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElseThrow()
                )));
            }
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
    @DisplayName("Test 1: Khách hàng xem danh sách sản phẩm và Live Search")
    void testGetProductsAndLiveSearch() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/api/products/live-search?q=Panadol"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].name", containsString("Panadol")));
    }

    @Test
    @DisplayName("Test 2: Luồng hoàn chỉnh Mua hàng bằng Số dư -> Nhân viên Bán hàng xác nhận -> Trừ tồn kho & Cộng tiền cho Admin")
    void testFullPurchaseAndConfirmationWorkflow() throws Exception {
        String customerToken = getAuthToken("khachhang1", "12345");
        String salesToken = getAuthToken("banhang1", "12345");

        Product sampleProduct = productRepository.findAll().stream()
                .filter(p -> p.getTotalStock() >= 5)
                .findFirst()
                .orElseThrow();
        int initialStock = sampleProduct.getTotalStock();

        User adminBefore = userRepository.findByUsername("quan41144").orElseThrow();
        BigDecimal adminInitialBalance = adminBefore.getBalance();

        // 1. Khách hàng tạo hóa đơn mua 2 sản phẩm bằng SYSTEM_BALANCE
        CreateInvoiceRequest createReq = new CreateInvoiceRequest();
        createReq.setPaymentMethod(PaymentMethod.SYSTEM_BALANCE);
        createReq.setItems(Collections.singletonList(new InvoiceItemRequest(sampleProduct.getId(), 2)));

        MvcResult invoiceResult = mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("UNPAID")))
                .andExpect(jsonPath("$.data.resultState", is("pending")))
                .andExpect(jsonPath("$.data.remainingSeconds", greaterThan(0)))
                .andReturn();

        long invoiceId = objectMapper.readTree(invoiceResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // 2. Nhân viên bán hàng xác nhận hóa đơn
        mockMvc.perform(post("/api/sales/invoices/" + invoiceId + "/confirm")
                        .header("Authorization", "Bearer " + salesToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.resultState", is("true")))
                .andExpect(jsonPath("$.data.invoice.status", is("PAID")));

        // 3. Kiểm tra tồn kho sản phẩm đã giảm 2
        Product updatedProduct = productRepository.findById(sampleProduct.getId()).orElseThrow();
        assertEquals(initialStock - 2, updatedProduct.getTotalStock());

        // 4. Kiểm tra số dư Admin đã được cộng tiền bán hàng
        User adminAfter = userRepository.findByUsername("quan41144").orElseThrow();
        BigDecimal expectedTotal = sampleProduct.getSellingPrice().multiply(BigDecimal.valueOf(2));
        assertEquals(adminInitialBalance.add(expectedTotal), adminAfter.getBalance());

        // 5. In hóa đơn PDF định dạng PHARMACY 80mm bằng Header Authorization
        byte[] pdfBytes = mockMvc.perform(get("/api/invoices/" + invoiceId + "/pdf")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", is("application/pdf")))
                .andReturn().getResponse().getContentAsByteArray();

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "File PDF phải có dung lượng hợp lệ (>500 bytes)");

        // 6. Test in hóa đơn PDF qua query parameter token (window.open trong trình duyệt không bị 401)
        byte[] pdfBytesFromParam = mockMvc.perform(get("/api/invoices/" + invoiceId + "/pdf")
                        .param("token", customerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", is("application/pdf")))
                .andReturn().getResponse().getContentAsByteArray();

        assertNotNull(pdfBytesFromParam);
        assertTrue(pdfBytesFromParam.length > 500, "File PDF truy cập qua URL token parameter phải hợp lệ");
    }

    @Test
    @DisplayName("Test 3: Khách hàng chủ động hủy hóa đơn chưa thanh toán -> Chuyển CANCELLED & resultState fail")
    void testCancelUnpaidInvoice() throws Exception {
        String customerToken = getAuthToken("khachhang1", "12345");
        Product sampleProduct = productRepository.findAll().stream().findFirst().orElseThrow();

        // 1. Tạo hóa đơn
        CreateInvoiceRequest createReq = new CreateInvoiceRequest();
        createReq.setPaymentMethod(PaymentMethod.CASH);
        createReq.setItems(Collections.singletonList(new InvoiceItemRequest(sampleProduct.getId(), 1)));

        MvcResult invoiceResult = mockMvc.perform(post("/api/invoices")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn();

        long invoiceId = objectMapper.readTree(invoiceResult.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        // 2. Khách bấm nút Hủy hóa đơn
        mockMvc.perform(post("/api/invoices/" + invoiceId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("CANCELLED")))
                .andExpect(jsonPath("$.data.resultState", is("fail")));

        // 3. Quy tắc nghiệp vụ: Chỉ in hóa đơn khi đã thanh toán thành công (PAID), chưa thanh toán/đã hủy không được in
        mockMvc.perform(get("/api/invoices/" + invoiceId + "/pdf")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
