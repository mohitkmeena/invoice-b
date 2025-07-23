package com.invoicefinancing.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileUploadService {

    private static final String KYC_UPLOAD_DIR = "uploads/kyc/";
    private static final String INVOICE_UPLOAD_DIR = "uploads/invoices/";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public String uploadKYCDocument(MultipartFile file) throws IOException {
        validateFile(file);
        return uploadFile(file, KYC_UPLOAD_DIR);
    }

    public String uploadInvoiceDocument(MultipartFile file) throws IOException {
        validateFile(file);
        return uploadFile(file, INVOICE_UPLOAD_DIR);
    }

    private String uploadFile(MultipartFile file, String uploadDir) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : "";
        String filename = UUID.randomUUID().toString() + fileExtension;
        
        Path filePath = uploadPath.resolve(filename);
        
        // Save file
        Files.copy(file.getInputStream(), filePath);
        
        return uploadDir + filename;
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("File size exceeds maximum limit of 10MB");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("application/pdf") && 
                                   !contentType.startsWith("image/"))) {
            throw new IOException("Invalid file type. Only PDF and image files are allowed");
        }
    }

    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }

    public boolean fileExists(String filePath) {
        Path path = Paths.get(filePath);
        return Files.exists(path);
    }
}