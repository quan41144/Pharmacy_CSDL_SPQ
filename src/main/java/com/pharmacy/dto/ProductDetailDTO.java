package com.pharmacy.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailDTO {
    private ProductDTO product;
    private List<BatchInfoDTO> batches;
}
