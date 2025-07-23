package com.invoicefinancing.controller;

import com.invoicefinancing.entity.*;
import com.invoicefinancing.repository.*;
import com.invoicefinancing.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @GetMapping("/platform-summary")
    public ResponseEntity<?> getPlatformSummary() {
        // User Statistics
        long totalUsers = userRepository.count();
        long totalMSMEs = userRepository.findByUserType(User.UserType.MSME).size();
        long totalInvestors = userRepository.findByUserType(User.UserType.INVESTOR).size();
        long verifiedUsers = userRepository.findByKycStatus(User.KYCStatus.APPROVED).size();
        
        // Invoice Statistics
        long totalInvoices = invoiceRepository.count();
        long approvedInvoices = invoiceRepository.findByStatus(Invoice.InvoiceStatus.APPROVED).size();
        long fundedInvoices = invoiceRepository.findByStatus(Invoice.InvoiceStatus.FUNDED).size();
        long completedInvoices = invoiceRepository.findByStatus(Invoice.InvoiceStatus.COMPLETED).size();
        
        // Investment Statistics
        long totalInvestments = investmentRepository.count();
        List<Investment> allInvestments = investmentRepository.findAll();
        
        BigDecimal totalInvestedAmount = allInvestments.stream()
                .map(Investment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalExpectedReturns = allInvestments.stream()
                .map(inv -> inv.getExpectedReturn() != null ? inv.getExpectedReturn() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Active investments
        long activeInvestments = allInvestments.stream()
                .filter(inv -> inv.getStatus() == Investment.InvestmentStatus.ACTIVE)
                .count();
        
        // Calculate total invoice value
        List<Invoice> allInvoices = invoiceRepository.findAll();
        BigDecimal totalInvoiceValue = allInvoices.stream()
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalFundingProgress = allInvoices.stream()
                .map(Invoice::getFundingProgress)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Transaction Statistics
        long totalTransactions = transactionRepository.count();
        List<Transaction> completedTransactions = transactionRepository.findAll().stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .toList();
        
        BigDecimal totalTransactionVolume = completedTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> summary = new HashMap<>();
        
        // User Metrics
        summary.put("totalUsers", totalUsers);
        summary.put("totalMSMEs", totalMSMEs);
        summary.put("totalInvestors", totalInvestors);
        summary.put("verifiedUsers", verifiedUsers);
        summary.put("verificationRate", totalUsers > 0 ? (verifiedUsers * 100.0 / totalUsers) : 0);
        
        // Invoice Metrics
        summary.put("totalInvoices", totalInvoices);
        summary.put("approvedInvoices", approvedInvoices);
        summary.put("fundedInvoices", fundedInvoices);
        summary.put("completedInvoices", completedInvoices);
        summary.put("totalInvoiceValue", totalInvoiceValue);
        summary.put("totalFundingProgress", totalFundingProgress);
        summary.put("fundingRate", totalInvoiceValue.compareTo(BigDecimal.ZERO) > 0 ? 
                totalFundingProgress.multiply(BigDecimal.valueOf(100)).divide(totalInvoiceValue, 2, BigDecimal.ROUND_HALF_UP) : 0);
        
        // Investment Metrics
        summary.put("totalInvestments", totalInvestments);
        summary.put("activeInvestments", activeInvestments);
        summary.put("totalInvestedAmount", totalInvestedAmount);
        summary.put("totalExpectedReturns", totalExpectedReturns);
        summary.put("averageInvestmentSize", totalInvestments > 0 ? 
                totalInvestedAmount.divide(BigDecimal.valueOf(totalInvestments), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO);
        
        // Transaction Metrics
        summary.put("totalTransactions", totalTransactions);
        summary.put("totalTransactionVolume", totalTransactionVolume);
        summary.put("completedTransactions", completedTransactions.size());
        
        // Platform Health
        summary.put("platformHealth", calculatePlatformHealth(totalUsers, verifiedUsers, totalInvestments, activeInvestments));
        summary.put("lastUpdated", LocalDateTime.now());

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/user-stats")
    public ResponseEntity<?> getUserStats(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> userStats = new HashMap<>();
        userStats.put("userId", user.getId());
        userStats.put("name", user.getName());
        userStats.put("email", user.getEmail());
        userStats.put("userType", user.getUserType());
        userStats.put("kycStatus", user.getKycStatus());
        userStats.put("isVerified", user.getIsVerified());
        userStats.put("memberSince", user.getCreatedAt());

        if (user.getUserType() == User.UserType.MSME) {
            List<Invoice> userInvoices = invoiceRepository.findByMsme(user);
            BigDecimal totalInvoiceValue = userInvoices.stream()
                    .map(Invoice::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalFunded = userInvoices.stream()
                    .map(Invoice::getFundingProgress)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            userStats.put("totalInvoices", userInvoices.size());
            userStats.put("totalInvoiceValue", totalInvoiceValue);
            userStats.put("totalFunded", totalFunded);
            userStats.put("pendingInvoices", userInvoices.stream().filter(i -> i.getStatus() == Invoice.InvoiceStatus.PENDING).count());
            userStats.put("approvedInvoices", userInvoices.stream().filter(i -> i.getStatus() == Invoice.InvoiceStatus.APPROVED).count());
            userStats.put("fundedInvoices", userInvoices.stream().filter(i -> i.getStatus() == Invoice.InvoiceStatus.FUNDED).count());
            userStats.put("completedInvoices", userInvoices.stream().filter(i -> i.getStatus() == Invoice.InvoiceStatus.COMPLETED).count());
            
        } else if (user.getUserType() == User.UserType.INVESTOR) {
            List<Investment> userInvestments = investmentRepository.findByInvestor(user);
            BigDecimal totalInvested = userInvestments.stream()
                    .map(Investment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalExpectedReturns = userInvestments.stream()
                    .map(inv -> inv.getExpectedReturn() != null ? inv.getExpectedReturn() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalActualReturns = userInvestments.stream()
                    .map(inv -> inv.getActualReturn() != null ? inv.getActualReturn() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            userStats.put("totalInvestments", userInvestments.size());
            userStats.put("totalInvested", totalInvested);
            userStats.put("totalExpectedReturns", totalExpectedReturns);
            userStats.put("totalActualReturns", totalActualReturns);
            userStats.put("activeInvestments", userInvestments.stream().filter(i -> i.getStatus() == Investment.InvestmentStatus.ACTIVE).count());
            userStats.put("completedInvestments", userInvestments.stream().filter(i -> i.getStatus() == Investment.InvestmentStatus.COMPLETED).count());
            userStats.put("overdueInvestments", userInvestments.stream().filter(i -> i.getStatus() == Investment.InvestmentStatus.OVERDUE).count());
            
            // Calculate ROI
            if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal roi = totalActualReturns.multiply(BigDecimal.valueOf(100)).divide(totalInvested, 2, BigDecimal.ROUND_HALF_UP);
                userStats.put("actualROI", roi);
            } else {
                userStats.put("actualROI", BigDecimal.ZERO);
            }
        }

        // Transaction history for user
        List<Transaction> userTransactions = transactionRepository.findByUserOrderByCreatedAtDesc(user);
        userStats.put("totalTransactions", userTransactions.size());
        userStats.put("recentTransactions", userTransactions.stream().limit(5).toList());

        return ResponseEntity.ok(userStats);
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<?> getRecentActivity() {
        Map<String, Object> activity = new HashMap<>();
        
        // Recent registrations (last 10)
        List<User> recentUsers = userRepository.findAll().stream()
                .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                .limit(10)
                .map(user -> {
                    User publicUser = new User();
                    publicUser.setId(user.getId());
                    publicUser.setName(user.getName());
                    publicUser.setUserType(user.getUserType());
                    publicUser.setKycStatus(user.getKycStatus());
                    publicUser.setCreatedAt(user.getCreatedAt());
                    return publicUser;
                })
                .toList();
        
        // Recent invoices (last 10)
        List<Invoice> recentInvoices = invoiceRepository.findAll().stream()
                .sorted((i1, i2) -> i2.getCreatedAt().compareTo(i1.getCreatedAt()))
                .limit(10)
                .toList();
        
        // Recent investments (last 10)
        List<Investment> recentInvestments = investmentRepository.findAll().stream()
                .sorted((i1, i2) -> i2.getCreatedAt().compareTo(i1.getCreatedAt()))
                .limit(10)
                .toList();
        
        // Recent transactions (last 10)
        List<Transaction> recentTransactions = transactionRepository.findAll().stream()
                .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                .limit(10)
                .toList();

        activity.put("recentUsers", recentUsers);
        activity.put("recentInvoices", recentInvoices);
        activity.put("recentInvestments", recentInvestments);
        activity.put("recentTransactions", recentTransactions);
        activity.put("lastUpdated", LocalDateTime.now());

        return ResponseEntity.ok(activity);
    }

    @GetMapping("/monthly-stats")
    public ResponseEntity<?> getMonthlyStats() {
        // This would typically involve more complex date-based queries
        // For now, providing basic monthly aggregation
        Map<String, Object> monthlyStats = new HashMap<>();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        
        // Users registered this month
        long monthlyRegistrations = userRepository.findAll().stream()
                .filter(user -> user.getCreatedAt().isAfter(monthStart))
                .count();
        
        // Invoices created this month
        long monthlyInvoices = invoiceRepository.findAll().stream()
                .filter(invoice -> invoice.getCreatedAt().isAfter(monthStart))
                .count();
        
        // Investments made this month
        long monthlyInvestments = investmentRepository.findAll().stream()
                .filter(investment -> investment.getCreatedAt().isAfter(monthStart))
                .count();
        
        // Monthly investment volume
        BigDecimal monthlyInvestmentVolume = investmentRepository.findAll().stream()
                .filter(investment -> investment.getCreatedAt().isAfter(monthStart))
                .map(Investment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        monthlyStats.put("monthlyRegistrations", monthlyRegistrations);
        monthlyStats.put("monthlyInvoices", monthlyInvoices);
        monthlyStats.put("monthlyInvestments", monthlyInvestments);
        monthlyStats.put("monthlyInvestmentVolume", monthlyInvestmentVolume);
        monthlyStats.put("month", now.getMonth().toString());
        monthlyStats.put("year", now.getYear());

        return ResponseEntity.ok(monthlyStats);
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
}