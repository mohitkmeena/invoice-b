package com.invoicefinancing.repository;

import com.invoicefinancing.entity.Investment;
import com.invoicefinancing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    List<Investment> findByInvestor(User investor);
    List<Investment> findByInvoiceId(Long invoiceId);
    
    @Query("SELECT i FROM Investment i WHERE i.investor.id = ?1 ORDER BY i.createdAt DESC")
    List<Investment> findByInvestorIdOrderByCreatedAtDesc(Long investorId);
    
    @Query("SELECT i FROM Investment i WHERE i.maturityDate < CURRENT_DATE AND i.status = 'ACTIVE'")
    List<Investment> findOverdueInvestments();
    
    default Integer getDurationDaysForInvestment(Long investmentId) {
        return findById(investmentId)
                .map(investment -> investment.getInvoice().getDurationDays())
                .orElse(null);
    }
}