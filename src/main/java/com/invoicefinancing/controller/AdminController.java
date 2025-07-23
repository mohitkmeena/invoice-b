package com.invoicefinancing.controller;

import com.invoicefinancing.entity.*;
import com.invoicefinancing.repository.*;
import com.invoicefinancing.service.KYCService;
import com.invoicefinancing.service.InvoiceService;
import com.invoicefinancing.security.UserPrincipal;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KYCDocumentRepository kycDocumentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private KYCService kycService;

    @Autowired
    private InvoiceService invoiceService;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        long totalUsers = userRepository.count();
        long totalMSMEs = userRepository.findByUserType(User.UserType.MSME).size();
        long totalInvestors = userRepository.findByUserType(User.UserType.INVESTOR).size();
        long pendingKYC = userRepository.findByKycStatus(User.KYCStatus.PENDING).size();
        long approvedKYC = userRepository.findByKycStatus(User.KYCStatus.APPROVED).size();
        long rejectedKYC = userRepository.findByKycStatus(User.KYCStatus.REJECTED).size();
        
        long totalInvoices = invoiceRepository.count();
        long pendingInvoices = invoiceRepository.findByStatus(Invoice.InvoiceStatus.PENDING).size();
        long approvedInvoices = invoiceRepository.findByStatus(Invoice.InvoiceStatus.APPROVED).size();
        long fundedInvoices = invoiceRepository.findByStatus(Invoice.InvoiceStatus.FUNDED).size();
        long completedInvoices = invoiceRepository.findByStatus(Invoice.InvoiceStatus.COMPLETED).size();
        long rejectedInvoices = invoiceRepository.findByStatus(Invoice.InvoiceStatus.REJECTED).size();
        
        long totalInvestments = investmentRepository.count();
        long activeInvestments = investmentRepository.findAll().stream()
                .filter(inv -> inv.getStatus() == Investment.InvestmentStatus.ACTIVE)
                .count();
        
        // Calculate financial metrics
        List<Invoice> allInvoices = invoiceRepository.findAll();
        BigDecimal totalInvoiceValue = allInvoices.stream()
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        List<Investment> allInvestments = investmentRepository.findAll();
        BigDecimal totalInvestedAmount = allInvestments.stream()
                .map(Investment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalExpectedReturns = allInvestments.stream()
                .map(inv -> inv.getExpectedReturn() != null ? inv.getExpectedReturn() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate platform metrics
        double kycApprovalRate = totalUsers > 0 ? (approvedKYC * 100.0 / totalUsers) : 0;
        double invoiceApprovalRate = totalInvoices > 0 ? ((approvedInvoices + fundedInvoices + completedInvoices) * 100.0 / totalInvoices) : 0;
        double platformUtilization = totalInvoiceValue.compareTo(BigDecimal.ZERO) > 0 ? 
                totalInvestedAmount.multiply(BigDecimal.valueOf(100)).divide(totalInvoiceValue, 2, BigDecimal.ROUND_HALF_UP).doubleValue() : 0;

        Map<String, Object> dashboard = new HashMap<>();
        
        // User Statistics
        dashboard.put("totalUsers", totalUsers);
        dashboard.put("totalMSMEs", totalMSMEs);
        dashboard.put("totalInvestors", totalInvestors);
        dashboard.put("pendingKYC", pendingKYC);
        dashboard.put("approvedKYC", approvedKYC);
        dashboard.put("rejectedKYC", rejectedKYC);
        dashboard.put("kycApprovalRate", kycApprovalRate);
        
        // Invoice Statistics
        dashboard.put("totalInvoices", totalInvoices);
        dashboard.put("pendingInvoices", pendingInvoices);
        dashboard.put("approvedInvoices", approvedInvoices);
        dashboard.put("fundedInvoices", fundedInvoices);
        dashboard.put("completedInvoices", completedInvoices);
        dashboard.put("rejectedInvoices", rejectedInvoices);
        dashboard.put("invoiceApprovalRate", invoiceApprovalRate);
        dashboard.put("totalInvoiceValue", totalInvoiceValue);
        
        // Investment Statistics
        dashboard.put("totalInvestments", totalInvestments);
        dashboard.put("activeInvestments", activeInvestments);
        dashboard.put("totalInvestedAmount", totalInvestedAmount);
        dashboard.put("totalExpectedReturns", totalExpectedReturns);
        dashboard.put("platformUtilization", platformUtilization);
        
        // Platform Health Metrics
        dashboard.put("platformHealth", calculatePlatformHealth(totalUsers, approvedKYC, totalInvestments, activeInvestments));
        dashboard.put("averageInvestmentSize", totalInvestments > 0 ? 
                totalInvestedAmount.divide(BigDecimal.valueOf(totalInvestments), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO);
        
        dashboard.put("lastUpdated", java.time.LocalDateTime.now());

        return ResponseEntity.ok(dashboard);
    }
    
    private String calculatePlatformHealth(long totalUsers, long verifiedUsers, long totalInvestments, long activeInvestments) {
        double verificationRate = totalUsers > 0 ? (verifiedUsers * 100.0 / totalUsers) : 0;
        double investmentActivityRate = totalInvestments > 0 ? (activeInvestments * 100.0 / totalInvestments) : 0;
        
        double healthScore = (verificationRate + investmentActivityRate) / 2;
        
        if (healthScore >= 80) return "EXCELLENT";
        else if (healthScore >= 60) return "GOOD";
        else if (healthScore >= 40) return "FAIR";
        else return "NEEDS_ATTENTION";
    }

    @GetMapping("/kyc-pending")
    public ResponseEntity<?> getPendingKYCUsers() {
        List<User> pendingUsers = userRepository.findByKycStatus(User.KYCStatus.PENDING);
        return ResponseEntity.ok(pendingUsers);
    }

    @GetMapping("/kyc-documents/{userId}")
    public ResponseEntity<?> getKYCDocuments(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<KYCDocument> documents = kycDocumentRepository.findByUser(user);
        return ResponseEntity.ok(documents);
    }

    @PutMapping("/kyc/{userId}/approve")
    public ResponseEntity<?> approveKYC(@PathVariable Long userId, Authentication authentication) {
        UserPrincipal adminPrincipal = (UserPrincipal) authentication.getPrincipal();
        User admin = userRepository.findById(adminPrincipal.getId()).orElse(null);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        user.setKycStatus(User.KYCStatus.APPROVED);
        user.setIsVerified(true);
        userRepository.save(user);

        // Update all KYC documents status
        List<KYCDocument> documents = kycDocumentRepository.findByUser(user);
        for (KYCDocument doc : documents) {
            doc.setStatus(KYCDocument.DocumentStatus.APPROVED);
            doc.setVerifiedAt(LocalDateTime.now());
            doc.setVerifiedBy(admin);
            kycDocumentRepository.save(doc);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "KYC approved successfully!");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/kyc/{userId}/reject")
    public ResponseEntity<?> rejectKYC(@PathVariable Long userId, 
                                     @RequestBody Map<String, String> requestBody,
                                     Authentication authentication) {
        UserPrincipal adminPrincipal = (UserPrincipal) authentication.getPrincipal();
        User admin = userRepository.findById(adminPrincipal.getId()).orElse(null);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        String rejectionReason = requestBody.get("reason");

        user.setKycStatus(User.KYCStatus.REJECTED);
        userRepository.save(user);

        // Update all KYC documents status
        List<KYCDocument> documents = kycDocumentRepository.findByUser(user);
        for (KYCDocument doc : documents) {
            doc.setStatus(KYCDocument.DocumentStatus.REJECTED);
            doc.setRejectionReason(rejectionReason);
            doc.setVerifiedAt(LocalDateTime.now());
            doc.setVerifiedBy(admin);
            kycDocumentRepository.save(doc);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "KYC rejected successfully!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/invoices/pending")
    public ResponseEntity<?> getPendingInvoices() {
        List<Invoice> pendingInvoices = invoiceRepository.findByStatus(Invoice.InvoiceStatus.PENDING);
        return ResponseEntity.ok(pendingInvoices);
    }

    @PutMapping("/invoice/{invoiceId}/approve")
    public ResponseEntity<?> approveInvoice(@PathVariable Long invoiceId, Authentication authentication) {
        UserPrincipal adminPrincipal = (UserPrincipal) authentication.getPrincipal();
        User admin = userRepository.findById(adminPrincipal.getId()).orElse(null);

        Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
        if (invoice == null) {
            return ResponseEntity.notFound().build();
        }

        invoice.setStatus(Invoice.InvoiceStatus.APPROVED);
        invoice.setApprovedAt(LocalDateTime.now());
        invoice.setApprovedBy(admin);
        invoiceRepository.save(invoice);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Invoice approved successfully!");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/invoice/{invoiceId}/reject")
    public ResponseEntity<?> rejectInvoice(@PathVariable Long invoiceId, Authentication authentication) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
        if (invoice == null) {
            return ResponseEntity.notFound().build();
        }

        invoice.setStatus(Invoice.InvoiceStatus.REJECTED);
        invoiceRepository.save(invoice);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Invoice rejected successfully!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getAllTransactions() {
        List<Transaction> transactions = transactionRepository.findAll();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/logs")
    public ResponseEntity<?> getSystemLogs() {
        // This would typically return audit logs from a separate audit table
        // For now, returning recent transactions as logs
        List<Transaction> recentTransactions = transactionRepository.findAll();
        return ResponseEntity.ok(recentTransactions);
    }
}