package com.pharmacy.controller;

import com.pharmacy.dto.ApiResponse;
import com.pharmacy.dto.BatchInfoDTO;
import com.pharmacy.service.BatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    @GetMapping("/fefo")
    public ResponseEntity<ApiResponse<List<BatchInfoDTO>>> getBatchesForSaleFEFO(
            @RequestParam Long warehouseId,
            @RequestParam Long productId
    ) {
        List<BatchInfoDTO> list = batchService.getBatchesForSaleFEFO(warehouseId, productId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/search-disposal")
    public ResponseEntity<ApiResponse<List<BatchInfoDTO>>> searchBatchesForDisposal(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long warehouseId
    ) {
        List<BatchInfoDTO> list = batchService.searchBatchesForDisposal(keyword, warehouseId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }
}
