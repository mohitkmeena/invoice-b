package com.invoicefinancing.controller;

import com.invoicefinancing.entity.KYCDocument;
import com.invoicefinancing.entity.User;
import com.invoicefinancing.repository.KYCDocumentRepository;
import com.invoicefinancing.repository.UserRepository;
import com.invoicefinancing.service.FileUploadService;
import com.invoicefinancing.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/kyc")
public class KYCController {

    @Autowired
    private KYCDocumentRepository kycDocumentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileUploadService fileUploadService;

    private final String UPLOAD_DIR = "uploads/kyc/";

    @PostMapping("/upload-pan")
    public ResponseEntity<?> uploadPAN(@RequestParam("file") MultipartFile file,
                                     @RequestParam("documentNumber") String documentNumber,
                                     Authentication authentication) {
        return uploadDocument(file, documentNumber, KYCDocument.DocumentType.PAN, authentication);
    }

    @PostMapping("/upload-aadhaar")
    public ResponseEntity<?> uploadAadhaar(@RequestParam("file") MultipartFile file,
                                         @RequestParam("documentNumber") String documentNumber,
                                         Authentication authentication) {
        return uploadDocument(file, documentNumber, KYCDocument.DocumentType.AADHAAR, authentication);
    }

    @PostMapping("/upload-gstin")
    public ResponseEntity<?> uploadGSTIN(@RequestParam("file") MultipartFile file,
                                       @RequestParam("documentNumber") String documentNumber,
                                       Authentication authentication) {
        return uploadDocument(file, documentNumber, KYCDocument.DocumentType.GSTIN, authentication);
    }

    private ResponseEntity<?> uploadDocument(MultipartFile file, String documentNumber, 
                                           KYCDocument.DocumentType documentType, Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            String documentUrl = fileUploadService.uploadKYCDocument(file);

            // Check if document already exists for this user and type
            KYCDocument existingDoc = kycDocumentRepository.findByUserAndDocumentType(user, documentType).orElse(null);
            
            KYCDocument kycDocument;
            if (existingDoc != null) {
                kycDocument = existingDoc;
                kycDocument.setStatus(KYCDocument.DocumentStatus.PENDING); // Reset status for re-verification
            } else {
                kycDocument = new KYCDocument();
                kycDocument.setUser(user);
                kycDocument.setDocumentType(documentType);
            }
            
            kycDocument.setDocumentUrl(documentUrl);
            kycDocument.setDocumentNumber(documentNumber);
            kycDocumentRepository.save(kycDocument);

            Map<String, Object> response = new HashMap<>();
            response.put("message", documentType.name() + " document uploaded successfully!");
            response.put("documentId", kycDocument.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Failed to upload document: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getKYCStatus(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<KYCDocument> documents = kycDocumentRepository.findByUser(user);
        
        Map<String, Object> response = new HashMap<>();
        response.put("kycStatus", user.getKycStatus());
        response.put("isVerified", user.getIsVerified());
        response.put("documents", documents);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/documents")
    public ResponseEntity<?> getKYCDocuments(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId()).orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        List<KYCDocument> documents = kycDocumentRepository.findByUser(user);
        return ResponseEntity.ok(documents);
    }
}