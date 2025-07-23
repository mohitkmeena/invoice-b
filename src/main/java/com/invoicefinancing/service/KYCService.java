package com.invoicefinancing.service;

import com.invoicefinancing.entity.KYCDocument;
import com.invoicefinancing.entity.User;
import com.invoicefinancing.repository.KYCDocumentRepository;
import com.invoicefinancing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class KYCService {

    @Autowired
    private KYCDocumentRepository kycDocumentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    public KYCDocument uploadDocument(User user, KYCDocument.DocumentType documentType, 
                                    String documentUrl, String documentNumber) {
        // Check if document already exists
        Optional<KYCDocument> existingDoc = kycDocumentRepository.findByUserAndDocumentType(user, documentType);
        
        KYCDocument kycDocument;
        if (existingDoc.isPresent()) {
            kycDocument = existingDoc.get();
            kycDocument.setStatus(KYCDocument.DocumentStatus.PENDING); // Reset status for re-verification
        } else {
            kycDocument = new KYCDocument();
            kycDocument.setUser(user);
            kycDocument.setDocumentType(documentType);
        }
        
        kycDocument.setDocumentUrl(documentUrl);
        kycDocument.setDocumentNumber(documentNumber);
        
        return kycDocumentRepository.save(kycDocument);
    }

    public List<KYCDocument> getDocumentsByUser(User user) {
        return kycDocumentRepository.findByUser(user);
    }

    public List<KYCDocument> getPendingDocuments() {
        return kycDocumentRepository.findByStatus(KYCDocument.DocumentStatus.PENDING);
    }

    public KYCDocument approveDocument(Long documentId, User verifiedBy) {
        Optional<KYCDocument> docOpt = kycDocumentRepository.findById(documentId);
        if (docOpt.isPresent()) {
            KYCDocument document = docOpt.get();
            document.setStatus(KYCDocument.DocumentStatus.APPROVED);
            document.setVerifiedAt(LocalDateTime.now());
            document.setVerifiedBy(verifiedBy);
            
            KYCDocument savedDoc = kycDocumentRepository.save(document);
            
            // Check if all required documents are approved
            checkAndUpdateUserKYCStatus(document.getUser());
            
            return savedDoc;
        }
        return null;
    }

    public KYCDocument rejectDocument(Long documentId, String rejectionReason, User verifiedBy) {
        Optional<KYCDocument> docOpt = kycDocumentRepository.findById(documentId);
        if (docOpt.isPresent()) {
            KYCDocument document = docOpt.get();
            document.setStatus(KYCDocument.DocumentStatus.REJECTED);
            document.setRejectionReason(rejectionReason);
            document.setVerifiedAt(LocalDateTime.now());
            document.setVerifiedBy(verifiedBy);
            
            KYCDocument savedDoc = kycDocumentRepository.save(document);
            
            // Update user KYC status
            User user = document.getUser();
            user.setKycStatus(User.KYCStatus.REJECTED);
            userRepository.save(user);
            
            // Send notification
            notificationService.createNotification(
                user,
                "KYC Document Rejected",
                "Your " + document.getDocumentType() + " document has been rejected. Reason: " + rejectionReason,
                "ERROR"
            );
            
            return savedDoc;
        }
        return null;
    }

    private void checkAndUpdateUserKYCStatus(User user) {
        List<KYCDocument> userDocuments = kycDocumentRepository.findByUser(user);
        
        // Check if user has all required documents approved
        boolean hasPAN = userDocuments.stream().anyMatch(doc -> 
            doc.getDocumentType() == KYCDocument.DocumentType.PAN && 
            doc.getStatus() == KYCDocument.DocumentStatus.APPROVED);
        
        boolean hasAadhaar = userDocuments.stream().anyMatch(doc -> 
            doc.getDocumentType() == KYCDocument.DocumentType.AADHAAR && 
            doc.getStatus() == KYCDocument.DocumentStatus.APPROVED);
        
        // GSTIN is required only for MSMEs
        boolean hasGSTIN = true;
        if (user.getUserType() == User.UserType.MSME) {
            hasGSTIN = userDocuments.stream().anyMatch(doc -> 
                doc.getDocumentType() == KYCDocument.DocumentType.GSTIN && 
                doc.getStatus() == KYCDocument.DocumentStatus.APPROVED);
        }
        
        if (hasPAN && hasAadhaar && hasGSTIN) {
            user.setKycStatus(User.KYCStatus.APPROVED);
            user.setIsVerified(true);
            userRepository.save(user);
            
            // Send notification
            notificationService.createNotification(
                user,
                "KYC Approved",
                "Your KYC verification has been completed successfully. You can now access all platform features.",
                "SUCCESS"
            );
        }
    }

    public boolean isUserKYCComplete(User user) {
        return user.getKycStatus() == User.KYCStatus.APPROVED && user.getIsVerified();
    }
}