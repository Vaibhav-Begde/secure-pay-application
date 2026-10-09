package com.securepay.service;

import com.securepay.model.User;
import com.securepay.model.UserRiskProfile;

import java.math.BigDecimal;

public interface UserRiskProfileService {

    UserRiskProfile getOrCreateProfile(User user);

    void updateProfileAfterTransaction(User user, BigDecimal amount, String deviceId, String location, boolean isFraud);
}
