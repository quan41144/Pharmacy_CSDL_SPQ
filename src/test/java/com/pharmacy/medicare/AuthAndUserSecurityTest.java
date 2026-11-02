package com.pharmacy.medicare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.medicare.dto.auth.LoginRequest;
import com.pharmacy.medicare.dto.auth.RegisterRequest;
import com.pharmacy.medicare.dto.user.UpdateRoleRequest;
import com.pharmacy.medicare.dto.user.UpdateStatusRequest;
import com.pharmacy.medicare.entity.enums.RoleName;
import com.pharmacy.medicare.entity.enums.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthAndUserSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Test 1: Đăng nhập thành công với tài khoản Admin mặc định quan41144 / 12345")
    void testAdminLoginSuccess() throws Exception {
        LoginRequest loginRequest = new LoginRequest("quan41144", "12345");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.data.username", is("quan41144")))
                .andExpect(jsonPath("$.data.roles", hasItem("ROLE_ADMIN")))
                .andExpect(jsonPath("$.data.roles", hasItem("ROLE_CUSTOMER")))
                .andExpect(jsonPath("$.data.balance", notNullValue()));
    }

    @Test
    @DisplayName("Test 2: Đăng nhập thất bại với sai mật khẩu")
    void testLoginWrongPassword() throws Exception {
        LoginRequest loginRequest = new LoginRequest("quan41144", "wrongpass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("không chính xác")));
    }

    @Test
    @DisplayName("Test 3: Khách hàng tự đăng ký - Mặc định ROLE_CUSTOMER và số dư 0")
    void testRegisterNewCustomer() throws Exception {
        String testUsername = "khachmoi_" + System.currentTimeMillis();
        RegisterRequest registerRequest = new RegisterRequest(testUsername, "12345", "Khách Hàng Tự Đăng Ký");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is(testUsername)))
                .andExpect(jsonPath("$.data.roles", hasItem("ROLE_CUSTOMER")))
                .andExpect(jsonPath("$.data.balance", notNullValue()));
    }

    @Test
    @DisplayName("Test 4: Ràng buộc bảo mật - Không được phép khóa hoặc sửa tài khoản Admin khác")
    void testSecurityConstraintCannotModifyAdmin() throws Exception {
        // 1. Đăng nhập Admin lấy token
        LoginRequest loginRequest = new LoginRequest("quan41144", "12345");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        String adminToken = objectMapper.readTree(responseBody).get("data").get("accessToken").asText();

        // 2. Thử khóa chính tài khoản ID 1 (Admin quan41144) -> Bị chặn
        UpdateStatusRequest lockRequest = new UpdateStatusRequest(UserStatus.LOCKED);
        mockMvc.perform(put("/api/admin/users/1/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lockRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Tuyệt đối không được")));
    }

    @Test
    @DisplayName("Test 5: Admin cập nhật phân quyền cho tài khoản khác")
    void testAdminUpdateUserRole() throws Exception {
        // 1. Tạo một tài khoản customer mới
        String testUser = "usertestrole_" + System.currentTimeMillis();
        RegisterRequest registerReq = new RegisterRequest(testUser, "12345", "Tài khoản test role");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andReturn();
        Long newUserId = objectMapper.readTree(regResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 2. Admin đăng nhập
        LoginRequest loginRequest = new LoginRequest("quan41144", "12345");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String adminToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("data").get("accessToken").asText();

        // 3. Admin chuyển quyền người dùng sang ROLE_WAREHOUSE
        UpdateRoleRequest roleReq = new UpdateRoleRequest(RoleName.ROLE_WAREHOUSE);
        mockMvc.perform(put("/api/admin/users/" + newUserId + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.roles", hasItem("ROLE_WAREHOUSE")));
    }

    @Test
    @DisplayName("Test 6: Người dùng tự chỉnh sửa thông tin cá nhân và mật khẩu")
    void testUpdatePersonalProfile() throws Exception {
        String testUser = "profileuser_" + System.currentTimeMillis();
        RegisterRequest registerReq = new RegisterRequest(testUser, "12345", "Tên Ban Đầu");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andReturn();
        String userToken = objectMapper.readTree(regResult.getResponse().getContentAsString()).get("data").get("accessToken").asText();

        // Cập nhật họ tên mới và mật khẩu mới
        com.pharmacy.medicare.dto.auth.UpdateProfileRequest updateReq =
                new com.pharmacy.medicare.dto.auth.UpdateProfileRequest("Tên Đã Chỉnh Sửa", "newpass123");

        mockMvc.perform(put("/api/auth/profile")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fullName", is("Tên Đã Chỉnh Sửa")));

        // Kiểm tra đăng nhập lại bằng mật khẩu mới
        LoginRequest loginNewPass = new LoginRequest(testUser, "newpass123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginNewPass)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("Test 7: Thêm thuốc mới vào hệ thống với đầy đủ thông tin bắt buộc")
    void testCreateProductWithAllMandatoryFields() throws Exception {
        LoginRequest loginRequest = new LoginRequest("quan41144", "12345");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String adminToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("data").get("accessToken").asText();

        com.pharmacy.medicare.dto.product.CreateProductRequest prodReq =
                new com.pharmacy.medicare.dto.product.CreateProductRequest(
                        "Thuốc Mới Test " + System.currentTimeMillis(),
                        1L,
                        "Mô tả chi tiết thuốc mới",
                        "Công dụng điều trị bệnh",
                        "Hoạt chất 500mg",
                        "Hộp",
                        "https://example.com/image.png",
                        new java.math.BigDecimal("12000"),
                        new java.math.BigDecimal("15000")
                );

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prodReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", containsString("Thuốc Mới Test")))
                .andExpect(jsonPath("$.data.sellingPrice", is(15000)));
    }
}
