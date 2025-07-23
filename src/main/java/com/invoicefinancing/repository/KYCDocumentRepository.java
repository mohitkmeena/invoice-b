package com.invoicefinancing.repository;

import com.invoicefinancing.entity.KYCDocument;
import com.invoicefinancing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KYCDocumentRepository extends JpaRepository<KYCDocument, Long> {
    List<KYCDocument> findByUser(User user);
    List<KYCDocument> findByStatus(KYCDocument.DocumentStatus status);
    Optional<KYCDocument> findByUserAndDocumentType(User user, KYCDocument.DocumentType documentType);
}