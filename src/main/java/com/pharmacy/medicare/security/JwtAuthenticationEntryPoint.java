package com.pharmacy.medicare.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.medicare.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> apiResponse = ApiResponse.error("Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại!");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
