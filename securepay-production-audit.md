# SecurePay — Production Security, Fraud Engine & Transaction Integrity Audit

## Role and objective
You are a Senior Software Engineer, Application Security Engineer, and Financial-System QA Engineer auditing the existing SecurePay application.

**Stack:** Spring Boot, React, MySQL, JWT, Spring Security/RBAC, money transfer, rule-based fraud detection, risk scoring, OTP, and face verification.

**Objective:** Make the existing application secure, transactionally correct, testable, maintainable, and production-oriented without unnecessary rewrites or breaking existing functionality.

## Non-negotiable workflow

For every important issue, follow this order:

1. Discover
2. Reproduce
3. Write a failing test
4. Apply the smallest safe fix
5. Verify the test passes
6. Run related and regression tests
7. Document the result

Rules:

- Do not modify code during Phase 1.
- Do not assume the current architecture or security controls.
- Do not trust client-controlled fields, including `balance`, `riskScore`, `riskLevel`, `otpVerified`, `faceVerified`, `transactionStatus`, `userId`, `role`, or sender identity.
- Do not claim a vulnerability is fixed unless an automated test proves it.
- Do not weaken security to make tests pass.
- Preserve API contracts where reasonably possible; prefer additive and backward-compatible changes.
- If a control cannot be verified, mark it **UNVERIFIED**. If a test cannot run, mark it **NOT TESTED**.
- If a production-grade control cannot be implemented within the architecture, document the limitation; do not claim it is secure.

Use these statuses consistently: **VERIFIED**, **UNVERIFIED**, **FAILED**, **FIXED**, **NOT TESTED**, **NOT APPLICABLE**.

---

## Phase 1 — Architecture audit (no code changes)

Inspect the complete repository and create `docs/ARCHITECTURE_AUDIT.md`.

### Backend inventory

Identify exact files/classes for:

- Controllers, services, repositories, entities, DTOs
- Security configuration, JWT generation/validation/filtering, RBAC enforcement
- Exception handling and configuration
- Transfer and balance services
- Fraud rules, rule orchestration, risk scoring, risk decisions
- OTP and face-verification services
- Audit logging

### Frontend inventory

Identify:

- Login, dashboard, transfer, OTP, face-verification, transaction-history pages
- Admin and fraud-analyst UI
- API clients, token storage, route guards, error handling
- Any client-controlled security flags or financial values

### Database inventory

Inspect tables and migrations for:

- Users/customers, roles, accounts, transactions, beneficiaries
- Fraud evaluations/rule results, OTP records, face-verification records, audit logs

Check primary/foreign keys, unique constraints, indexes, nullability, money types, timestamps, transaction statuses, and integrity constraints.

### Required architecture report contents

Document:

1. Current architecture and package/module map
2. Actual transfer request flow
3. Fraud detection and risk-decision flow
4. Authentication and authorization flows
5. OTP and face-verification flows
6. Database relationships and integrity controls
7. Transaction state flow
8. Existing fraud rules, scoring, and thresholds
9. Existing security controls
10. Known weaknesses and highest-risk areas

After Phase 1, report exact file/class references and propose only the smallest safe next changes. Do not fabricate findings.

---

## Phase 2 — Money-integrity tests (highest priority)

Trace and test the actual transfer implementation before changing it.

| Test | Inputs / scenario | Expected result | Severity if failed |
|---|---|---|---|
| Negative amount | `-100`, `-1` | HTTP 400/422; no balance change; no successful transfer | Critical/High |
| Zero amount | `0` | Rejected; no money movement | High |
| Decimal precision | `0.01`, `1.99`, `100.50`, `999.99` | Money uses `BigDecimal` and suitable SQL `DECIMAL`; no floating point | High/Critical |
| Fake balance | Client submits `balance=1000000` | Backend ignores it and reads authoritative balance | Critical |
| Self transfer | sender equals receiver | Rejected unless explicitly supported by business rules | High |
| Insufficient balance | balance = amount - 1 | Rejected; no partial movement | Critical |
| Atomic failure | Fail after debit, before credit/save | Full rollback; no partial transfer | Critical |
| Concurrent transfer | Balance ₹10,000; two simultaneous ₹8,000 transfers | At most one succeeds, or funds are safely reserved | Critical |
| Duplicate request | Same idempotency key and request | Same transaction/result; no duplicate movement | Critical |

Search all relevant monetary code for `double`, `Double`, `float`, and `Float`. Floating-point usage for balances, transfer amounts, or fraud amount calculations is a high or critical finding depending on exploitability.

### Concurrency requirements

Determine the existing strategy before changing it. Evaluate the appropriate solution for the current data model:

- Pessimistic locking
- Optimistic locking/version fields
- Atomic conditional SQL update
- Transaction isolation strategy
- Safe reservation model

Do not add locks blindly. Tests must prove the selected approach prevents double spending under a real transactional database, not only mocks.

### Idempotency requirements

Test:

- Same key plus same request
- Same key plus different request
- Missing key
- Expired/reused key
- Retry after timeout/network failure

If idempotency is absent, classify based on actual transfer duplication risk and document the smallest backward-compatible implementation.

Create `docs/TRANSACTION_INTEGRITY_REPORT.md` with test evidence and findings.

---

## Phase 3 — Authentication, authorization, and request tampering

### IDOR and horizontal access control

Create User A and User B. Authenticate as User A and test User B resources:

- `GET /transactions/{user-b-transaction}`
- `GET /accounts/{user-b-account}`
- `GET /users/{user-b}`
- Applicable `POST`, `PUT`, `PATCH`, and `DELETE` operations

Expected result: `403` or safe `404`; User A must never access or modify User B financial data.

### Vertical privilege escalation

Test server-side authorization for:

- CUSTOMER to FRAUD_ANALYST endpoints
- CUSTOMER to ADMIN endpoints
- FRAUD_ANALYST to ADMIN endpoints

Expected result: `403 Forbidden`. Frontend route guards are not sufficient.

### Request tampering

Using API tests/DevTools/Postman, modify:

- `senderId`, `receiverId`, `amount`, `balance`
- `riskScore`, `riskLevel`, `otpVerified`, `faceVerified`
- `transactionStatus`, `userId`, `role`

Expected result: server derives authenticated user from security context/JWT and calculates all risk, verification, balance, and state values itself. Unauthorized fields must be ignored or rejected.

### JWT audit

Audit generation, signing algorithm/key strength, validation, expiration, claims, refresh behavior, storage, and role extraction.

Test:

- Missing token
- Expired token
- Modified token
- Invalid signature
- Malformed token
- Wrong issuer/audience where configured
- Wrong role

Verify claims as applicable: `iss`, `sub`, `exp`, `iat`, `aud`, `roles`.

Secrets must not be hardcoded. Verify environment/deployment secret management and document the actual signing approach rather than assuming a particular algorithm.

---

## Phase 4 — OTP and face verification

### OTP audit and tests

Audit generation, delivery, storage, expiration, verification, attempt counter, resend behavior, rate limiting, and invalidation.

Test:

- Correct OTP
- Wrong OTP
- Expired OTP
- Reused/already-used OTP
- Too many attempts
- Multiple concurrent OTP requests
- OTP resend abuse

Required controls:

- Cryptographically secure generation
- Short configurable expiration
- Configurable maximum attempts and request limits
- One-time use and invalidation after success
- No OTP in production API responses or logs
- Prefer secure hashed storage over plaintext where practical

### Face verification audit and tests

Determine the real implementation. Never accept client input such as `faceVerified=true` as proof.

A successful verification must be server-validated and bound to:

- `userId`
- `transactionId`
- `verificationId`
- timestamp/expiration
- session or equivalent authentication context

Test replay and tampering:

- Verify Transaction A, then reuse result/token for Transaction B
- Wrong user
- Wrong transaction
- Expired verification
- Duplicate verification
- Modified verification ID

Expected result: all invalid/replayed attempts are rejected.

Document whether the implementation includes face matching, liveness detection, anti-spoofing, replay protection, image-quality validation, and secure biometric-data handling.

If liveness detection is absent, include this exact limitation in the findings:

> **LIMITATION:** The current implementation does not provide production-grade liveness/anti-spoofing protection. Basic image matching must not be presented as secure biometric authentication.

---

## Phase 5 — Fraud engine audit

Create `docs/FRAUD_RULE_INVENTORY.md`.

For every implemented rule document:

- Rule name and code
- Purpose and input data
- Threshold, score contribution, priority, and decision behavior
- Hard block versus scoring rule
- Configuration source and version
- Database queries and indexes used
- Existing tests and missing tests
- Known limitations, false-positive risks, and false-negative risks

### Required-rule audit matrix

Do not add missing rules immediately. First determine whether the current business design requires them and whether they can be implemented safely.

| Rule | Verify | Boundary scenarios |
|---|---|---|
| Large transaction | Exists, configurable threshold/score, exact comparison semantics | ₹9,999 / ₹10,000 / ₹10,001 |
| Velocity | User/beneficiary scope, success/failure handling, indexed time query | limit-1 / limit / limit+1 |
| New beneficiary | Configurable window and exact boundary behavior | 1 sec / 5 min / 23h59m / 24h / 7d |
| Unusual amount | Historical average, max, recent behavior, frequency | normal / 2x / 5x / 10x average / exceeds max |
| Account age | Configurable age window | 1 min / 1d / 6d23h / 7d / 30d |
| Failed login | Scope and time window per user/IP/device | 1 / 3 / 5 / 10 failures |
| Device risk | Trust model for device identity | known / new / rapid switch |
| Location/IP risk | Trusted proxy/IP extraction and location logic | normal / new / rapid change |
| Blacklist | Sender, receiver, beneficiary, account | each blocked entity |

### Risk-score and decision audit

Verify:

- Individual scores, total, thresholds, priority, configuration source/version
- Score range and clamping only if 0–100 is the intended domain
- LOW/MEDIUM/HIGH/BLOCKED actions
- Hard blocks happen before money movement
- Fraud-service failures fail safely according to business policy; critical failures must not silently approve a transfer
- Overlap between rules, such as large amount and unusual amount

For overlapping rules, document whether combined scoring is intentional or accidental double-counting; do not remove rules merely to lower scores.

Hard failures such as invalid authentication, blocked accounts, insufficient balance, tampered verification, expired required verification, unauthorized access, or invalid state must be rejected/blocked rather than merely increasing risk.

If the existing architecture already has modular rule behavior, preserve it. Only introduce a common `FraudRule` contract if it improves correctness/testability without unnecessary refactoring.

```java
public interface FraudRule {
    FraudRuleResult evaluate(TransactionContext context);
    String getRuleCode();
    int getPriority();
}
```

### Fraud tests per rule

Create `docs/FRAUD_TEST_MATRIX.md`. Every rule needs:

1. Positive test
2. Negative test
3. Boundary test
4. Null-input test
5. Invalid-input test
6. Combination test
7. Regression test proving legitimate transfer behavior remains intact

Also analyze:

- False positives: salary, rent, legitimate high-value transfer, new phone, travel, new legitimate beneficiary, several legitimate purchases
- False negatives: new account + new device + new beneficiary + large amount + rapid transfers

For each scenario document triggered rules, total score, expected decision, actual decision, and recommendation.

---

## Phase 6 — Transaction state, validation, and safe failures

### Transaction state machine

Identify the actual state machine. A preferred conceptual flow is:

`INITIATED -> FRAUD_CHECK -> VERIFICATION_REQUIRED -> VERIFIED -> PROCESSING -> SUCCESS`

Potential terminal/failure states: `FAILED`, `REJECTED`, `BLOCKED`, `EXPIRED`.

Test and reject undefined transitions, especially:

- `SUCCESS -> INITIATED`
- `SUCCESS -> PROCESSING`
- `FAILED -> SUCCESS`

unless a formally authorized business workflow defines them.

### Server-side validation

Validate at minimum:

- Non-null positive amount
- Sender and receiver exist and are active
- Sender is not receiver unless explicitly supported
- Beneficiary is valid
- Account is not blocked
- Authoritative balance is sufficient
- Currency is supported and enforced
- Authenticated principal is permitted to initiate the transfer

### Errors and HTTP behavior

Audit global exception handling. Do not disclose stack traces, SQL errors, secrets, JWT details, internal class names, or credentials.

Use structured safe responses, for example:

```json
{
  "success": false,
  "code": "TRANSFER_FAILED",
  "message": "The transfer could not be completed."
}
```

Use appropriate status codes: `400`, `401`, `403`, `404`, `409`, `422`, `429`, `500`. Do not return `200 OK` for all failures.

---

## Phase 7 — Application security hardening

### Injection and XSS

Test SQL injection in search, beneficiary, name, email, and transaction reference inputs using representative payloads such as `' OR 1=1 --`.

Verify parameterized JPA/SQL queries and reject unsafe query concatenation.

Test XSS payloads such as `<script>alert(1)</script>` in names, beneficiary names, notes, descriptions, and search fields. Verify React-safe rendering, no unsanitized `dangerouslySetInnerHTML`, and safe handling of stored input.

### CORS, CSRF, and rate limiting

Audit CORS. Reject wildcard origins combined with credentials. Configure explicit trusted production origins.

Determine CSRF exposure from the actual JWT storage architecture:

- If JWT is cookie-based, evaluate CSRF protection.
- If JWT is in authorization headers, evaluate XSS/token-exposure controls.

Audit and test configurable rate limits for login, OTP request/verification, transfer, beneficiary creation, face verification, and password reset. Test burst behavior without imposing limits that break legitimate expected usage.

### Audit logging and traceability

Verify immutable/security-focused audit events for:

- `LOGIN_SUCCESS`, `LOGIN_FAILURE`
- `TRANSFER_CREATED`, `TRANSFER_BLOCKED`, `TRANSFER_SUCCESS`, `TRANSFER_FAILED`
- `FRAUD_RULE_TRIGGERED`
- `OTP_REQUESTED`, `OTP_VERIFIED`, `OTP_FAILED`
- `FACE_VERIFICATION_STARTED`, `FACE_VERIFICATION_SUCCESS`, `FACE_VERIFICATION_FAILED`
- `ADMIN_ACTION`

Never log passwords, OTPs, JWTs, private keys, secrets, or biometric payloads. Sanitize untrusted values before logging.

Every fraud decision should be traceable with minimum necessary data: transaction ID, timestamp, risk score/level, triggered rules and contributions, decision, verification requirement, and fraud-engine/configuration version.

---

## Phase 8 — Database, performance, dependencies, and secrets

### Database and performance

Check fraud/transfer queries for N+1 patterns, repeated queries, unindexed filters, unnecessary joins/entity loading, and large history scans.

Review actual query plans before adding indexes. Evaluate fields used by real queries, likely including `user_id`, `sender_id`, `receiver_id`, `beneficiary_id`, `created_at`, and `status`.

Check database-level protections: foreign keys, unique constraints, required fields, balance invariants where appropriate, transaction references, and state/status constraints. Application validation alone is insufficient for critical invariants.

### Time testability

Search for uncontrolled `LocalDateTime.now()` and `Instant.now()` calls in time-sensitive business logic. Prefer injected `Clock` where deterministic testing is needed; do not refactor unrelated time calls merely for style.

### Configuration and secrets

Audit application configuration, Docker/deployment files, environment variables, logging configuration, database credentials, JWT configuration, CORS, `.env` files, frontend configuration, and Git history where available.

Search for: `password=`, `secret=`, `apiKey=`, `token=`, `privateKey=`, `jwtSecret=`.

Use environment/deployment secret management. Do not commit production secrets. Prefer separate development, test, and production configuration profiles where appropriate.

### Dependency audit

Inspect backend and frontend dependency manifests/lockfiles for outdated or vulnerable dependencies, including Spring Boot/Security, Hibernate, MySQL connector, JWT libraries, React, and npm packages.

Do not upgrade everything blindly. Upgrade focused dependency groups and run regression tests after each change.

---

## Phase 9 — Test execution and deliverables

Use a non-production test environment with synthetic users, accounts, transactions, beneficiaries, OTP provider, and face-verification integration. Use a real transactional database environment for concurrency tests.

Run tests in this order:

1. Compile/build
2. Unit tests
3. Fraud-rule tests
4. Service tests
5. Repository tests
6. Integration tests
7. Security/API tests
8. Concurrency tests
9. Regression tests
10. Performance tests

Investigate failing earlier layers before declaring later layers successful.

Create:

```text
docs/
├── ARCHITECTURE_AUDIT.md
├── SECURITY_FINDINGS.md
├── FRAUD_RULE_INVENTORY.md
├── FRAUD_TEST_MATRIX.md
├── TRANSACTION_INTEGRITY_REPORT.md
└── PRODUCTION_READINESS_REPORT.md
```

### Security finding template

```markdown
## [CRITICAL] Concurrent Transfer Double Spend

**Location:** `TransferService.java` / exact method

**Status:** FAILED | FIXED | UNVERIFIED | NOT TESTED

**Problem:** Two simultaneous transfer requests can read the same balance before either commits.

**Reproduction:**
1. Set account balance to ₹10,000.
2. Submit two ₹8,000 requests simultaneously.
3. Record transaction outcomes and final balance.

**Expected:** Only one transfer succeeds, or funds are safely reserved.

**Actual:** [Observed evidence only]

**Impact:** Potential financial loss, duplicate movement, or negative balance.

**Test added:** `ConcurrentTransferTest`.

**Smallest safe fix:** [Implementation-specific recommendation].

**Regression evidence:** [Test names/results].
```

### Production readiness report

Include:

- Overall status: **PRODUCTION READY** or **NOT PRODUCTION READY**
- Count and list of critical/high/medium/low findings
- Fraud rules: implemented, missing, incorrect, unverified
- False positives and false negatives
- Money-integrity, security, and regression test results
- Performance findings
- Known limitations
- Remaining production blockers

---

## Production blockers

Do not claim **PRODUCTION READY** if any applicable item remains failed or unverified:

- Client can manipulate transfer amount, balance, risk score/level, OTP status, face-verification status, transaction status, user identity, or role
- Authorization bypass or IDOR exists
- Concurrent transfer can double spend
- Duplicate requests can cause duplicate money movement
- Negative or zero amount is accepted
- Fraud/security failure silently approves transfer
- Secrets are exposed or passwords are insecurely stored
- SQL injection exists
- Critical authentication or authorization bypass exists

## Final instruction

Start with **Phase 1 only**. Make no code changes until the repository architecture and existing implementation have been inspected and documented.

After Phase 1, report what was found, identify the highest-risk exact files/classes, propose smallest safe changes, and only then proceed to tests and fixes.

The goal is not code that merely appears production-ready. The goal is money-transfer behavior that is demonstrably secure and transactionally correct.
