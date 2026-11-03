package com.pharmacy.repository;

import com.pharmacy.entity.DisposalSlip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisposalSlipRepository extends JpaRepository<DisposalSlip, Long> {
    List<DisposalSlip> findAllByOrderByDisposalSlipIdDesc();

    List<DisposalSlip> findByStatusOrderByDisposalSlipIdDesc(String status);
}
