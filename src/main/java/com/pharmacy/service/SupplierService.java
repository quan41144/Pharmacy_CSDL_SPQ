package com.pharmacy.service;

import com.pharmacy.dto.SupplierDTO;
import com.pharmacy.entity.Supplier;
import com.pharmacy.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public List<SupplierDTO> liveSearch(String keyword) {
        return supplierRepository.liveSearchSuppliers(keyword).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<SupplierDTO> getAllSuppliers() {
        return supplierRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional
    public SupplierDTO saveSupplier(SupplierDTO dto) {
        Supplier supplier;
        if (dto.getSupplierId() != null) {
            supplier = supplierRepository.findById(dto.getSupplierId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà cung cấp"));
        } else {
            supplier = new Supplier();
        }

        supplier.setSupplierName(dto.getSupplierName());
        supplier.setAddress(dto.getAddress());
        supplier.setTaxCode(dto.getTaxCode());
        supplier.setRepresentativeName(dto.getRepresentativeName());
        supplier.setPhone(dto.getPhone());

        Supplier saved = supplierRepository.save(supplier);
        return toDTO(saved);
    }

    @Transactional
    public void deleteSupplier(Long id) {
        supplierRepository.deleteById(id);
    }

    private SupplierDTO toDTO(Supplier s) {
        return SupplierDTO.builder()
                .supplierId(s.getSupplierId())
                .supplierName(s.getSupplierName())
                .address(s.getAddress())
                .taxCode(s.getTaxCode())
                .representativeName(s.getRepresentativeName())
                .phone(s.getPhone())
                .build();
    }
}
