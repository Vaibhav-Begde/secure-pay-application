# SecurePay — Architecture Audit (Phase 1)

> **Date:** 2026-10-08
> **Status:** Phase 1 complete — NO code changes made

---

## 1. Package / Module Map

### Backend Controllers
- AuthController — /api/auth/** (login, register, me, tx-pin)
- AdminFraudRuleController — /api/admin/fraud-rules (ADMIN only, @PreAuthorize)
- FaceVerificationController — /api/face-verification/configure|verify
- FraudAnalystController — /api/analyst/** (FRAUD_ANALYST|ADMIN)
- OtpController — /api/otp/send|verify
- TransactionController — /api/transactions/evaluate|transfer|history
- WalletController — /api/wallet/balance

### Backend Services
- AuthServiceImpl — register, login, getCurrentUser, setTransactionPIN
- TransactionServiceImpl — transferMoney, completeOtpVerification, approveTransaction
- FraudDetectionServiceImpl — evaluateTransaction (7 rules), recordFraudAlert
- OtpServiceImpl — generateAndSendOtp, verifyOtp (BCrypt, SecureRandom, max 3 attempts)
- FaceVerificationServiceImpl — configureFace (stores to User.referenceFace), verifyFace (MOCK)
- WalletServiceImpl — getBalance
- EmailServiceImpl — SMTP OTP delivery
- UserRiskProfileServiceImpl — profile tracking for anomaly rules

### Entities
- User — id, username, email, passwordHash, role, txPinHash, referenceFace(LONGTEXT)
- Wallet — id, user(1:1), balance(DECIMAL 19,2), currency
- Transaction — id, referenceCode(unique), sender, receiver, amount, status, riskScore
- TransactionStatus — PENDING, COMPLETED, FAILED, BLOCKED, VERIFICATION_REQUIRED
- OtpVerification — id, transaction, user, otpHash(BCrypt), expiresAt, attempts, used
- FraudAlert — linked to transaction
- FraudRule — ruleCode, riskPoints, enabled (DB-configurable)
- UserRiskProfile — sender history stats (avg amount, usual device/location/hour)
- Role — CUSTOMER, FRAUD_ANALYST, ADMIN

### Security Layer
- JwtTokenProvider — HMAC-SHA, claims: sub, userId, role, iat, exp(24h)
- JwtAuthenticationFilter — OncePerRequestFilter, Bearer header extraction
- JwtAuthenticationEntryPoint — 401 responses
- JwtAccessDeniedHandler — 403 responses
- CustomUserDetailsService — loads user for Spring Security

---

## 2. Transfer Flow

POST /api/transactions/transfer (CUSTOMER JWT)
  -> Sender identity from JWT (never from request body)
  -> Validate amount > 0
  -> Verify sender has transactionPinHash, match BCrypt
  -> Reject self-transfer
  -> FraudDetectionService.evaluateTransaction()
  ->  HIGH (>=70): Save BLOCKED, send OTP, no money moved
  ->  MEDIUM (40-69): Save VERIFICATION_REQUIRED, send OTP, no money moved
  ->  LOW (0-39): PESSIMISTIC_WRITE lock wallets, check balance, BigDecimal subtract/add, Save COMPLETED

---

## 3. Fraud Detection Flow

FraudDetectionServiceImpl evaluates 7 DB-configurable rules:
  HIGH_AMOUNT     — amount>=10k: +40pts, amount>=50k: +65pts, amount>=3x avg: +base
  NEW_RECEIVER    — no prior completed tx to receiver: +15pts
  NEW_DEVICE      — deviceId != profile.usualDevice: +20pts
  NEW_LOCATION    — location != profile.usualLocation: +15pts
  RAPID_TRANSACTIONS — >=2 tx in 5min: +20pts
  UNUSUAL_TIME    — 1AM-5AM IST or >8h from usual hour: +10pts
  PREVIOUS_FRAUD  — prior BLOCKED/FAILED tx exists: +30pts

Thresholds: LOW 0-39, MEDIUM 40-69, HIGH 70+

---

## 4. Auth & Authorization Flow

Login -> DaoAuthProvider -> JwtTokenProvider.generateToken(sub, userId, role, exp)
All requests -> JwtAuthenticationFilter validates token -> SecurityContextHolder set
RBAC: /api/auth/** permitAll | /api/admin/** ADMIN | /api/analyst/** FRAUD_ANALYST|ADMIN | /api/** authenticated

---

## 5. OTP Flow

generateAndSendOtp: SecureRandom 6-digit -> BCrypt hash stored (no plaintext) -> expiresAt=now+300s -> prev OTPs invalidated -> email sent
verifyOtp: ownership check -> expiry check -> max 3 attempts -> BCrypt.matches -> used=true on success -> completeOtpVerification()

---

## 6. Face Verification Flow

configure: base64 image stored in User.referenceFace (LONGTEXT)
verify: owner check -> description string-gate "Awaiting Face Verification" -> calculateMockMatch() 95% random pass -> PENDING/BLOCKED
PENDING requires analyst manual approve -> COMPLETED

---

## 7. Transaction State Machine

INITIATED -> LOW risk -> COMPLETED
INITIATED -> MEDIUM risk -> VERIFICATION_REQUIRED -> [verifyOtp] -> COMPLETED
INITIATED -> HIGH risk -> BLOCKED -> [verifyOtp] -> BLOCKED(description update) -> [verifyFace] -> PENDING -> [analyst approve] -> COMPLETED
Any -> insufficient balance -> FAILED

---

## 8. Existing Fraud Rules

Rule            | Condition                        | Score
HIGH_AMOUNT     | amount >= 10,000                 | +40
HIGH_AMOUNT     | amount >= 50,000                 | +65
HIGH_AMOUNT     | amount >= 3x sender average      | +base
NEW_RECEIVER    | no prior completed tx to receiver| +15
NEW_DEVICE      | unrecognized device              | +20
NEW_LOCATION    | unrecognized location            | +15
RAPID_TXNS      | >=2 tx in 5 minutes              | +20
UNUSUAL_TIME    | 1AM-5AM IST                      | +10
PREVIOUS_FRAUD  | prior blocked/failed tx          | +30

---

## 9. Verified Security Controls

JWT HMAC-SHA signing                     VERIFIED
JWT validated every request              VERIFIED
RBAC SecurityConfig + @PreAuthorize      VERIFIED
Sender from JWT not request body         VERIFIED
BCrypt passwords                         VERIFIED
BCrypt OTP (no plaintext in DB)          VERIFIED
OTP 5-min expiry                         VERIFIED
OTP max 3 attempts                       VERIFIED
OTP one-time use                         VERIFIED
OTP invalidated on resend                VERIFIED
SecureRandom OTP generation              VERIFIED
PESSIMISTIC_WRITE wallet lock            VERIFIED
BigDecimal all monetary values           VERIFIED
DECIMAL(19,2) DB columns                 VERIFIED
Self-transfer rejection                  VERIFIED
Amount > 0 server-side validation        VERIFIED
Transaction PIN required for transfers   VERIFIED
GlobalExceptionHandler no stack traces   VERIFIED (partial, see C5)
CORS restricted to localhost             VERIFIED (dev only)
No raw OTP in logs                       VERIFIED
No float/double for money                VERIFIED

---

## 10. Findings

### CRITICAL

C1 — Hardcoded DB password
  Location: application.yml:10 (password: Vaibhav2034)
  Status: FAILED
  Fix: Remove hardcoded fallback, require SPRING_DATASOURCE_PASSWORD env var

C2 — Hardcoded email app password
  Location: application.yml:24 (ebfkyrwzjircurne Gmail app password in source)
  Status: FAILED
  Fix: Require SPRING_MAIL_PASSWORD env var with no hardcoded fallback

C3 — Hardcoded JWT secret
  Location: application.yml:38 (static hex fallback)
  Status: FAILED
  Fix: Require APP_JWT_SECRET env var with no hardcoded fallback

C4 — Face verification is mock/simulated
  Location: FaceVerificationServiceImpl.java (calculateMockMatch)
  Status: UNVERIFIED as real biometric control
  LIMITATION: Current implementation uses 95% random probability. No actual biometric
  comparison, liveness detection, or anti-spoofing. Must not be presented as secure
  biometric authentication.

C5 — Internal error messages leak in 500 responses
  Location: GlobalExceptionHandler.java:118 (ex.getMessage() returned to client)
  Status: FAILED
  Fix: Return generic message for HTTP 500; log detail server-side only

### HIGH

H1 — Risk score not clamped to max 100
  Location: FraudDetectionServiceImpl.java
  Status: UNVERIFIED (no test proves clamping)

H2 — No rate limiting on OTP resend
  Location: OtpController.java POST /api/otp/send
  Status: FAILED (unlimited OTP spam possible)

H3 — No rate limiting on login
  Location: AuthController.java POST /api/auth/login
  Status: FAILED (brute force possible)

H4 — HIGH_AMOUNT adds base + tier bonus (potential double-count)
  Location: FraudDetectionServiceImpl.java:63-91
  Status: UNVERIFIED (may be intentional; needs test to confirm expected behavior)

H5 — approveTransaction has no idempotency guard
  Location: TransactionServiceImpl.java approveTransaction()
  Status: FAILED (analyst can double-approve -> duplicate balance credit)

H6 — Face gate uses description string-match (brittle control)
  Location: FaceVerificationServiceImpl.java:50
  Status: FAILED (bypassed if description changed; not a reliable state gate)

H7 — No idempotency key on transfer endpoint
  Location: TransactionController.java
  Status: UNVERIFIED (network retry risk for LOW-risk path)

H8 — CORS allows all localhost ports
  Location: SecurityConfig.java
  Status: NOT APPLICABLE (dev) / FAILED (production)

### MEDIUM

M1 — show-sql: true and SQL DEBUG logging will leak in production
  Location: application.yml:15,46

M2 — Frontend hardcodes point values instead of reading from backend
  Location: TransferPage.jsx:356-394

M3 — No persistent audit log table; events only in application logs

M4 — ddl-auto: update unsafe for production schema management
  Location: application.yml:14

M5 — referenceFace biometric data stored in main users table as raw base64

M6 — JWT has no iss or aud claims
  Location: JwtTokenProvider.java:44-51

M7 — format_sql and DEBUG hibernate logging enabled in main config

---

## Phase 2 Priority Order

1. C1/C2/C3 — Move secrets to environment variables (config only, no code logic change)
2. C5 — Sanitize 500 response message
3. H5 — Guard approveTransaction against double-approval (add COMPLETED status check)
4. H6 — Replace description string-match with proper status/enum-based gate
5. H1 — Clamp totalRiskScore to 100 in FraudDetectionServiceImpl
6. H2/H3 — Add rate limiting on OTP and login endpoints
7. M1/M4 — Create separate production application profile
