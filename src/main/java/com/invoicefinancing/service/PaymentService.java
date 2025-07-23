package com.invoicefinancing.service;

import com.invoicefinancing.entity.PaymentDetails;
import com.invoicefinancing.entity.User;
import com.invoicefinancing.repository.PaymentDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PaymentService {

    @Autowired
    private PaymentDetailsRepository paymentDetailsRepository;

    public PaymentDetails savePaymentDetails(User user, PaymentDetails paymentDetails) {
        paymentDetails.setUser(user);
        return paymentDetailsRepository.save(paymentDetails);
    }

    public Optional<PaymentDetails> getPaymentDetailsByUser(User user) {
        return paymentDetailsRepository.findByUser(user);
    }

    public PaymentDetails updatePaymentDetails(PaymentDetails paymentDetails) {
        return paymentDetailsRepository.save(paymentDetails);
    }

    public void deletePaymentDetails(Long paymentDetailsId) {
        paymentDetailsRepository.deleteById(paymentDetailsId);
    }

    public boolean verifyPaymentDetails(Long paymentDetailsId) {
        Optional<PaymentDetails> paymentDetailsOpt = paymentDetailsRepository.findById(paymentDetailsId);
        if (paymentDetailsOpt.isPresent()) {
            PaymentDetails paymentDetails = paymentDetailsOpt.get();
            paymentDetails.setIsVerified(true);
            paymentDetailsRepository.save(paymentDetails);
            return true;
        }
        return false;
    }
}