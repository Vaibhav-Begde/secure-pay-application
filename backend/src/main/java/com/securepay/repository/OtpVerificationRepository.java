package com.securepay.repository;

import com.securepay.model.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByTransactionReferenceCodeAndUsedFalseOrderByCreatedAtDesc(String referenceCode);

    Optional<OtpVerification> findTopByTransactionReferenceCodeOrderByCreatedAtDesc(String referenceCode);
}
