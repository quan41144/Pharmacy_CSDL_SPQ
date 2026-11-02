package com.pharmacy.medicare.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File uploadDir = new File("uploads/products");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
