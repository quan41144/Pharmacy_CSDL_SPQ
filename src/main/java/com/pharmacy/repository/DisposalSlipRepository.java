package com.pharmacy.repository;

import com.pharmacy.entity.DisposalSlip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisposalSlipRepository extends JpaRepository<DisposalSlip, Long> {
    List<DisposalSlip> findAllByOrderByDisposalSlipIdDesc();

    List<DisposalSlip> findByStatusOrderByDisposalSlipIdDesc(String status);

    @Query("SELECT COUNT(d) > 0 FROM DisposalSlip d WHERE d.warehouse.warehouseId = :warehouseId")
    boolean existsByWarehouseId(@Param("warehouseId") Long warehouseId);
}
