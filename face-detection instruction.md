# SecurePay – Face Verification & High-Risk Transaction Security

## 1. Project Context

SecurePay is a Money Transfer and Fraud Detection System.

The application allows customers to transfer money and uses a rule-based fraud detection system to calculate a transaction risk score.

The existing fraud detection system categorizes transactions into:

- LOW
- MEDIUM
- HIGH

Risk score range:

- 0–39 → LOW
- 40–69 → MEDIUM
- 70–100 → HIGH

### New Feature

Add a **Face Verification Security Layer** for HIGH-risk transactions.

When a transaction is classified as HIGH risk, the transaction must NOT be completed immediately.

The user must complete face verification before the transaction can be approved.

The face verification feature is a **step-up authentication mechanism**.

---

# 2. IMPORTANT: Existing Features Must Not Be Broken

This is the most important requirement.

## DO NOT rewrite or unnecessarily modify existing functionality.

The existing SecurePay features must continue working exactly as they work currently.

Do NOT remove, replace, or redesign:

- Existing authentication
- Existing JWT authentication
- Existing user management
- Existing money transfer functionality
- Existing transaction functionality
- Existing fraud detection rules
- Existing risk score calculation
- Existing LOW/MEDIUM/HIGH classification
- Existing role-based access control
- Existing CUSTOMER role
- Existing FRAUD_ANALYST role
- Existing ADMIN role
- Existing dashboards
- Existing APIs
- Existing database tables
- Existing frontend pages
- Existing UI components
- Existing styling
- Existing validation
- Existing business logic

The new feature must be integrated into the existing architecture.

### Golden Rule

> EXTEND existing functionality. DO NOT REPLACE existing functionality.

Before making changes, inspect the complete existing project structure and understand how the current transaction and fraud detection flow works.

---

# 3. Existing Technology Stack

The existing application uses:

## Frontend

- React
- Vite
- Tailwind CSS
- JavaScript
- REST APIs

## Backend

- Java
- Spring Boot 3
- Spring Security
- JWT
- Spring Data JPA
- Hibernate

## Database

- MySQL 8

The implementation must use the existing technology stack.

Do NOT introduce a completely different framework unless absolutely necessary.

---

# 4. Existing Transaction Flow

The current flow is conceptually:

```text
User Login
    ↓
JWT Authentication
    ↓
Customer creates money transfer
    ↓
Transaction validation
    ↓
Fraud Detection
    ↓
Risk Score
    ↓
Risk Level
    ↓
Transaction Processing
```

The new feature must modify this flow only where necessary.

---

# 5. New Transaction Flow

The new flow should be:

```text
User Login
    ↓
Create Money Transfer
    ↓
Validate Transaction
    ↓
Fraud Detection Engine
    ↓
Calculate Risk Score
    ↓
Determine Risk Level
    ↓
 ┌───────────────┬────────────────┬─────────────────────┐
 │ LOW           │ MEDIUM         │ HIGH                │
 │ 0–39          │ 40–69          │ 70–100              │
 └───────┬───────┴────────┬───────┴──────────┬──────────┘
         ↓                ↓                  ↓
     Existing Flow    Existing Flow     Face Verification
                                             ↓
                                      Liveness Detection
                                             ↓
                                      Face Matching
                                             ↓
                                    Verification Decision
                                       ↓            ↓
                                     PASS          FAIL
                                       ↓            ↓
                                  Continue       Reject/Block
                                  Transfer       Transaction
```

---

# 6. HIGH Risk Transaction Behavior

When:

```text
riskScore >= HIGH_RISK_THRESHOLD
```

the transaction must enter:

```text
FACE_VERIFICATION_REQUIRED
```

instead of immediately completing.

Example:

```json
{
  "riskScore": 87,
  "riskLevel": "HIGH",
  "transactionStatus": "FACE_VERIFICATION_REQUIRED"
}
```

The frontend must recognize this status and open the face verification UI.

---

# 7. Transaction State Machine

Introduce transaction states only if the existing project does not already have equivalent states.

Recommended states:

```text
PENDING
    ↓
FRAUD_ANALYSIS
    ↓
LOW/MEDIUM
    ↓
PROCESSING
    ↓
COMPLETED
```

For HIGH risk:

```text
PENDING
    ↓
FRAUD_ANALYSIS
    ↓
HIGH_RISK
    ↓
FACE_VERIFICATION_REQUIRED
    ↓
FACE_VERIFICATION_IN_PROGRESS
    ↓
       ┌─────────────┐
       │             │
     PASSED        FAILED
       ↓             ↓
   PROCESSING     REJECTED
       ↓
   COMPLETED
```

Do not create duplicate statuses if equivalent statuses already exist.

First inspect the existing code.

---

# 8. Face Verification Architecture

Create a dedicated face verification module/service.

Recommended backend structure:

```text
controller/
    FaceVerificationController

service/
    FaceVerificationService

repository/
    FaceVerificationRepository

entity/
    FaceVerification

dto/
    FaceVerificationRequest
    FaceVerificationResponse
```

If the existing project follows a different package structure, follow the existing project convention instead of restructuring the entire application.

---

# 9. Face Verification Service Responsibilities

The FaceVerificationService should be responsible for:

1. Starting face verification.
2. Receiving face verification data.
3. Validating the transaction.
4. Validating the authenticated user.
5. Performing liveness verification if supported.
6. Performing face matching.
7. Recording verification results.
8. Updating transaction status.
9. Allowing the transaction to continue only after successful verification.
10. Rejecting the transaction after failed verification attempts.

Do not put all logic inside the controller.

Controllers should remain thin.

---

# 10. Face Verification Database Design

Create a separate table for face verification records if the existing database does not already contain one.

Recommended table:

```sql
face_verifications
```

Suggested fields:

```text
id
transaction_id
user_id
verification_status
verification_type
liveness_status
confidence_score
attempt_count
failure_reason
created_at
verified_at
```

Possible verification statuses:

```text
PENDING
IN_PROGRESS
PASSED
FAILED
BLOCKED
```

Possible liveness statuses:

```text
NOT_CHECKED
PASSED
FAILED
```

Use foreign keys where appropriate.

Do not store unnecessary biometric data.

---

# 11. Important Biometric Data Rule

Do NOT blindly store raw face images in MySQL.

Do not store:

- Raw camera images
- Videos
- Face recordings

unless there is a clearly justified and secure requirement.

Prefer using:

```text
Temporary image/frame
        ↓
Face verification service
        ↓
Verification result
        ↓
Discard temporary biometric data
```

If the selected face verification technology requires biometric templates, follow its secure storage requirements.

Never log biometric data.

---

# 12. Face Verification API

Create REST APIs according to the existing API conventions.

Recommended API:

## Start Verification

```http
POST /api/face-verification/start
```

Request:

```json
{
  "transactionId": 1025
}
```

Response:

```json
{
  "success": true,
  "transactionId": 1025,
  "status": "FACE_VERIFICATION_REQUIRED"
}
```

---

## Verify Face

```http
POST /api/face-verification/verify
```

Request should contain the minimum information required by the selected face verification implementation.

Example conceptual request:

```json
{
  "transactionId": 1025,
  "faceData": "..."
}
```

Response:

### Successful verification

```json
{
  "success": true,
  "verificationStatus": "PASSED",
  "transactionStatus": "PROCESSING",
  "message": "Face verification successful"
}
```

### Failed verification

```json
{
  "success": false,
  "verificationStatus": "FAILED",
  "transactionStatus": "REJECTED",
  "message": "Face verification failed"
}
```

The exact request/response structure must be adapted to the existing project's DTO and API conventions.

---

# 13. Security Rules

The following security rules are mandatory.

## Rule 1 – User Ownership

A customer must only be able to verify their own transaction.

Example:

```text
Authenticated User ID
        ↓
Transaction User ID
        ↓
Must match
```

Never trust a user ID supplied by the frontend.

Get the authenticated user from Spring Security/JWT.

---

## Rule 2 – Transaction Validation

Before face verification:

```text
Transaction exists?
        ↓
Transaction belongs to authenticated user?
        ↓
Transaction is HIGH risk?
        ↓
Transaction status = FACE_VERIFICATION_REQUIRED?
        ↓
Allow verification
```

Otherwise reject the request.

---

## Rule 3 – Prevent Direct Approval

A user must not be able to call:

```text
/api/transaction/approve
```

or any equivalent endpoint and bypass face verification.

The backend must enforce the security rule.

Frontend restrictions alone are NOT sufficient.

---

# 14. Face Verification Bypass Protection

The following must NOT be possible:

```text
HIGH RISK
   ↓
User modifies frontend
   ↓
Calls transaction completion API
   ↓
Transaction completed
```

Backend must check:

```java
if (transaction.isHighRisk()
        && !transaction.isFaceVerified()) {

    throw new SecurityException(
        "Face verification required"
    );
}
```

Adapt this logic to the existing implementation rather than blindly copying it.

---

# 15. Face Matching

The face verification implementation should support:

```text
Face Detection
+
Face Matching
+
Liveness Detection
```

Conceptual flow:

```text
Camera
  ↓
Capture Face
  ↓
Detect Face
  ↓
Check Liveness
  ↓
Generate/Extract Face Representation
  ↓
Compare Against Registered User
  ↓
Calculate Match Confidence
  ↓
Verification Decision
```

Use a reliable and appropriate face verification technology.

Do not implement insecure image comparison such as:

```text
image1.equals(image2)
```

or simple pixel comparison.

---

# 16. Face Verification Threshold

The system should use a configurable confidence threshold.

Example:

```properties
securepay.face-verification.match-threshold=0.80
```

Do NOT hard-code the threshold throughout the application.

The exact threshold depends on the selected face verification technology.

---

# 17. Attempt Limiting

Prevent unlimited verification attempts.

Example:

```text
Maximum attempts = 3
```

Flow:

```text
Attempt 1 → Failed
Attempt 2 → Failed
Attempt 3 → Failed
        ↓
BLOCKED
```

After the maximum number of failures:

```text
Transaction → REJECTED/BLOCKED
```

The exact behavior should follow the existing security model.

---

# 18. Frontend Face Verification UI

Add a dedicated face verification screen/modal.

Suggested flow:

```text
┌─────────────────────────────────────┐
│       SecurePay Verification        │
│                                     │
│  ⚠ High-risk transaction detected  │
│                                     │
│  For your security, verify your     │
│  identity using face verification.  │
│                                     │
│       ┌─────────────────┐           │
│       │                 │           │
│       │   Camera View   │           │
│       │                 │           │
│       │      FACE       │           │
│       │                 │           │
│       └─────────────────┘           │
│                                     │
│        [ Start Verification ]       │
│                                     │
└─────────────────────────────────────┘
```

Do not redesign the entire existing application.

Follow the existing SecurePay UI design.

---

# 19. Frontend States

The face verification UI should handle:

```text
INITIAL
↓
REQUESTING_CAMERA_PERMISSION
↓
CAMERA_READY
↓
CAPTURING
↓
VERIFYING
↓
SUCCESS
```

Error states:

```text
CAMERA_PERMISSION_DENIED
FACE_NOT_DETECTED
MULTIPLE_FACES_DETECTED
LIVENESS_FAILED
FACE_NOT_MATCHED
MAX_ATTEMPTS_REACHED
SERVER_ERROR
NETWORK_ERROR
```

Display user-friendly messages.

Never expose internal exception messages to users.

---

# 20. Camera Permission

The application must explicitly request camera permission.

If permission is denied:

```text
"Camera access is required for face verification."
```

Provide an appropriate retry option.

Do not silently access the camera.

---

# 21. Multiple Face Detection

If multiple faces are detected:

```text
Verification → FAIL
```

Display:

```text
"Please make sure only your face is visible."
```

Do not proceed with verification.

---

# 22. No Face Detection

If no face is detected:

```text
Verification → FAIL
```

Display:

```text
"Please position your face inside the camera frame."
```

---

# 23. Liveness Detection

If the selected technology supports liveness detection, enable it.

Conceptual flow:

```text
Camera
 ↓
Liveness Check
 ↓
Live Person?
 ├── NO → Reject
 └── YES
       ↓
    Face Match
       ↓
    Decision
```

Do not treat a static photograph as a successful verification.

---

# 24. Successful Verification

After successful verification:

```text
Face Match = PASS
Liveness = PASS
Transaction = HIGH RISK
        ↓
Transaction can continue
```

Backend should update:

```text
faceVerificationStatus = PASSED
transactionStatus = PROCESSING
```

Then continue using the existing transaction processing logic.

Do not duplicate the existing money transfer logic inside FaceVerificationService.

---

# 25. Failed Verification

If verification fails:

```text
faceVerificationStatus = FAILED
```

The transaction must not be completed.

Depending on the existing design:

```text
Transaction → REJECTED
```

or:

```text
Transaction → FACE_VERIFICATION_REQUIRED
```

until maximum attempts are reached.

Do not transfer money when verification has failed.

---

# 26. Audit Logging

The system should maintain security audit information.

Example:

```text
Transaction ID
User ID
Risk Score
Risk Level
Verification Status
Liveness Status
Attempt Count
Timestamp
Failure Reason
```

Do NOT log:

- Face images
- Raw biometric data
- Camera frames
- JWT tokens
- Passwords
- Sensitive credentials

---

# 27. Fraud Detection Integration

Do NOT replace the existing fraud detection algorithm.

The existing algorithm should continue calculating:

```text
riskScore
riskLevel
fraudReasons
```

The new feature should only consume the result.

Example:

```java
FraudResult fraudResult =
        fraudDetectionService.analyze(transaction);
```

Then:

```text
riskLevel == HIGH
        ↓
FACE_VERIFICATION_REQUIRED
```

The face verification feature is an additional security layer.

It is NOT the fraud detection algorithm itself.

---

# 28. Role-Based Access

Existing roles must continue working.

```text
CUSTOMER
FRAUD_ANALYST
ADMIN
```

Customers:

```text
Can verify their own transactions.
```

Fraud Analysts:

```text
Can view appropriate fraud/verification information
according to existing permissions.
```

Admins:

```text
Can manage/view appropriate verification information
according to existing permissions.
```

Do not weaken existing Spring Security rules.

---

# 29. Error Handling

Use proper HTTP status codes.

Examples:

```text
400 → Invalid request
401 → Unauthenticated
403 → Unauthorized
404 → Transaction not found
409 → Invalid transaction state
422 → Face verification failed
429 → Too many verification attempts
500 → Internal server error
```

Adapt these to the existing application's error-handling strategy.

Do not expose stack traces to the frontend.

---

# 30. Frontend API Handling

The frontend must handle:

```text
200
400
401
403
404
409
422
429
500
```

Use the existing API client/service layer.

Do NOT create multiple inconsistent API request mechanisms if the project already has a centralized API service.

---

# 31. Environment Configuration

Any external face verification API credentials must be stored in environment/configuration variables.

Never write:

```text
API_KEY=actual-key
```

inside source code.

Use:

```properties
securepay.face.api-key=${FACE_API_KEY}
```

or the project's existing configuration mechanism.

Never commit secrets to Git.

---

# 32. Recommended Backend Package Structure

Adapt this to the existing project:

```text
src/main/java/com/securepay/

├── controller/
│   ├── TransactionController.java
│   └── FaceVerificationController.java
│
├── service/
│   ├── TransactionService.java
│   ├── FraudDetectionService.java
│   └── FaceVerificationService.java
│
├── repository/
│   ├── TransactionRepository.java
│   └── FaceVerificationRepository.java
│
├── entity/
│   ├── Transaction.java
│   └── FaceVerification.java
│
├── dto/
│   ├── FaceVerificationRequest.java
│   └── FaceVerificationResponse.java
│
├── security/
│
└── exception/
```

Do not reorganize existing packages merely to match this example.

---

# 33. Recommended Frontend Structure

Adapt to the existing React structure:

```text
src/

├── components/
│   └── FaceVerification/
│       ├── FaceVerificationModal.jsx
│       ├── FaceCamera.jsx
│       └── VerificationStatus.jsx
│
├── pages/
│
├── services/
│   └── faceVerificationService.js
│
├── hooks/
│   └── useFaceVerification.js
│
└── utils/
```

Follow the existing project naming conventions.

---

# 34. Testing Requirements

The implementation is not complete until it has been tested.

## Test Case 1 – LOW Risk

```text
Risk Score = 25
Risk Level = LOW
```

Expected:

```text
No face verification
Transaction follows existing flow
```

---

## Test Case 2 – MEDIUM Risk

```text
Risk Score = 55
Risk Level = MEDIUM
```

Expected:

```text
Existing MEDIUM-risk behavior remains unchanged.
```

---

## Test Case 3 – HIGH Risk

```text
Risk Score = 85
Risk Level = HIGH
```

Expected:

```text
Face verification required
Transaction must not complete before verification
```

---

## Test Case 4 – Successful Face Verification

```text
Risk = HIGH
Liveness = PASS
Face Match = PASS
```

Expected:

```text
Transaction proceeds successfully.
```

---

## Test Case 5 – Failed Face Verification

```text
Risk = HIGH
Face Match = FAIL
```

Expected:

```text
Transaction does not complete.
```

---

## Test Case 6 – Liveness Failure

```text
Risk = HIGH
Liveness = FAIL
```

Expected:

```text
Transaction rejected/not approved.
```

---

## Test Case 7 – Multiple Faces

Expected:

```text
Verification fails.
Transaction does not proceed.
```

---

## Test Case 8 – Camera Permission Denied

Expected:

```text
User receives a clear error message.
Transaction remains unapproved.
```

---

## Test Case 9 – Maximum Attempts

Example:

```text
Attempt 1 → FAIL
Attempt 2 → FAIL
Attempt 3 → FAIL
```

Expected:

```text
Verification blocked/rejected.
```

---

## Test Case 10 – Unauthorized Transaction

User A attempts to verify User B's transaction.

Expected:

```text
403 Forbidden
```

---

## Test Case 11 – Bypass Attempt

User attempts to directly call the transaction completion API without face verification.

Expected:

```text
Transaction must NOT complete.
```

---

# 35. Regression Testing

After implementing the feature, verify that existing features still work.

Test:

```text
Login
Logout
JWT authentication
Customer dashboard
Money transfer
Transaction history
Fraud detection
Risk score calculation
LOW risk transaction
MEDIUM risk transaction
HIGH risk transaction
Admin functionality
Fraud analyst functionality
Existing APIs
Existing database operations
```

The new feature must not introduce regressions.

---

# 36. AI Coding Agent Instructions

Before modifying any code:

## STEP 1

Inspect the complete repository.

Understand:

```text
Frontend architecture
Backend architecture
Database schema
Authentication
Authorization
Transaction flow
Fraud detection flow
Risk calculation
Existing APIs
Existing UI
Existing tests
```

## STEP 2

Identify the exact files responsible for:

```text
Transaction creation
Fraud detection
Risk calculation
Transaction completion
Authentication
User identity
```

## STEP 3

Explain internally how the existing flow works before changing it.

## STEP 4

Implement the new feature with minimal changes.

## STEP 5

Reuse existing services and utilities whenever possible.

## STEP 6

Do not duplicate existing business logic.

## STEP 7

Run/build/test the backend.

## STEP 8

Run/build/test the frontend.

## STEP 9

Test existing functionality.

## STEP 10

Test the new face verification functionality.

---

# 37. Critical AI Restrictions

The AI coding agent MUST follow these rules:

### DO NOT

- Rewrite the project
- Change the existing architecture unnecessarily
- Replace the fraud detection system
- Remove existing APIs
- Remove existing database tables
- Change existing authentication
- Remove JWT
- Remove RBAC
- Change existing UI unnecessarily
- Replace existing transaction logic
- Create duplicate transaction processing logic
- Hard-code secrets
- Store raw biometric information unnecessarily
- Disable security checks
- Bypass backend authorization
- Modify unrelated files without reason

### DO

- Inspect first
- Reuse existing code
- Follow existing coding conventions
- Make minimal changes
- Create isolated face verification components
- Add proper validation
- Add backend authorization
- Add transaction-state validation
- Add audit information
- Add tests
- Preserve backward compatibility

---

# 38. Definition of Done

The feature is considered complete only when:

- [ ] Existing application still runs.
- [ ] Existing login still works.
- [ ] Existing JWT authentication still works.
- [ ] Existing RBAC still works.
- [ ] Existing fraud detection still works.
- [ ] LOW-risk transactions work normally.
- [ ] MEDIUM-risk transactions retain existing behavior.
- [ ] HIGH-risk transactions require face verification.
- [ ] Face verification API is implemented.
- [ ] Face verification UI is implemented.
- [ ] Camera permission is handled.
- [ ] Liveness detection is implemented if supported.
- [ ] Face matching is implemented.
- [ ] Failed verification prevents transaction completion.
- [ ] Successful verification allows transaction continuation.
- [ ] Users cannot verify another user's transaction.
- [ ] Users cannot bypass face verification.
- [ ] Verification attempts are limited.
- [ ] Verification results are audited.
- [ ] Sensitive biometric information is protected.
- [ ] No secrets are committed.
- [ ] Backend tests pass.
- [ ] Frontend tests/build pass.
- [ ] Regression testing passes.

---

# 39. Final Architecture

The target architecture should conceptually become:

```text
                    ┌──────────────────┐
                    │      React       │
                    │     Frontend     │
                    └────────┬─────────┘
                             │
                             │ REST API
                             ↓
                    ┌──────────────────┐
                    │  Spring Boot API │
                    └────────┬─────────┘
                             │
              ┌──────────────┼──────────────┐
              ↓              ↓              ↓
        Transaction      Fraud Engine    Security
          Service             │              │
              │               ↓              │
              │          Risk Score          │
              │               │              │
              │        ┌──────┴──────┐       │
              │        ↓             ↓       │
              │      LOW/MED       HIGH      │
              │        │             │        │
              │        ↓             ↓        │
              │    Existing      Face        │
              │      Flow       Verification │
              │                      │        │
              │                 ┌────┴────┐   │
              │                 ↓         ↓   │
              │              Liveness   Match │
              │                 │         │   │
              │                 └────┬────┘   │
              │                      ↓        │
              │                Verification   │
              │                  Decision     │
              │                      │        │
              └──────────────────────┘        │
                                             │
                                             ↓
                                          MySQL
```

---

# 40. Final Instruction to the AI Developer

Read this entire file before making any changes.

Treat the existing SecurePay application as a working production-style application.

Your primary objective is:

> Add HIGH-RISK transaction face verification without breaking or unnecessarily modifying any existing SecurePay functionality.

Follow this priority order:

```text
1. Preserve existing functionality
2. Preserve existing security
3. Understand existing architecture
4. Integrate with existing fraud detection
5. Add face verification
6. Prevent transaction bypass
7. Protect biometric information
8. Add tests
9. Perform regression testing
10. Keep changes minimal and maintainable
```

If an implementation decision conflicts with existing project behavior, inspect the existing code first and choose the solution that preserves backward compatibility.

Do not make large architectural changes unless they are absolutely necessary.

Before finishing, provide a summary containing:

```text
1. Files created
2. Files modified
3. Database changes
4. API changes
5. Frontend changes
6. Security changes
7. Tests added
8. Existing features verified
9. Any assumptions made
10. Any configuration/environment variables required
```