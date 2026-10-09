package com.securepay.repository;

import com.securepay.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReferenceCode(String referenceCode);

    @Query("SELECT t FROM Transaction t WHERE t.sender.id = :userId OR (t.receiver.id = :userId AND t.status = com.securepay.model.TransactionStatus.COMPLETED) ORDER BY t.createdAt DESC")
    List<Transaction> findUserTransactionHistory(@Param("userId") Long userId);

    @Query("SELECT t FROM Transaction t WHERE t.sender.username = :username OR (t.receiver.username = :username AND t.status = com.securepay.model.TransactionStatus.COMPLETED) ORDER BY t.createdAt DESC")
    List<Transaction> findUserTransactionHistoryByUsername(@Param("username") String username);
}
