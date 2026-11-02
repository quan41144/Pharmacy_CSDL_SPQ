package com.pharmacy.medicare.config;

import com.pharmacy.medicare.security.CustomAccessDeniedHandler;
import com.pharmacy.medicare.security.CustomUserDetailsService;
import com.pharmacy.medicare.security.JwtAuthenticationEntryPoint;
import com.pharmacy.medicare.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          CustomUserDetailsService userDetailsService,
                          JwtAuthenticationEntryPoint authenticationEntryPoint,
                          CustomAccessDeniedHandler accessDeniedHandler,
                          PasswordEncoder passwordEncoder) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // Cho phép truy cập tĩnh và trang chủ
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/static/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico"
                        ).permitAll()
                        // Endpoint Xác thực công khai
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh").permitAll()
                        // Khách vãng lai và mọi người đều xem được danh mục & sản phẩm & nhân viên bán hàng
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**", "/api/sales-staff").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasAnyRole("WAREHOUSE", "ADMIN")
                        // Phân hệ Admin
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Phân hệ Nhân viên Bán hàng (Admin cũng có quyền truy cập)
                        .requestMatchers("/api/sales/**").hasAnyRole("SALES", "ADMIN")
                        // Phân hệ Nhân viên Kho (Admin cũng có quyền truy cập)
                        .requestMatchers("/api/warehouse/**").hasAnyRole("WAREHOUSE", "ADMIN")
                        // Mọi yêu cầu còn lại đều cần đăng nhập
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Cho phép frontend truy cập từ localhost hoặc mở trực tiếp từ trình duyệt
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
