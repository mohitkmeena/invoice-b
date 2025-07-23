package com.invoicefinancing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "investments")
public class Investment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investor_id", nullable = false)
    private User investor;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal amount;

    @Column(name = "expected_return")
    private BigDecimal expectedReturn;

    @Enumerated(EnumType.STRING)
    private InvestmentStatus status = InvestmentStatus.ACTIVE;

    @Column(name = "investment_date")
    private LocalDate investmentDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "actual_return")
    private BigDecimal actualReturn;

    @Column(name = "repaid_at")
    private LocalDateTime repaidAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Constructors
    public Investment() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }

    public User getInvestor() { return investor; }
    public void setInvestor(User investor) { this.investor = investor; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getExpectedReturn() { return expectedReturn; }
    public void setExpectedReturn(BigDecimal expectedReturn) { this.expectedReturn = expectedReturn; }

    public InvestmentStatus getStatus() { return status; }
    public void setStatus(InvestmentStatus status) { this.status = status; }

    public LocalDate getInvestmentDate() { return investmentDate; }
    public void setInvestmentDate(LocalDate investmentDate) { this.investmentDate = investmentDate; }

    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }

    public BigDecimal getActualReturn() { return actualReturn; }
    public void setActualReturn(BigDecimal actualReturn) { this.actualReturn = actualReturn; }

    public LocalDateTime getRepaidAt() { return repaidAt; }
    public void setRepaidAt(LocalDateTime repaidAt) { this.repaidAt = repaidAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // Helper method to get duration days from invoice
    public Integer getDurationDays() {
        return this.invoice != null ? this.invoice.getDurationDays() : null;
    }

    public enum InvestmentStatus {
        ACTIVE, COMPLETED, OVERDUE
    }
}