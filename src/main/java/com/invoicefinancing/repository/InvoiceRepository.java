package com.invoicefinancing.repository;

import com.invoicefinancing.entity.Invoice;
import com.invoicefinancing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByMsme(User msme);
    List<Invoice> findByStatus(Invoice.InvoiceStatus status);
    
    @Query("SELECT i FROM Invoice i WHERE i.status = 'APPROVED' ORDER BY i.createdAt DESC")
    List<Invoice> findApprovedInvoicesForInvestment();
    
    @Query("SELECT i FROM Invoice i WHERE i.msme.id = ?1 ORDER BY i.createdAt DESC")
    List<Invoice> findByMsmeIdOrderByCreatedAtDesc(Long msmeId);
}