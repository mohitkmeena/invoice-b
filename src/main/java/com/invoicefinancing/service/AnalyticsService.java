package com.invoicefinancing.service;

import com.invoicefinancing.entity.*;
import com.invoicefinancing.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public Map<String, Object> getPlatformAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        
        // User Analytics
        long totalUsers = userRepository.count();
        long totalMSMEs = userRepository.findByUserType(User.UserType.MSME).size();
        long totalInvestors = userRepository.findByUserType(User.UserType.INVESTOR).size();
        long verifiedUsers = userRepository.findByKycStatus(User.KYCStatus.APPROVED).size();
        
        // Invoice Analytics
        List<Invoice> allInvoices = invoiceRepository.findAll();
        BigDecimal totalInvoiceValue = allInvoices.stream()
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalFundedValue = allInvoices.stream()
                .map(Invoice::getFundingProgress)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Investment Analytics
        List<Investment> allInvestments = investmentRepository.findAll();
        BigDecimal totalInvestedAmount = allInvestments.stream()
                .map(Investment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalExpectedReturns = allInvestments.stream()
                .map(inv -> inv.getExpectedReturn() != null ? inv.getExpectedReturn() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Performance Metrics
        double fundingRate = totalInvoiceValue.compareTo(BigDecimal.ZERO) > 0 ? 
                totalFundedValue.multiply(BigDecimal.valueOf(100)).divide(totalInvoiceValue, 2, BigDecimal.ROUND_HALF_UP).doubleValue() : 0;
        
        double verificationRate = totalUsers > 0 ? (verifiedUsers * 100.0 / totalUsers) : 0;
        
        analytics.put("totalUsers", totalUsers);
        analytics.put("totalMSMEs", totalMSMEs);
        analytics.put("totalInvestors", totalInvestors);
        analytics.put("verifiedUsers", verifiedUsers);
        analytics.put("verificationRate", verificationRate);
        analytics.put("totalInvoiceValue", totalInvoiceValue);
        analytics.put("totalFundedValue", totalFundedValue);
        analytics.put("fundingRate", fundingRate);
        analytics.put("totalInvestedAmount", totalInvestedAmount);
        analytics.put("totalExpectedReturns", totalExpectedReturns);
        analytics.put("platformHealth", calculatePlatformHealth(verificationRate, fundingRate));
        
        return analytics;
    }

    public Map<String, Object> getUserAnalytics(User user) {
        Map<String, Object> analytics = new HashMap<>();
        
        if (user.getUserType() == User.UserType.MSME) {
            analytics = getMSMEAnalytics(user);
        } else if (user.getUserType() == User.UserType.INVESTOR) {
            analytics = getInvestorAnalytics(user);
        }
        
        return analytics;
    }

    private Map<String, Object> getMSMEAnalytics(User msme) {
        List<Invoice> invoices = invoiceRepository.findByMsme(msme);
        Map<String, Object> analytics = new HashMap<>();
        
        BigDecimal totalValue = invoices.stream()
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalFunded = invoices.stream()
                .map(Invoice::getFundingProgress)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long approvedCount = invoices.stream()
                .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.APPROVED || 
                              inv.getStatus() == Invoice.InvoiceStatus.FUNDED || 
                              inv.getStatus() == Invoice.InvoiceStatus.COMPLETED)
                .count();
        
        double approvalRate = invoices.size() > 0 ? (approvedCount * 100.0 / invoices.size()) : 0;
        double fundingRate = totalValue.compareTo(BigDecimal.ZERO) > 0 ? 
                totalFunded.multiply(BigDecimal.valueOf(100)).divide(totalValue, 2, BigDecimal.ROUND_HALF_UP).doubleValue() : 0;
        
        // Average funding time
        double avgFundingTime = invoices.stream()
                .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.FUNDED && inv.getApprovedAt() != null)
                .mapToLong(inv -> ChronoUnit.DAYS.between(inv.getApprovedAt().toLocalDate(), LocalDateTime.now().toLocalDate()))
                .average()
                .orElse(0.0);
        
        analytics.put("totalInvoices", invoices.size());
        analytics.put("totalValue", totalValue);
        analytics.put("totalFunded", totalFunded);
        analytics.put("approvalRate", approvalRate);
        analytics.put("fundingRate", fundingRate);
        analytics.put("averageFundingTime", avgFundingTime);
        
        return analytics;
    }

    private Map<String, Object> getInvestorAnalytics(User investor) {
        List<Investment> investments = investmentRepository.findByInvestor(investor);
        Map<String, Object> analytics = new HashMap<>();
        
        BigDecimal totalInvested = investments.stream()
                .map(Investment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalExpectedReturns = investments.stream()
                .map(inv -> inv.getExpectedReturn() != null ? inv.getExpectedReturn() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalActualReturns = investments.stream()
                .map(inv -> inv.getActualReturn() != null ? inv.getActualReturn() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long completedInvestments = investments.stream()
                .filter(inv -> inv.getStatus() == Investment.InvestmentStatus.COMPLETED)
                .count();
        
        double successRate = investments.size() > 0 ? (completedInvestments * 100.0 / investments.size()) : 0;
        double actualROI = totalInvested.compareTo(BigDecimal.ZERO) > 0 ? 
                totalActualReturns.multiply(BigDecimal.valueOf(100)).divide(totalInvested, 2, BigDecimal.ROUND_HALF_UP).doubleValue() : 0;
        
        // Portfolio diversification
        long uniqueInvoices = investments.stream()
                .map(inv -> inv.getInvoice().getId())
                .distinct()
                .count();
        
        analytics.put("totalInvestments", investments.size());
        analytics.put("totalInvested", totalInvested);
        analytics.put("totalExpectedReturns", totalExpectedReturns);
        analytics.put("totalActualReturns", totalActualReturns);
        analytics.put("successRate", successRate);
        analytics.put("actualROI", actualROI);
        analytics.put("portfolioDiversification", uniqueInvoices);
        
        return analytics;
    }

    public Map<String, Object> getMonthlyTrends() {
        Map<String, Object> trends = new HashMap<>();
        
        // Monthly user registrations
        Map<String, Long> monthlyRegistrations = userRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        user -> user.getCreatedAt().getMonth().toString() + "_" + user.getCreatedAt().getYear(),
                        Collectors.counting()
                ));
        
        // Monthly invoice creation
        Map<String, Long> monthlyInvoices = invoiceRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        invoice -> invoice.getCreatedAt().getMonth().toString() + "_" + invoice.getCreatedAt().getYear(),
                        Collectors.counting()
                ));
        
        // Monthly investment volume
        Map<String, BigDecimal> monthlyInvestmentVolume = investmentRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        investment -> investment.getCreatedAt().getMonth().toString() + "_" + investment.getCreatedAt().getYear(),
                        Collectors.reducing(BigDecimal.ZERO, Investment::getAmount, BigDecimal::add)
                ));
        
        trends.put("monthlyRegistrations", monthlyRegistrations);
        trends.put("monthlyInvoices", monthlyInvoices);
        trends.put("monthlyInvestmentVolume", monthlyInvestmentVolume);
        
        return trends;
    }

    private String calculatePlatformHealth(double verificationRate, double fundingRate) {
        double healthScore = (verificationRate + fundingRate) / 2;
        
        if (healthScore >= 80) return "EXCELLENT";
        else if (healthScore >= 60) return "GOOD";
        else if (healthScore >= 40) return "FAIR";
        else return "NEEDS_ATTENTION";
    }
}