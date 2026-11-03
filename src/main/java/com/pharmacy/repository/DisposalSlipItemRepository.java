package com.pharmacy.repository;

import com.pharmacy.entity.DisposalSlipItem;
import com.pharmacy.entity.DisposalSlipItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisposalSlipItemRepository extends JpaRepository<DisposalSlipItem, DisposalSlipItemId> {
    List<DisposalSlipItem> findByDisposalSlipDisposalSlipId(Long disposalSlipId);
}
