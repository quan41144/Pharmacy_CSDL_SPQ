package com.pharmacy.repository;

import com.pharmacy.entity.InvoiceItem;
import com.pharmacy.entity.InvoiceItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, InvoiceItemId> {
    List<InvoiceItem> findByInvoiceInvoiceId(Long invoiceId);
}
