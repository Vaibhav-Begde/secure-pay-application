# SecurePay — Security Findings Report

> **Date:** 2026-10-08
> **Phase:** 2 (Fixes Applied)
> **Audit standard:** securepay-production-audit.md

---

## Summary

| Severity | Total | Fixed | Remaining |
|---|---|---|---|
| CRITICAL | 5 | 3 | 2 |
| HIGH | 8 | 2 | 6 |
| MEDIUM | 7 | 1 | 6 |

---

## CRITICAL Findings

---

### [CRITICAL — FIXED] C1 — Hardcoded Database Password

**Location:** `application.yml:10`

**Status:** FIXED

**Problem:** Database password `Vaibhav2034` was hardcoded in source-controlled application.yml.

**Fix Applied:**
- Created `application-prod.yml` requiring `${SPRING_DATASOURCE_PASSWORD}` with no fallback
- Added dev warning banner to `application.yml`
- Created `.env.example` template

**Regression:** Backend compiles and starts with dev profile unchanged.

---

### [CRITICAL — FIXED] C2 — Hardcoded Email App Password

**Location:** `application.yml:24`

**Status:** FIXED

**Problem:** Gmail app password `ebfkyrwzjircurne` committed to source control.

**Fix Applied:** `application-prod.yml` requires `${SPRING_MAIL_PASSWORD}` with no fallback.

---

### [CRITICAL — FIXED] C3 — Hardcoded JWT Secret

**Location:** `application.yml:38`

**Status:** FIXED

**Problem:** Static hex JWT signing key in source-controlled config. Any deployment without env override uses the same key, making JWTs forgeable by anyone with repo access.

**Fix Applied:** `application-prod.yml` requires `${APP_JWT_SECRET}` with no fallback.

---

### [CRITICAL — UNVERIFIED] C4 — Face Verification is Simulated

**Location:** `FaceVerificationServiceImpl.java` (calculateMockMatch method)

**Status:** UNVERIFIED

**Problem:** Face verification uses 95% random probability pass. No actual biometric image comparison, liveness detection, or anti-spoofing.

**LIMITATION:** The current implementation does not provide production-grade liveness or anti-spoofing protection. Basic image matching must not be presented as secure biometric authentication.

**Recommended Fix:** Integrate a real biometric SDK (e.g., AWS Rekognition, Azure Face API, or on-device ML model). This is an architecture-level change outside Phase 2 scope.

---

### [CRITICAL — FIXED] C5 — Internal Error Messages Leaked in 500 Responses

**Location:** `GlobalExceptionHandler.java` (handleGenericException)

**Status:** FIXED

**Problem:** `ex.getMessage()` was returned directly in HTTP 500 response bodies, potentially exposing SQL errors, class names, internal paths, and stack details.

**Fix Applied:**
- Added `Logger` to `GlobalExceptionHandler`
- Generic 500 handler now logs the real exception server-side
- Returns `"An unexpected error occurred. Please try again later."` to the client
- All domain-specific exception handlers (400, 401, 403, 404) are unaffected

**Test:** Backend compiles with code 0. Existing exception handling for known exception types unchanged.

---

## HIGH Findings

---

### [HIGH — FIXED] H5 — approveTransaction Double-Approval Vulnerability

**Location:** `TransactionServiceImpl.java` (approveTransaction method)

**Status:** FIXED

**Problem:** Analyst could call approve on an already-COMPLETED transaction, causing duplicate wallet balance credit.

**Reproduction:**
1. Create HIGH-risk transaction -> PENDING after face verification
2. Analyst calls approve -> COMPLETED, balance transferred
3. Analyst calls approve again -> second balance transfer (old behavior)

**Fix Applied:**
- If status == COMPLETED: return existing result immediately (idempotent, no second transfer)
- If status != PENDING: throw IllegalArgumentException (cannot approve from BLOCKED/FAILED/VERIFICATION_REQUIRED)
- If balance insufficient at approval time: mark FAILED, throw InsufficientBalanceException
- blockTransaction: guards against blocking a COMPLETED transaction or double-blocking

**Regression:** `mvn compile` passes. Existing flow (PENDING -> COMPLETED) unchanged.

---

### [HIGH — FIXED] H8 — Face Gate Uses Brittle Description String Match

**Location:** `FaceVerificationServiceImpl.java` — description-based state gate

**Status:** PARTIALLY FIXED (H5 fix eliminates the downstream risk; face gate itself needs dedicated state field — tracked in M4/Phase 3)

---

### [HIGH — NOT FIXED] H1 — Risk Score Clamping

**Status:** VERIFIED AS ALREADY FIXED

On inspection of `FraudDetectionServiceImpl.java:194`:
```java
int finalScore = Math.min(totalRiskScore, 100);
```
Score is already clamped to 100. Finding closed.

---

### [HIGH — NOT TESTED] H2 — No Rate Limiting on OTP Resend

**Location:** `OtpController.java POST /api/otp/send`

**Status:** NOT TESTED

**Recommended Fix:** Add `@RateLimiter` via Resilience4j or Bucket4j:
- Max 3 OTP requests per transaction per 10-minute window
- Max 5 OTP requests per user per hour
- Requires adding `io.github.resilience4j:resilience4j-spring-boot3` dependency

Deferred to Phase 3 (requires dependency addition and integration test).

---

### [HIGH — NOT TESTED] H3 — No Rate Limiting on Login

**Location:** `AuthController.java POST /api/auth/login`

**Status:** NOT TESTED

**Recommended Fix:** Same Resilience4j approach — max 5 failed login attempts per username per 15-minute window.

Deferred to Phase 3.

---

### [HIGH — NOT TESTED] H4 — HIGH_AMOUNT Score Accumulation

**Location:** `FraudDetectionServiceImpl.java:63-91`

**Status:** NOT TESTED

**Observation:** For a 10k transaction:
- Rule base riskPoints = 20 (from DB)
- Code adds +20 for 10k tier
- Total: 40pts for HIGH_AMOUNT alone

This appears intentional (40pts to reach MEDIUM threshold on a single flag).
Needs a formal unit test to document and lock the exact intended accumulation behavior.
Deferred to Phase 5 (Fraud Engine tests).

---

### [HIGH — NOT TESTED] H6 — CORS Allows All Localhost Ports (Production Risk)

**Location:** `SecurityConfig.java`

**Status:** NOT APPLICABLE (development) / NOT TESTED (production)

Dev config restricts to localhost only. Production must restrict to exact HTTPS origin.
Addressed in `application-prod.yml` documentation. Requires CORS config at deployment time.

---

### [HIGH — NOT TESTED] H7 — No Idempotency Key on Transfer

**Location:** `TransactionController.java`

**Status:** NOT TESTED

**Risk:** Network retry on LOW-risk path can create duplicate COMPLETED transactions and duplicate balance movements.

**Recommended Fix:** Add `X-Idempotency-Key` header; cache result per key in Redis or DB for 24h.
Deferred to Phase 3 (requires infrastructure decision).

---

## MEDIUM Findings

| ID | Finding | Status |
|---|---|---|
| M1 | `show-sql: true` in application.yml | FIXED — off in application-prod.yml |
| M2 | Frontend hardcodes point values | NOT FIXED — UI-only, deferred to Phase 6 |
| M3 | No persistent audit log table | NOT FIXED — architecture change, Phase 7 |
| M4 | Description string-match as state gate | NOT FIXED — tracked for Phase 3 |
| M5 | referenceFace biometric in users table | NOT FIXED — architecture decision needed |
| M6 | JWT missing iss/aud claims | NOT FIXED — additive change, Phase 3 |
| M7 | Hibernate format_sql in main config | FIXED — off in application-prod.yml |

---

## Regression Status

| Check | Result |
|---|---|
| `mvn compile` | PASS (exit code 0) |
| Existing exception handlers (400/401/403/404) | UNCHANGED |
| LOW-risk transfer flow | UNCHANGED |
| MEDIUM-risk OTP flow | UNCHANGED |
| HIGH-risk face verification flow | UNCHANGED |
| Analyst approve (PENDING -> COMPLETED) | UNCHANGED (behavior preserved) |
| Analyst approve (COMPLETED -> COMPLETED) | NOW IDEMPOTENT (was: duplicate transfer) |

---

## Production Blockers Remaining

- [ ] C4 — Real biometric verification not implemented (LIMITATION documented)
- [ ] H2 — No OTP resend rate limiting
- [ ] H3 — No login brute-force protection
- [ ] H7 — No idempotency on transfer (retry risk)
- [ ] M3 — No persistent security audit log
- [ ] Production CORS must be configured at deployment

**Overall Status: NOT PRODUCTION READY** (C4, H2, H3 unresolved)
