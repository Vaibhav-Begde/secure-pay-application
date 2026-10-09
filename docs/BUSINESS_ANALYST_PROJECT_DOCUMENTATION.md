# SecurePay

## Business Analyst Project Documentation

**Document version:** 1.0  
**Document date:** 9 October 2026  
**Product:** SecurePay Money Transfer and Fraud Detection Platform  
**Document status:** Baseline implementation documentation

---

## 1. Executive Summary

SecurePay is a role-based digital money-transfer platform designed to help customers send funds securely while applying real-time fraud-risk evaluation to every transfer. The platform combines wallet management, transaction PIN protection, adaptive OTP verification, configurable fraud rules, analyst review capabilities, and administrative controls.

The system is implemented as a full-stack web application:

- React, Vite, and Tailwind CSS for the user interface.
- Spring Boot, Spring Security, and Java for the backend.
- JPA/Hibernate for persistence.
- MySQL for the application database.
- JWT for stateless authentication.
- SMTP email delivery for OTP challenges.

Face scanning and biometric verification are not part of the current product scope. The implemented security model uses account-password verification for HIGH-risk transfers, transaction PIN verification for transfers, and OTP verification when risk rules require step-up authentication.

## 2. Business Problem

Traditional money-transfer workflows often apply the same level of authentication to every transaction. This creates two problems:

1. Low-risk transfers become unnecessarily difficult for customers.
2. High-risk or unusual transfers may not receive sufficient additional verification.

SecurePay addresses this by evaluating transaction risk before money movement and selecting the appropriate verification path based on the risk outcome.

## 3. Business Objectives

| ID | Objective |
|---|---|
| BO-01 | Allow authenticated customers to transfer money between SecurePay wallets. |
| BO-02 | Evaluate transfer risk using configurable rules and a 0–100 risk score. |
| BO-03 | Apply stronger verification to suspicious or high-value transfers. |
| BO-04 | Prevent unauthorized money movement through account-password and transaction-PIN checks. |
| BO-05 | Provide analysts with a fraud-alert and transaction-investigation workspace. |
| BO-06 | Give administrators control over fraud rules, users, and account status. |
| BO-07 | Maintain a clear transaction history showing Credit/Debit direction and counterparty. |
| BO-08 | Provide customers with account and recent-login security information. |

## 4. Stakeholders and User Roles

### 4.1 Customer

Customers can:

- Register and log in.
- Set or update a six-digit transaction PIN.
- View their virtual wallet balance.
- Evaluate and initiate transfers.
- Complete OTP challenges.
- View transaction history and recent transactions.
- View Credit/Debit direction and counterparty information.
- View account profile and latest login-security details.

### 4.2 Fraud Analyst

Fraud analysts can:

- View risk analytics.
- Review fraud alerts.
- Inspect transaction details and risk factors.
- Approve eligible transactions.
- Block suspicious transactions.
- Mark fraud alerts as reviewed.

### 4.3 Administrator

Administrators can:

- View system-level dashboard metrics.
- Create, update, enable, and delete fraud rules.
- Review the user directory.
- Change user roles and account status according to administrative permissions.

## 5. Scope

### 5.1 In Scope

- Customer registration and authentication.
- JWT-based session handling.
- Role-based access control.
- Customer wallet display.
- Transfer PIN setup and validation.
- Transfer-risk evaluation.
- LOW, MEDIUM, and HIGH risk classification.
- Email OTP generation, expiry, resend, attempt limits, and verification.
- HIGH-risk account-login-password verification.
- Credit/Debit transaction presentation.
- Customer profile and latest-login information.
- Analyst fraud investigation and transaction actions.
- Administrator fraud-rule management.
- Transaction audit information including device, IP, location, risk score, and status.

### 5.2 Out of Scope

- Face scanning, facial recognition, liveness detection, or biometric verification.
- Real-world bank settlement or external payment-network integration.
- Card processing.
- Cash withdrawal.
- Multi-currency settlement.
- Native mobile applications.
- Precise physical geolocation services. The current implementation records a supplied location value and identifies local development access as local.
- Production-grade anti-money-laundering case management.

## 6. Core Business Rules

| ID | Rule |
|---|---|
| BR-01 | A customer must be authenticated before accessing protected application functions. |
| BR-02 | A customer must configure a six-digit transaction PIN before transferring money. |
| BR-03 | The transaction PIN is stored as a BCrypt hash and is never returned to the frontend. |
| BR-04 | The backend validates the transaction PIN for every transfer. |
| BR-05 | Risk is evaluated before wallet balances are changed. |
| BR-06 | LOW-risk transfers may complete without OTP after successful transaction-PIN validation. |
| BR-07 | MEDIUM-risk transfers require email OTP verification. |
| BR-08 | HIGH-risk transfers require the account login password, transaction PIN, and email OTP. |
| BR-09 | OTPs are six digits, hashed before storage, valid for five minutes, and limited to three attempts. |
| BR-10 | A used or expired OTP cannot be reused. |
| BR-11 | A customer cannot transfer money to their own wallet. |
| BR-12 | Insufficient balance prevents completion and records a failed transaction outcome. |
| BR-13 | Customers see their own name as “You” in transaction lists. |
| BR-14 | Credits display the counterparty as the sender; debits display the counterparty as the receiver. |
| BR-15 | The actual transaction PIN cannot be displayed or recovered because only its hash is stored. |

## 7. Risk and Verification Model

### 7.1 Risk Levels

| Risk level | Typical outcome | Customer verification |
|---|---|---|
| LOW | Transfer completes immediately after PIN validation. | Transaction PIN |
| MEDIUM | Transfer enters OTP challenge flow. | Transaction PIN + email OTP |
| HIGH | Transfer is flagged and enters the strongest verification flow. | Account password + transaction PIN + email OTP |

Risk scoring uses configurable fraud rules and produces a score from 0 to 100. The rules consider factors such as transaction amount, receiver history, device, location, transaction velocity, transaction time, and previous fraud history.

### 7.2 HIGH-Risk Transfer Flow

1. Customer enters recipient, amount, and optional note.
2. SecurePay evaluates risk.
3. If the result is HIGH, the customer is asked for:
   - Account login password.
   - Six-digit transaction PIN.
4. The backend validates both credentials.
5. SecurePay sends a one-time code to the registered email address.
6. Customer enters the OTP.
7. The backend verifies expiry, reuse, ownership, and attempt limits.
8. If valid, the transfer is completed according to the current transaction service flow.

Passwords and PINs are never logged or returned in API responses.

## 8. Primary Customer Workflows

### 8.1 Registration and Login

1. Customer submits username, email, and password.
2. The backend validates uniqueness and creates a CUSTOMER account.
3. A wallet and risk profile are initialized.
4. Customer logs in using username/email and password.
5. Backend issues a JWT.
6. Backend records latest login time, device, IP, and location classification.

### 8.2 Transaction PIN Setup

1. Customer opens Send Money.
2. If no PIN exists, the setup form is shown.
3. Customer confirms the account password.
4. Customer enters and confirms a six-digit PIN.
5. Backend verifies the account password and stores the PIN hash.

### 8.3 Money Transfer

1. Customer enters recipient and amount.
2. System validates amount and prevents self-transfer.
3. Fraud engine evaluates risk.
4. Customer supplies required verification credentials.
5. Backend validates credentials before any balance change.
6. The transaction is saved with reference code, status, risk score, and audit data.
7. OTP is generated when required.
8. Customer receives the final outcome or verification challenge.

### 8.4 Transaction History

The customer transaction ledger provides:

- Reference code.
- Credit or Debit type.
- Direction symbol.
- Counterparty.
- Amount.
- Status.
- Risk level.
- Date and time.
- Transaction inspection action.

For a debit, the ledger shows `To [counterparty]`. For a credit, it shows `From [counterparty]`.

## 9. Functional Requirements

### Authentication and Access

| ID | Requirement | Priority |
|---|---|---|
| FR-AUTH-01 | The system shall authenticate customers using username or email and password. | Must |
| FR-AUTH-02 | The system shall issue JWT access tokens after successful login. | Must |
| FR-AUTH-03 | The system shall restrict protected APIs to authenticated users. | Must |
| FR-AUTH-04 | The system shall enforce role-based access for customer, analyst, and administrator features. | Must |
| FR-AUTH-05 | The system shall record the latest successful login details. | Should |

### Wallet and Transfers

| ID | Requirement | Priority |
|---|---|---|
| FR-TXN-01 | The system shall display the customer wallet balance and account number. | Must |
| FR-TXN-02 | The system shall require a six-digit transaction PIN for transfers. | Must |
| FR-TXN-03 | The system shall validate recipient, amount, and self-transfer restrictions. | Must |
| FR-TXN-04 | The system shall evaluate fraud risk before changing balances. | Must |
| FR-TXN-05 | The system shall prevent transfers when balance is insufficient. | Must |
| FR-TXN-06 | The system shall store transaction audit data. | Must |

### Fraud and OTP

| ID | Requirement | Priority |
|---|---|---|
| FR-RISK-01 | The system shall calculate a risk score from 0 to 100. | Must |
| FR-RISK-02 | The system shall classify risk as LOW, MEDIUM, or HIGH. | Must |
| FR-RISK-03 | The system shall require OTP for step-up risk decisions. | Must |
| FR-RISK-04 | The system shall require account password verification for HIGH-risk transfers. | Must |
| FR-RISK-05 | The system shall expire OTPs after five minutes. | Must |
| FR-RISK-06 | The system shall block OTP reuse. | Must |
| FR-RISK-07 | The system shall limit OTP verification attempts to three. | Must |

### Reporting and Administration

| ID | Requirement | Priority |
|---|---|---|
| FR-OPS-01 | Analysts shall be able to view fraud alerts and transaction risk data. | Must |
| FR-OPS-02 | Analysts shall be able to approve eligible transactions. | Must |
| FR-OPS-03 | Analysts shall be able to block suspicious transactions. | Must |
| FR-OPS-04 | Administrators shall be able to manage fraud rules. | Must |
| FR-OPS-05 | Customers shall be able to view transaction history with Credit/Debit classification. | Must |
| FR-OPS-06 | Authenticated users shall be able to view profile and latest-login details. | Should |

## 10. Role Permission Matrix

| Capability | Customer | Fraud Analyst | Administrator |
|---|:---:|:---:|:---:|
| Register / login | Yes | Yes | Yes |
| View own wallet | Yes | No | No |
| Send money | Yes | No | No |
| View own transaction history | Yes | No | No |
| View profile | Yes | Yes | Yes |
| View fraud analytics | No | Yes | Yes |
| Investigate transactions | No | Yes | Yes |
| Approve / block transactions | No | Yes | Yes |
| Manage fraud rules | No | No | Yes |
| Manage user directory | No | No | Yes |

## 11. Data Model Summary

| Entity | Purpose |
|---|---|
| User | Identity, credentials, role, transaction-PIN hash, account status, and latest-login metadata. |
| Wallet | Customer balance, account number, currency, and wallet status. |
| Transaction | Sender, receiver, amount, status, reference code, risk data, device, IP, location, and timestamps. |
| OtpVerification | Hashed OTP, expiry, attempt count, usage state, and linked transaction/user. |
| FraudRule | Configurable fraud rule code, description, risk points, and enabled state. |
| FraudAlert | Risk alert generated from a transaction and its review state. |
| UserRiskProfile | Historical risk-related profile information used by the fraud engine. |

## 12. API Inventory

### Authentication

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Register a customer. |
| POST | `/api/auth/login` | Authenticate and issue JWT. |
| GET | `/api/auth/me` | Retrieve current user profile. |
| POST | `/api/auth/transaction-pin` | Set or update transaction PIN. |

### Wallet and Transactions

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/wallet` | Retrieve current wallet. |
| POST | `/api/transactions/evaluate` | Evaluate transfer risk. |
| POST | `/api/transactions/transfer` | Initiate a transfer. |
| GET | `/api/transactions/history` | Retrieve customer transaction history. |

### OTP

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/otp/send` | Send or resend a transaction OTP. |
| POST | `/api/otp/verify` | Verify a transaction OTP. |

### Analyst Operations

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/analyst/dashboard` | Retrieve analyst dashboard metrics. |
| GET | `/api/analyst/transactions` | Retrieve system transactions. |
| GET | `/api/analyst/alerts` | Retrieve fraud alerts. |
| PATCH | `/api/analyst/transactions/{id}/approve` | Approve an eligible transaction. |
| PATCH | `/api/analyst/transactions/{id}/block` | Block a transaction. |
| PATCH | `/api/analyst/alerts/{id}/review` | Mark an alert as reviewed. |

### Administration

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/admin/fraud-rules` | List fraud rules. |
| POST | `/api/admin/fraud-rules` | Create a fraud rule. |
| PUT | `/api/admin/fraud-rules/{id}` | Update a fraud rule. |
| DELETE | `/api/admin/fraud-rules/{id}` | Delete a fraud rule. |

## 13. Non-Functional Requirements

### Security

- Passwords and transaction PINs are hashed with BCrypt.
- JWT tokens protect authenticated API access.
- Role-based authorization restricts analyst and administrator APIs.
- OTP values are hashed before persistence.
- OTP values expire and cannot be reused.
- OTP attempts are limited.
- Sensitive passwords, PINs, JWTs, OTPs, and biometric data must not be logged.
- Account login password is required only for HIGH-risk transfers.
- Face and biometric scanning are intentionally excluded from the product.

### Reliability and Integrity

- Risk evaluation occurs before balance modification.
- Wallet balance changes use transactional service operations and row-level wallet locking.
- Completed transactions are protected by idempotency checks in approval flows.
- Failed or blocked outcomes are retained for audit and fraud analysis.

### Usability

- Responsive web interface for desktop and mobile layouts.
- Consistent role-based navigation.
- Clear Credit/Debit indicators and direction symbols.
- User-friendly status badges for transaction and risk outcomes.
- Password and PIN visibility controls are available only where appropriate.

### Performance

- Frontend uses Vite production builds.
- Backend uses Spring Boot and connection pooling.
- Database access uses Spring Data JPA repositories.
- Transaction history supports filtering and pagination in the UI.

## 14. Technical Architecture

```text
Browser
  |
  | React / Vite / Tailwind / Axios
  v
Spring Boot REST API
  |
  | Spring Security + JWT
  | Service and fraud-rule layer
  v
MySQL 8
  |
  +-- Users
  +-- Wallets
  +-- Transactions
  +-- OTP records
  +-- Fraud rules and alerts

External dependency: SMTP provider for OTP email delivery
```

## 15. Deployment and Configuration

### Prerequisites

- Java 21 or compatible supported Java runtime.
- Maven 3.8 or later.
- Node.js 18 or later.
- MySQL 8 or later.
- SMTP account capable of sending email through the configured provider.

### Backend

```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

Default development API: `http://localhost:8080`

Required mail configuration should be provided through environment variables rather than committed source code:

```text
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-sender@example.com
SPRING_MAIL_PASSWORD=your-provider-app-password
APP_MAIL_ENABLED=true
```

For Gmail, use an App Password where applicable. SMTP configuration must be valid for OTP delivery to work.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Default development UI: `http://localhost:5173`

## 16. Testing and Acceptance Criteria

### Acceptance Criteria

- A new customer can register and log in.
- A customer without a transaction PIN cannot initiate a transfer.
- A LOW-risk transfer requires the transaction PIN and can complete without OTP.
- A MEDIUM-risk transfer requires OTP.
- A HIGH-risk transfer requires account password, transaction PIN, and OTP.
- Incorrect account password blocks the HIGH-risk transfer before OTP issuance.
- Incorrect or expired OTP does not complete a transfer.
- Insufficient wallet balance does not result in money movement.
- Transaction history correctly displays Debit and Credit direction.
- Current customer identity is represented as “You” in transaction lists.
- Analyst and administrator screens are unavailable to unauthorized roles.
- Profile page does not display or expose the transaction PIN.
- OTP email delivery failure produces a controlled error and does not silently authorize the transfer.

### Verification Evidence

The implementation has been verified with:

- Frontend production build using `npm run build`.
- Backend compilation and packaging using Maven.
- Authentication controller and authentication service tests.
- Existing fraud, OTP, transaction, and security test suites, subject to the project’s current test-environment dependencies.

## 17. Risks, Constraints, and Known Limitations

| Area | Limitation / Risk | Mitigation or next step |
|---|---|---|
| SMTP | Invalid or blocked SMTP credentials prevent OTP delivery. | Configure provider-approved credentials through environment variables and monitor mail delivery. |
| Location | Current location is not a precise geolocation service. | Integrate a trusted location service only with explicit privacy and consent review. |
| Fraud engine | Rules are configurable heuristics, not a machine-learning model. | Calibrate thresholds using production transaction data and analyst feedback. |
| Login history | Current profile records the latest successful login, not a full login-history timeline. | Add a dedicated login-audit table if historical login reporting is required. |
| Transaction PIN | PIN cannot be displayed or recovered by design. | Provide secure reset/change workflow after re-authentication. |
| Database schema | Development mode uses Hibernate update behavior. | Use versioned database migrations for production deployment. |
| Email security | SMTP provider availability affects high-risk and medium-risk transfer completion. | Add monitored production mail provider or transactional email service. |

## 18. Future Enhancements

- Dedicated login-audit history with multiple sessions and revocation.
- Device/session management and logout-from-all-devices.
- Secure transaction-PIN reset workflow.
- Push notifications in addition to email OTP.
- Production-grade fraud analytics and model-assisted scoring.
- Case management and analyst notes with audit history.
- Versioned database migrations.
- Observability dashboards, alerting, and audit-log export.
- Formal privacy, retention, and data-deletion policies.

## 19. Glossary

| Term | Definition |
|---|---|
| Account password | The customer’s primary login password. |
| Transaction PIN | A separate six-digit secret used to authorize transfers. |
| OTP | One-time password sent to the registered email address for step-up verification. |
| Risk score | Numeric fraud score from 0 to 100. |
| Risk level | LOW, MEDIUM, or HIGH classification derived from the risk score. |
| Counterparty | The other participant in a transaction: sender for a credit or receiver for a debit. |
| Fraud alert | An operational alert generated when a transaction triggers risk rules. |
| JWT | JSON Web Token used for stateless authenticated API access. |

## 20. Document Approval

| Role | Name | Status |
|---|---|---|
| Business Owner | To be assigned | Pending |
| Product Owner | To be assigned | Pending |
| Technical Lead | To be assigned | Pending |
| Security Reviewer | To be assigned | Pending |
| Business Analyst | Codex-generated baseline | Draft |
