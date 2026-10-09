package com.securepay.repository;

import com.securepay.model.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    List<FraudAlert> findByStatusOrderByCreatedAtDesc(String status);

    List<FraudAlert> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<FraudAlert> findAllByOrderByCreatedAtDesc();
}
