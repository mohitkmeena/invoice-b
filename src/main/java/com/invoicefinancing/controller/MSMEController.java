package com.invoicefinancing.controller;

import com.invoicefinancing.dto.InvoiceRequest;
import com.invoicefinancing.entity.Invoice;
import com.invoicefinancing.entity.Investment;
import com.invoicefinancing.entity.User;
import com.invoicefinancing.repository.InvoiceRepository;
import com.invoicefinancing.repository.InvestmentRepository;
import com.invoicefinancing.repository.UserRepository;
import com.invoicefinancing.service.InvoiceService;
import com.invoicefinancing.service.FileUploadService;
import com.invoicefinancing.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/msme")
@PreAuthorize("hasRole('MSME')")
public class MSMEController {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private FileUploadService fileUploadService;

    private final String UPLOAD_DIR = "uploads/invoices/";

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<Invoice> invoices = invoiceRepository.findByMsmeIdOrderByCreatedAtDesc(user.getId());
        
        // Calculate financial metrics
        BigDecimal totalInvoiceValue = invoices.stream()
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalFunded = invoices.stream()
                .map(Invoice::getFundingProgress)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal pendingAmount = invoices.stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.PENDING || i.getStatus() == Invoice.InvoiceStatus.APPROVED)
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate average funding time and success rate
        long approvedInvoices = invoices.stream().filter(i -> i.getStatus() == Invoice.InvoiceStatus.APPROVED || 
                i.getStatus() == Invoice.InvoiceStatus.FUNDED || i.getStatus() == Invoice.InvoiceStatus.COMPLETED).count();
        double approvalRate = invoices.size() > 0 ? (approvedInvoices * 100.0 / invoices.size()) : 0;
        
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalInvoices", invoices.size());
        dashboard.put("pendingInvoices", invoices.stream().filter(i -> i.getStatus() == Invoice.InvoiceStatus.PENDING).count());
        dashboard.put("approvedInvoices", invoices.stream().filter(i -> i.getStatus() == Invoice.InvoiceStatus.APPROVED).count());
        dashboard.put("fundedInvoices", invoices.stream().filter(i -> i.getStatus() == Invoice.InvoiceStatus.FUNDED).count());
        dashboard.put("completedInvoices", invoices.stream().filter(i -> i.getStatus() == Invoice.InvoiceStatus.COMPLETED).count());
        dashboard.put("rejectedInvoices", invoices.stream().filter(i -> i.getStatus() == Invoice.InvoiceStatus.REJECTED).count());
        
        // Financial metrics
        dashboard.put("totalInvoiceValue", totalInvoiceValue);
        dashboard.put("totalFunded", totalFunded);
        dashboard.put("pendingAmount", pendingAmount);
        dashboard.put("fundingRate", totalInvoiceValue.compareTo(BigDecimal.ZERO) > 0 ? 
                totalFunded.multiply(BigDecimal.valueOf(100)).divide(totalInvoiceValue, 2, BigDecimal.ROUND_HALF_UP) : 0);
        dashboard.put("approvalRate", approvalRate);
        
        // User profile info
        dashboard.put("kycStatus", user.getKycStatus());
        dashboard.put("isVerified", user.getIsVerified());
        dashboard.put("memberSince", user.getCreatedAt());
        
        dashboard.put("recentInvoices", invoices.stream().limit(5).toList());

        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/invoice/{id}/progress")
    public ResponseEntity<?> getInvoiceProgress(@PathVariable Long id, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Invoice invoice = invoiceRepository.findById(id).orElse(null);
        if (invoice == null || !invoice.getMsme().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        // Get all investments for this invoice
        List<Investment> investments = investmentRepository.findByInvoiceId(id);
        
        BigDecimal totalInvested = investments.stream()
                .map(Investment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal remainingAmount = invoice.getAmount().subtract(totalInvested);
        double progressPercentage = invoice.getAmount().compareTo(BigDecimal.ZERO) > 0 ? 
                totalInvested.multiply(BigDecimal.valueOf(100)).divide(invoice.getAmount(), 2, BigDecimal.ROUND_HALF_UP).doubleValue() : 0;
        
        // Calculate time metrics
        long daysSinceCreation = java.time.temporal.ChronoUnit.DAYS.between(
                invoice.getCreatedAt().toLocalDate(), 
                java.time.LocalDate.now()
        );
        
        long daysUntilDue = invoice.getDueDate() != null ? 
                java.time.temporal.ChronoUnit.DAYS.between(
                        java.time.LocalDate.now(), 
                        invoice.getDueDate()
                ) : 0;

        Map<String, Object> progress = new HashMap<>();
        progress.put("invoiceId", invoice.getId());
        progress.put("invoiceNumber", invoice.getInvoiceNumber());
        progress.put("totalAmount", invoice.getAmount());
        progress.put("totalInvested", totalInvested);
        progress.put("remainingAmount", remainingAmount);
        progress.put("progressPercentage", progressPercentage);
        progress.put("numberOfInvestors", investments.size());
        progress.put("status", invoice.getStatus());
        progress.put("daysSinceCreation", daysSinceCreation);
        progress.put("daysUntilDue", daysUntilDue);
        progress.put("interestRate", invoice.getInterestRate());
        progress.put("durationDays", invoice.getDurationDays());
        
        // Investment breakdown
        List<Map<String, Object>> investmentBreakdown = investments.stream()
                .map(inv -> {
                    Map<String, Object> invData = new HashMap<>();
                    invData.put("investmentId", inv.getId());
                    invData.put("amount", inv.getAmount());
                    invData.put("investorName", inv.getInvestor().getName());
                    invData.put("investmentDate", inv.getInvestmentDate());
                    invData.put("expectedReturn", inv.getExpectedReturn());
                    invData.put("status", inv.getStatus());
                    return invData;
                })
                .toList();
        
        progress.put("investments", investmentBreakdown);
        progress.put("lastUpdated", java.time.LocalDateTime.now());

        return ResponseEntity.ok(progress);
    }

    @GetMapping("/analytics")
    public ResponseEntity<?> getMSMEAnalytics(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<Invoice> invoices = invoiceRepository.findByMsmeIdOrderByCreatedAtDesc(user.getId());
        
        // Monthly breakdown
        Map<String, Object> monthlyData = new HashMap<>();
        Map<String, BigDecimal> monthlyInvoiceValue = new HashMap<>();
        Map<String, BigDecimal> monthlyFunding = new HashMap<>();
        Map<String, Long> monthlyCount = new HashMap<>();
        
        for (Invoice invoice : invoices) {
            String monthKey = invoice.getCreatedAt().getMonth().toString() + "_" + invoice.getCreatedAt().getYear();
            
            monthlyInvoiceValue.merge(monthKey, invoice.getAmount(), BigDecimal::add);
            monthlyFunding.merge(monthKey, invoice.getFundingProgress(), BigDecimal::add);
            monthlyCount.merge(monthKey, 1L, Long::sum);
        }
        
        monthlyData.put("invoiceValue", monthlyInvoiceValue);
        monthlyData.put("funding", monthlyFunding);
        monthlyData.put("count", monthlyCount);
        
        // Status distribution
        Map<String, Long> statusDistribution = invoices.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        inv -> inv.getStatus().toString(),
                        java.util.stream.Collectors.counting()
                ));
        
        // Average metrics
        BigDecimal avgInvoiceValue = invoices.size() > 0 ? 
                invoices.stream().map(Invoice::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(invoices.size()), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
        
        double avgFundingTime = invoices.stream()
                .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.FUNDED && inv.getApprovedAt() != null)
                .mapToLong(inv -> java.time.temporal.ChronoUnit.DAYS.between(
                        inv.getApprovedAt().toLocalDate(),
                        java.time.LocalDate.now()
                ))
                .average()
                .orElse(0.0);

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("monthlyBreakdown", monthlyData);
        analytics.put("statusDistribution", statusDistribution);
        analytics.put("averageInvoiceValue", avgInvoiceValue);
        analytics.put("averageFundingTime", avgFundingTime);
        analytics.put("totalInvoices", invoices.size());
        analytics.put("successRate", invoices.size() > 0 ? 
                (invoices.stream().filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.COMPLETED).count() * 100.0 / invoices.size()) : 0);
        
        return ResponseEntity.ok(analytics);
    }

    @PostMapping("/invoice")
    public ResponseEntity<?> createInvoice(@Valid @RequestBody InvoiceRequest invoiceRequest, 
                                         Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Invoice invoice = new Invoice();
        invoice.setMsme(user);
        invoice.setInvoiceNumber(invoiceRequest.getInvoiceNumber());
        invoice.setClientName(invoiceRequest.getClientName());
        invoice.setAmount(invoiceRequest.getAmount());
        invoice.setDueDate(invoiceRequest.getDueDate());
        invoice.setDescription(invoiceRequest.getDescription());
        invoice.setInterestRate(invoiceRequest.getInterestRate());
        invoice.setDurationDays(invoiceRequest.getDurationDays());

        invoiceRepository.save(invoice);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Invoice created successfully!");
        response.put("invoiceId", invoice.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/invoice/{id}/upload-document")
    public ResponseEntity<?> uploadInvoiceDocument(@PathVariable Long id,
                                                 @RequestParam("file") MultipartFile file,
                                                 Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Invoice invoice = invoiceRepository.findById(id).orElse(null);
        if (invoice == null || !invoice.getMsme().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        try {
            String documentUrl = fileUploadService.uploadInvoiceDocument(file);
            
            // Update invoice with document URL
            invoice.setDocumentUrl(documentUrl);
            invoiceRepository.save(invoice);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Document uploaded successfully!");
            response.put("documentUrl", invoice.getDocumentUrl());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Failed to upload document: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/invoices")
    public ResponseEntity<?> getInvoices(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<Invoice> invoices = invoiceRepository.findByMsmeIdOrderByCreatedAtDesc(user.getId());
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/invoice/{id}")
    public ResponseEntity<?> getInvoice(@PathVariable Long id, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Invoice invoice = invoiceRepository.findById(id).orElse(null);
        if (invoice == null || !invoice.getMsme().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(invoice);
    }

    @PutMapping("/invoice/{id}")
    public ResponseEntity<?> updateInvoice(@PathVariable Long id,
                                         @Valid @RequestBody InvoiceRequest invoiceRequest,
                                         Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Invoice invoice = invoiceRepository.findById(id).orElse(null);
        if (invoice == null || !invoice.getMsme().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        // Only allow updates if invoice is still pending
        if (invoice.getStatus() != Invoice.InvoiceStatus.PENDING) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Cannot update invoice that is not in pending status");
            return ResponseEntity.badRequest().body(response);
        }

        invoice.setInvoiceNumber(invoiceRequest.getInvoiceNumber());
        invoice.setClientName(invoiceRequest.getClientName());
        invoice.setAmount(invoiceRequest.getAmount());
        invoice.setDueDate(invoiceRequest.getDueDate());
        invoice.setDescription(invoiceRequest.getDescription());
        invoice.setInterestRate(invoiceRequest.getInterestRate());
        invoice.setDurationDays(invoiceRequest.getDurationDays());

        invoiceRepository.save(invoice);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Invoice updated successfully!");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/invoice/{id}")
    public ResponseEntity<?> deleteInvoice(@PathVariable Long id, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Invoice invoice = invoiceRepository.findById(id).orElse(null);
        if (invoice == null || !invoice.getMsme().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        // Only allow deletion if invoice is still pending
        if (invoice.getStatus() != Invoice.InvoiceStatus.PENDING) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Cannot delete invoice that is not in pending status");
            return ResponseEntity.badRequest().body(response);
        }

        invoiceRepository.delete(invoice);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Invoice deleted successfully!");
        return ResponseEntity.ok(response);
    }
}