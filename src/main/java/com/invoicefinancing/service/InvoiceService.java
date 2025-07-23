package com.invoicefinancing.service;

import com.invoicefinancing.entity.Invoice;
import com.invoicefinancing.entity.Investment;
import com.invoicefinancing.entity.User;
import com.invoicefinancing.repository.InvoiceRepository;
import com.invoicefinancing.repository.InvestmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private NotificationService notificationService;

    public Invoice createInvoice(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }

    public Optional<Invoice> getInvoiceById(Long id) {
        return invoiceRepository.findById(id);
    }

    public List<Invoice> getInvoicesByMsme(User msme) {
        return invoiceRepository.findByMsme(msme);
    }

    public List<Invoice> getApprovedInvoices() {
        return invoiceRepository.findApprovedInvoicesForInvestment();
    }

    public Invoice updateInvoice(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }

    public void deleteInvoice(Long id) {
        invoiceRepository.deleteById(id);
    }

    public Invoice approveInvoice(Long invoiceId, User approvedBy) {
        Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
        if (invoiceOpt.isPresent()) {
            Invoice invoice = invoiceOpt.get();
            invoice.setStatus(Invoice.InvoiceStatus.APPROVED);
            invoice.setApprovedAt(LocalDateTime.now());
            invoice.setApprovedBy(approvedBy);
            
            Invoice savedInvoice = invoiceRepository.save(invoice);
            
            // Send notification to MSME
            notificationService.createNotification(
                invoice.getMsme(),
                "Invoice Approved",
                "Your invoice " + invoice.getInvoiceNumber() + " has been approved and is now available for investment.",
                "SUCCESS"
            );
            
            return savedInvoice;
        }
        return null;
    }

    public Invoice rejectInvoice(Long invoiceId, String reason) {
        Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
        if (invoiceOpt.isPresent()) {
            Invoice invoice = invoiceOpt.get();
            invoice.setStatus(Invoice.InvoiceStatus.REJECTED);
            
            Invoice savedInvoice = invoiceRepository.save(invoice);
            
            // Send notification to MSME
            notificationService.createNotification(
                invoice.getMsme(),
                "Invoice Rejected",
                "Your invoice " + invoice.getInvoiceNumber() + " has been rejected. Reason: " + reason,
                "ERROR"
            );
            
            return savedInvoice;
        }
        return null;
    }

    public void updateFundingProgress(Long invoiceId, BigDecimal additionalFunding) {
        Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId);
        if (invoiceOpt.isPresent()) {
            Invoice invoice = invoiceOpt.get();
            invoice.setFundingProgress(invoice.getFundingProgress().add(additionalFunding));
            
            // Check if fully funded
            if (invoice.getFundingProgress().compareTo(invoice.getAmount()) >= 0) {
                invoice.setStatus(Invoice.InvoiceStatus.FUNDED);
                
                // Send notification to MSME
                notificationService.createNotification(
                    invoice.getMsme(),
                    "Invoice Fully Funded",
                    "Your invoice " + invoice.getInvoiceNumber() + " has been fully funded!",
                    "SUCCESS"
                );
            }
            
            invoiceRepository.save(invoice);
        }
    }

    public List<Invoice> getPendingInvoices() {
        return invoiceRepository.findByStatus(Invoice.InvoiceStatus.PENDING);
    }

    public BigDecimal getTotalInvoiceValue() {
        return invoiceRepository.findAll().stream()
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalFundedAmount() {
        return invoiceRepository.findAll().stream()
                .map(Invoice::getFundingProgress)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}