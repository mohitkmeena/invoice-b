package com.invoicefinancing.service;

import com.invoicefinancing.entity.Transaction;
import com.invoicefinancing.entity.Investment;
import com.invoicefinancing.entity.User;
import com.invoicefinancing.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    public Transaction createTransaction(User user, Investment investment, BigDecimal amount, 
                                       Transaction.TransactionType type) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        transaction.setUser(user);
        transaction.setInvestment(investment);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setStatus(Transaction.TransactionStatus.PENDING);
        
        return transactionRepository.save(transaction);
    }

    public Transaction completeTransaction(String transactionId, String gatewayTransactionId) {
        Optional<Transaction> transactionOpt = transactionRepository.findByTransactionId(transactionId);
        if (transactionOpt.isPresent()) {
            Transaction transaction = transactionOpt.get();
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            transaction.setGatewayTransactionId(gatewayTransactionId);
            transaction.setCompletedAt(LocalDateTime.now());
            return transactionRepository.save(transaction);
        }
        return null;
    }

    public Transaction failTransaction(String transactionId, String reason) {
        Optional<Transaction> transactionOpt = transactionRepository.findByTransactionId(transactionId);
        if (transactionOpt.isPresent()) {
            Transaction transaction = transactionOpt.get();
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setGatewayResponse(reason);
            return transactionRepository.save(transaction);
        }
        return null;
    }

    public List<Transaction> getTransactionsByUser(User user) {
        return transactionRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findById(id);
    }

    public Optional<Transaction> getTransactionByTransactionId(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId);
    }

    public BigDecimal getTotalTransactionVolume() {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long getCompletedTransactionCount() {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.COMPLETED)
                .count();
    }
}