package com.invoicefinancing.controller;

import com.invoicefinancing.dto.InvestmentRequest;
import com.invoicefinancing.dto.PaymentDetailsRequest;
import com.invoicefinancing.entity.*;
import com.invoicefinancing.repository.*;
import com.invoicefinancing.service.InvestmentService;
import com.invoicefinancing.service.KYCService;
import com.invoicefinancing.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/investor")
@PreAuthorize("hasRole('INVESTOR')")
public class InvestorController {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private PaymentDetailsRepository paymentDetailsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private InvestmentService investmentService;

    @Autowired
    private KYCService kycService;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<Investment> investments = investmentRepository.findByInvestorIdOrderByCreatedAtDesc(user.getId());
        
        BigDecimal totalInvested = investments.stream()
                .map(Investment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal expectedReturns = investments.stream()
                .map(Investment::getExpectedReturn)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal actualReturns = investments.stream()
                .map(inv -> inv.getActualReturn() != null ? inv.getActualReturn() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate portfolio performance
        long completedInvestments = investments.stream().filter(i -> i.getStatus() == Investment.InvestmentStatus.COMPLETED).count();
        long overdueInvestments = investments.stream().filter(i -> i.getStatus() == Investment.InvestmentStatus.OVERDUE).count();
        
        // Calculate average investment size and ROI
        BigDecimal averageInvestment = investments.size() > 0 ? 
                totalInvested.divide(BigDecimal.valueOf(investments.size()), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
        
        BigDecimal actualROI = totalInvested.compareTo(BigDecimal.ZERO) > 0 ? 
                actualReturns.multiply(BigDecimal.valueOf(100)).divide(totalInvested, 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
        
        // Portfolio diversification
        long uniqueInvoices = investments.stream().map(inv -> inv.getInvoice().getId()).distinct().count();

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalInvestments", investments.size());
        dashboard.put("totalInvested", totalInvested);
        dashboard.put("expectedReturns", expectedReturns);
        dashboard.put("actualReturns", actualReturns);
        dashboard.put("averageInvestment", averageInvestment);
        dashboard.put("actualROI", actualROI);
        
        dashboard.put("activeInvestments", investments.stream().filter(i -> i.getStatus() == Investment.InvestmentStatus.ACTIVE).count());
        dashboard.put("completedInvestments", completedInvestments);
        dashboard.put("overdueInvestments", overdueInvestments);
        dashboard.put("portfolioDiversification", uniqueInvoices);
        
        // Performance metrics
        double successRate = investments.size() > 0 ? (completedInvestments * 100.0 / investments.size()) : 0;
        dashboard.put("successRate", successRate);
        
        // User profile info
        dashboard.put("kycStatus", user.getKycStatus());
        dashboard.put("isVerified", user.getIsVerified());
        dashboard.put("memberSince", user.getCreatedAt());
        
        dashboard.put("recentInvestments", investments.stream().limit(5).toList());

        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/invoices")
    public ResponseEntity<?> getAvailableInvoices() {
        List<Invoice> invoices = invoiceRepository.findApprovedInvoicesForInvestment();
        return ResponseEntity.ok(invoices);
    }

    @PostMapping("/invest")
    public ResponseEntity<?> makeInvestment(@Valid @RequestBody InvestmentRequest investmentRequest,
                                          Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        // Check if user's KYC is approved
        if (user.getKycStatus() != User.KYCStatus.APPROVED) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "KYC verification required before making investments");
            return ResponseEntity.badRequest().body(response);
        }

        Invoice invoice = invoiceRepository.findById(investmentRequest.getInvoiceId()).orElse(null);
        if (invoice == null || invoice.getStatus() != Invoice.InvoiceStatus.APPROVED) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Invoice not found or not available for investment");
            return ResponseEntity.badRequest().body(response);
        }

        // Check if investment amount is valid
        BigDecimal remainingAmount = invoice.getAmount().subtract(invoice.getFundingProgress());
        if (investmentRequest.getAmount().compareTo(remainingAmount) > 0) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Investment amount exceeds remaining funding requirement");
            return ResponseEntity.badRequest().body(response);
        }

        // Create investment
        Investment investment = new Investment();
        investment.setInvoice(invoice);
        investment.setInvestor(user);
        investment.setAmount(investmentRequest.getAmount());
        investment.setInvestmentDate(LocalDate.now());
        investment.setMaturityDate(LocalDate.now().plusDays(invoice.getDurationDays()));
        
        // Calculate expected return
        BigDecimal expectedReturn = investmentRequest.getAmount()
                .multiply(invoice.getInterestRate().divide(BigDecimal.valueOf(100)))
                .multiply(BigDecimal.valueOf(invoice.getDurationDays()).divide(BigDecimal.valueOf(365)));
        investment.setExpectedReturn(expectedReturn);

        investmentRepository.save(investment);

        // Update invoice funding progress
        invoice.setFundingProgress(invoice.getFundingProgress().add(investmentRequest.getAmount()));
        if (invoice.getFundingProgress().compareTo(invoice.getAmount()) >= 0) {
            invoice.setStatus(Invoice.InvoiceStatus.FUNDED);
        }
        invoiceRepository.save(invoice);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN_" + System.currentTimeMillis());
        transaction.setUser(user);
        transaction.setInvestment(investment);
        transaction.setAmount(investmentRequest.getAmount());
        transaction.setType(Transaction.TransactionType.INVESTMENT);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Investment made successfully!");
        response.put("investmentId", investment.getId());
        response.put("expectedReturn", expectedReturn);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/investments")
    public ResponseEntity<?> getInvestments(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<Investment> investments = investmentRepository.findByInvestorIdOrderByCreatedAtDesc(user.getId());
        return ResponseEntity.ok(investments);
    }

    @GetMapping("/investment/{id}")
    public ResponseEntity<?> getInvestment(@PathVariable Long id, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Investment investment = investmentRepository.findById(id).orElse(null);
        if (investment == null || !investment.getInvestor().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(investment);
    }

    @GetMapping("/investment/{id}/progress")
    public ResponseEntity<?> getInvestmentProgress(@PathVariable Long id, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Investment investment = investmentRepository.findById(id).orElse(null);
        if (investment == null || !investment.getInvestor().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        Invoice invoice = investment.getInvoice();
        
        // Calculate progress metrics
        long daysInvested = java.time.temporal.ChronoUnit.DAYS.between(
                investment.getInvestmentDate(), 
                java.time.LocalDate.now()
        );
        
        long daysUntilMaturity = java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDate.now(), 
                investment.getMaturityDate()
        );
        
        double progressPercentage = investment.getDurationDays() != null && investment.getDurationDays() > 0 ? 
                (daysInvested * 100.0 / investment.getDurationDays()) : 0;
        
        // Calculate current expected return based on time elapsed
        BigDecimal currentExpectedReturn = BigDecimal.ZERO;
        if (investment.getExpectedReturn() != null && investment.getDurationDays() != null && investment.getDurationDays() > 0) {
            double timeRatio = Math.min(1.0, daysInvested / (double) investment.getDurationDays());
            currentExpectedReturn = investment.getExpectedReturn().multiply(BigDecimal.valueOf(timeRatio));
        }
        
        // Get invoice funding status
        List<Investment> allInvestments = investmentRepository.findByInvoiceId(invoice.getId());
        BigDecimal totalInvoiceFunding = allInvestments.stream()
                .map(Investment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        double invoiceFundingPercentage = invoice.getAmount().compareTo(BigDecimal.ZERO) > 0 ? 
                totalInvoiceFunding.multiply(BigDecimal.valueOf(100)).divide(invoice.getAmount(), 2, BigDecimal.ROUND_HALF_UP).doubleValue() : 0;

        Map<String, Object> progress = new HashMap<>();
        progress.put("investmentId", investment.getId());
        progress.put("amount", investment.getAmount());
        progress.put("expectedReturn", investment.getExpectedReturn());
        progress.put("currentExpectedReturn", currentExpectedReturn);
        progress.put("actualReturn", investment.getActualReturn());
        progress.put("status", investment.getStatus());
        progress.put("investmentDate", investment.getInvestmentDate());
        progress.put("maturityDate", investment.getMaturityDate());
        progress.put("daysInvested", daysInvested);
        progress.put("daysUntilMaturity", daysUntilMaturity);
        progress.put("progressPercentage", Math.min(100.0, progressPercentage));
        
        // Invoice details
        Map<String, Object> invoiceDetails = new HashMap<>();
        invoiceDetails.put("invoiceId", invoice.getId());
        invoiceDetails.put("invoiceNumber", invoice.getInvoiceNumber());
        invoiceDetails.put("clientName", invoice.getClientName());
        invoiceDetails.put("totalAmount", invoice.getAmount());
        invoiceDetails.put("dueDate", invoice.getDueDate());
        invoiceDetails.put("status", invoice.getStatus());
        invoiceDetails.put("fundingPercentage", invoiceFundingPercentage);
        invoiceDetails.put("totalFunding", totalInvoiceFunding);
        invoiceDetails.put("msmeCompany", invoice.getMsme().getName());
        
        progress.put("invoice", invoiceDetails);
        progress.put("lastUpdated", java.time.LocalDateTime.now());

        return ResponseEntity.ok(progress);
    }

    @GetMapping("/portfolio-analytics")
    public ResponseEntity<?> getPortfolioAnalytics(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<Investment> investments = investmentRepository.findByInvestorIdOrderByCreatedAtDesc(user.getId());
        
        // Portfolio performance metrics
        BigDecimal totalInvested = investments.stream()
                .map(Investment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalExpectedReturns = investments.stream()
                .map(inv -> inv.getExpectedReturn() != null ? inv.getExpectedReturn() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalActualReturns = investments.stream()
                .map(inv -> inv.getActualReturn() != null ? inv.getActualReturn() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Monthly investment breakdown
        Map<String, BigDecimal> monthlyInvestments = new HashMap<>();
        Map<String, BigDecimal> monthlyReturns = new HashMap<>();
        Map<String, Long> monthlyCount = new HashMap<>();
        
        for (Investment investment : investments) {
            String monthKey = investment.getCreatedAt().getMonth().toString() + "_" + investment.getCreatedAt().getYear();
            
            monthlyInvestments.merge(monthKey, investment.getAmount(), BigDecimal::add);
            monthlyReturns.merge(monthKey, 
                    investment.getActualReturn() != null ? investment.getActualReturn() : BigDecimal.ZERO, 
                    BigDecimal::add);
            monthlyCount.merge(monthKey, 1L, Long::sum);
        }
        
        // Status distribution
        Map<String, Long> statusDistribution = investments.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        inv -> inv.getStatus().toString(),
                        java.util.stream.Collectors.counting()
                ));
        
        // Risk analysis - diversification by MSME
        Map<String, BigDecimal> msmeDistribution = investments.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        inv -> inv.getInvoice().getMsme().getName(),
                        java.util.stream.Collectors.reducing(BigDecimal.ZERO, Investment::getAmount, BigDecimal::add)
                ));
        
        // Performance metrics
        double actualROI = totalInvested.compareTo(BigDecimal.ZERO) > 0 ? 
                totalActualReturns.multiply(BigDecimal.valueOf(100)).divide(totalInvested, 2, BigDecimal.ROUND_HALF_UP).doubleValue() : 0;
        
        double expectedROI = totalInvested.compareTo(BigDecimal.ZERO) > 0 ? 
                totalExpectedReturns.multiply(BigDecimal.valueOf(100)).divide(totalInvested, 2, BigDecimal.ROUND_HALF_UP).doubleValue() : 0;
        
        long completedInvestments = investments.stream().filter(inv -> inv.getStatus() == Investment.InvestmentStatus.COMPLETED).count();
        double successRate = investments.size() > 0 ? (completedInvestments * 100.0 / investments.size()) : 0;
        
        BigDecimal averageInvestment = investments.size() > 0 ? 
                totalInvested.divide(BigDecimal.valueOf(investments.size()), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalInvested", totalInvested);
        analytics.put("totalExpectedReturns", totalExpectedReturns);
        analytics.put("totalActualReturns", totalActualReturns);
        analytics.put("actualROI", actualROI);
        analytics.put("expectedROI", expectedROI);
        analytics.put("successRate", successRate);
        analytics.put("averageInvestment", averageInvestment);
        analytics.put("totalInvestments", investments.size());
        analytics.put("portfolioDiversification", msmeDistribution.size());
        
        // Monthly data
        Map<String, Object> monthlyData = new HashMap<>();
        monthlyData.put("investments", monthlyInvestments);
        monthlyData.put("returns", monthlyReturns);
        monthlyData.put("count", monthlyCount);
        
        analytics.put("monthlyBreakdown", monthlyData);
        analytics.put("statusDistribution", statusDistribution);
        analytics.put("msmeDistribution", msmeDistribution);
        
        return ResponseEntity.ok(analytics);
    }

    @PostMapping("/payment-details")
    public ResponseEntity<?> savePaymentDetails(@Valid @RequestBody PaymentDetailsRequest paymentRequest,
                                              Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        PaymentDetails paymentDetails = paymentDetailsRepository.findByUser(user).orElse(new PaymentDetails());
        paymentDetails.setUser(user);
        paymentDetails.setUpiId(paymentRequest.getUpiId());
        paymentDetails.setBankAccountNumber(paymentRequest.getBankAccountNumber());
        paymentDetails.setIfscCode(paymentRequest.getIfscCode());
        paymentDetails.setAccountHolderName(paymentRequest.getAccountHolderName());
        paymentDetails.setBankName(paymentRequest.getBankName());

        paymentDetailsRepository.save(paymentDetails);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Payment details saved successfully!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payment-details")
    public ResponseEntity<?> getPaymentDetails(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        PaymentDetails paymentDetails = paymentDetailsRepository.findByUser(user).orElse(null);
        if (paymentDetails == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(paymentDetails);
    }

    @PutMapping("/payment-details")
    public ResponseEntity<?> updatePaymentDetails(@Valid @RequestBody PaymentDetailsRequest paymentRequest,
                                                Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        PaymentDetails paymentDetails = paymentDetailsRepository.findByUser(user).orElse(null);
        if (paymentDetails == null) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Payment details not found. Please create them first.");
            return ResponseEntity.notFound().build();
        }

        paymentDetails.setUpiId(paymentRequest.getUpiId());
        paymentDetails.setBankAccountNumber(paymentRequest.getBankAccountNumber());
        paymentDetails.setIfscCode(paymentRequest.getIfscCode());
        paymentDetails.setAccountHolderName(paymentRequest.getAccountHolderName());
        paymentDetails.setBankName(paymentRequest.getBankName());

        paymentDetailsRepository.save(paymentDetails);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Payment details updated successfully!");
        return ResponseEntity.ok(response);
    }
}