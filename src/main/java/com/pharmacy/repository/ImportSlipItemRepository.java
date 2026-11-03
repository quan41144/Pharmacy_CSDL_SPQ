package com.pharmacy.repository;

import com.pharmacy.entity.ImportSlipItem;
import com.pharmacy.entity.ImportSlipItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportSlipItemRepository extends JpaRepository<ImportSlipItem, ImportSlipItemId> {
    List<ImportSlipItem> findByImportSlipImportSlipId(Long importSlipId);
}
