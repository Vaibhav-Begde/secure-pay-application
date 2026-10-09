package com.securepay.repository;

import com.securepay.model.UserRiskProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRiskProfileRepository extends JpaRepository<UserRiskProfile, Long> {

    Optional<UserRiskProfile> findByUserId(Long userId);

    Optional<UserRiskProfile> findByUserUsername(String username);
}
