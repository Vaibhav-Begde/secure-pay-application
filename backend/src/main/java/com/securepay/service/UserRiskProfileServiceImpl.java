package com.securepay.service;

import com.securepay.model.User;
import com.securepay.model.UserRiskProfile;
import com.securepay.repository.UserRiskProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.ZoneId;

@Service
public class UserRiskProfileServiceImpl implements UserRiskProfileService {

    private final UserRiskProfileRepository riskProfileRepository;
    private static final BigDecimal DEFAULT_AVG_AMOUNT = new BigDecimal("250.00");

    @Autowired
    public UserRiskProfileServiceImpl(UserRiskProfileRepository riskProfileRepository) {
        this.riskProfileRepository = riskProfileRepository;
    }

    @Override
    @Transactional
    public UserRiskProfile getOrCreateProfile(User user) {
        return riskProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserRiskProfile initialProfile = UserRiskProfile.builder()
                            .user(user)
                            .averageTransactionAmount(DEFAULT_AVG_AMOUNT)
                            .totalTransactionCount(0L)
                            .usualLocation("New York, USA")
                            .usualDevice("Web-Browser-Chrome")
                            .usualTransactionHour(LocalTime.now(ZoneId.of("UTC")).getHour())
                            .previousFraudCount(0)
                            .build();
                    return riskProfileRepository.save(initialProfile);
                });
    }

    @Override
    @Transactional
    public void updateProfileAfterTransaction(User user, BigDecimal amount, String deviceId, String location, boolean isFraud) {
        UserRiskProfile profile = getOrCreateProfile(user);

        if (isFraud) {
            profile.setPreviousFraudCount(profile.getPreviousFraudCount() + 1);
        } else if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            long oldCount = profile.getTotalTransactionCount();
            long newCount = oldCount + 1;

            BigDecimal oldTotal = profile.getAverageTransactionAmount().multiply(BigDecimal.valueOf(oldCount));
            BigDecimal newAverage = oldTotal.add(amount).divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP);

            profile.setAverageTransactionAmount(newAverage);
            profile.setTotalTransactionCount(newCount);

            if (deviceId != null && !deviceId.isBlank()) {
                profile.setUsualDevice(deviceId);
            }
            if (location != null && !location.isBlank()) {
                profile.setUsualLocation(location);
            }
            profile.setUsualTransactionHour(LocalTime.now(ZoneId.of("UTC")).getHour());
        }

        riskProfileRepository.save(profile);
    }
}
