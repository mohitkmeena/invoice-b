package com.invoicefinancing.service;

import com.invoicefinancing.entity.Investment;
import com.invoicefinancing.entity.Invoice;
import com.invoicefinancing.entity.User;
import com.invoicefinancing.entity.Transaction;
import com.invoicefinancing.repository.InvestmentRepository;
import com.invoicefinancing.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InvestmentService {

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private NotificationService notificationService;

    public Investment createInvestment(Investment investment) {
        Investment savedInvestment = investmentRepository.save(investment);
        
        // Update invoice funding progress
        invoiceService.updateFundingProgress(investment.getInvoice().getId(), investment.getAmount());
        
        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN_" + System.currentTimeMillis());
        transaction.setUser(investment.getInvestor());
        transaction.setInvestment(savedInvestment);
        transaction.setAmount(investment.getAmount());
        transaction.setType(Transaction.TransactionType.INVESTMENT);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);
        
        // Send notifications
        notificationService.createNotification(
            investment.getInvestor(),
            "Investment Successful",
            "Your investment of ₹" + investment.getAmount() + " has been successfully made.",
            "SUCCESS"
        );
        
        notificationService.createNotification(
            investment.getInvoice().getMsme(),
            "New Investment Received",
            "Your invoice " + investment.getInvoice().getInvoiceNumber() + " received an investment of ₹" + investment.getAmount(),
            "INFO"
        );
        
        return savedInvestment;
    }

    public Optional<Investment> getInvestmentById(Long id) {
        return investmentRepository.findById(id);
    }

    public List<Investment> getInvestmentsByInvestor(User investor) {
        return investmentRepository.findByInvestor(investor);
    }

    public List<Investment> getInvestmentsByInvoice(Long invoiceId) {
        return investmentRepository.findByInvoiceId(invoiceId);
    }

    public Investment completeInvestment(Long investmentId, BigDecimal actualReturn) {
        Optional<Investment> investmentOpt = investmentRepository.findById(investmentId);
        if (investmentOpt.isPresent()) {
            Investment investment = investmentOpt.get();
            investment.setStatus(Investment.InvestmentStatus.COMPLETED);
            investment.setActualReturn(actualReturn);
            investment.setRepaidAt(LocalDateTime.now());
            
            Investment savedInvestment = investmentRepository.save(investment);
            
            // Create repayment transaction
            Transaction transaction = new Transaction();
            transaction.setTransactionId("TXN_" + System.currentTimeMillis());
            transaction.setUser(investment.getInvestor());
            transaction.setInvestment(savedInvestment);
            transaction.setAmount(actualReturn);
            transaction.setType(Transaction.TransactionType.REPAYMENT);
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            transactionRepository.save(transaction);
            
            // Send notification
            notificationService.createNotification(
                investment.getInvestor(),
                "Investment Completed",
                "Your investment has been completed with a return of ₹" + actualReturn,
                "SUCCESS"
            );
            
            return savedInvestment;
        }
        return null;
    }

    public void markOverdueInvestments() {
        List<Investment> overdueInvestments = investmentRepository.findOverdueInvestments();
        for (Investment investment : overdueInvestments) {
            investment.setStatus(Investment.InvestmentStatus.OVERDUE);
            investmentRepository.save(investment);
            
            // Send notification
            notificationService.createNotification(
                investment.getInvestor(),
                "Investment Overdue",
                "Your investment in invoice " + investment.getInvoice().getInvoiceNumber() + " is now overdue.",
                "WARNING"
            );
        }
    }

    public BigDecimal getTotalInvestedAmount() {
        return investmentRepository.findAll().stream()
                .map(Investment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalExpectedReturns() {
        return investmentRepository.findAll().stream()
                .map(inv -> inv.getExpectedReturn() != null ? inv.getExpectedReturn() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalActualReturns() {
        return investmentRepository.findAll().stream()
                .map(inv -> inv.getActualReturn() != null ? inv.getActualReturn() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}