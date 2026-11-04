package com.pharmacy.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {
    private Long productId;
    private Long categoryId;
    private String categoryName;
    private String productName;
    private String dosage;
    private String uses;
    private String dosageForm;
    private String unit;
    private BigDecimal sellingPrice;
    private String imageUrl;
    private Integer totalStock;
    private String stockStatus; // CÒN HÀNG / HẾT HÀNG
}
