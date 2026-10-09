# SecurePay - Money Transfer & Fraud Detection System

Full-stack enterprise solution for money transfer with real-time rule-based fraud detection, risk scoring (0-100), risk levels (LOW / MEDIUM / HIGH), step-up OTP verification, and role-based access control (CUSTOMER, FRAUD_ANALYST, ADMIN).

## 🏗 Architecture Overview

```
[ React + Vite + Tailwind CSS ] ──(REST API & JWT)──> [ Spring Boot 3 + Spring Security ] ──(JPA)──> [ MySQL 8.0 ]
```

## 📁 Monorepo Structure

```
SecurePay/
├── frontend/             # React JS + Vite + Tailwind CSS
│   ├── src/
│   │   ├── components/   # Shared UI components
│   │   ├── context/      # Auth & Application State
│   │   ├── pages/        # Role-based dashboards & views
│   │   └── services/     # Axios REST API services
│   ├── package.json
│   └── vite.config.js
│
└── backend/              # Java 21 + Spring Boot 3 + Maven
    ├── src/main/java/com/securepay/
    │   ├── config/       # App & Security configurations
    │   ├── controller/   # REST API Controllers
    │   ├── dto/          # Data Transfer Objects
    │   ├── model/        # JPA Entities (User, Wallet, Transaction, FraudRule)
    │   ├── repository/   # Spring Data Repositories
    │   ├── security/     # JWT Filters & Auth Services
    │   └── service/      # Business & Fraud Engine Logic
    └── pom.xml
```

## 🚀 Getting Started

### Prerequisites
- **Java**: 17+ or 21+
- **Maven**: 3.8+
- **Node.js**: 18+ or 20+
- **MySQL Server**: 8.0+

### Backend Setup (`backend/`)
```bash
cd backend
mvn clean compile
mvn spring-boot:run
```
*Backend runs at `http://localhost:8080`*

Configure `SPRING_MAIL_USERNAME` and `SPRING_MAIL_PASSWORD` with an SMTP account before starting the backend. Medium-risk transfer codes are emailed to the sender's registered email address; if delivery is unavailable, the transfer challenge fails rather than using a development code.

Customers must set a 6-digit transaction PIN in Send Money (confirming their account password) before their first transfer. The PIN is stored as a BCrypt hash and verified by the backend for every transfer; risk-based email OTP remains an additional check when required.

### Frontend Setup (`frontend/`)
```bash
cd frontend
npm install
npm run dev
```
*Frontend runs at `http://localhost:5173`*

## 👥 System Roles
- **CUSTOMER**: Registration, Virtual Wallet, Money Transfers, Transaction History, OTP Challenge handling.
- **FRAUD_ANALYST**: High-risk transaction monitoring, fraud alerts queue, manual flag/approval actions.
- **ADMIN**: Fraud rule threshold configuration, system management, user status controls.
