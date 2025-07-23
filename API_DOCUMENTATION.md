# Invoice Financing Platform - API Documentation

## Base URL
```
http://localhost:8080/api
```

## Authentication
All protected endpoints require JWT token in Authorization header:
```
Authorization: Bearer <jwt_token>
```

---

## 1. Authentication APIs

### POST /auth/register
Register a new user (MSME, INVESTOR, or ADMIN)

**Request Body:**
```json
{
  "name": "string (required, max 100)",
  "email": "string (required, valid email, max 100)",
  "password": "string (required, min 6, max 255)",
  "userType": "MSME | INVESTOR | ADMIN (required)"
}
```

**Response (200):**
```json
{
  "message": "User registered successfully!"
}
```

**Response (400):**
```json
{
  "message": "Error: Email is already taken!"
}
```

### POST /auth/login
Authenticate user and get JWT token

**Request Body:**
```json
{
  "email": "string (required, valid email)",
  "password": "string (required)"
}
```

**Response (200):**
```json
{
  "token": "string (JWT token)",
  "type": "Bearer",
  "id": "number (user ID)",
  "email": "string",
  "name": "string",
  "userType": "ROLE_MSME | ROLE_INVESTOR | ROLE_ADMIN"
}
```

### GET /auth/profile
Get current user profile (requires authentication)

**Response (200):**
```json
{
  "id": "number",
  "name": "string",
  "email": "string",
  "userType": "MSME | INVESTOR | ADMIN",
  "kycStatus": "PENDING | APPROVED | REJECTED",
  "isVerified": "boolean"
}
```

---

## 2. Dashboard & Analytics APIs

### GET /dashboard/platform-summary
Get comprehensive platform statistics (public endpoint)

**Response (200):**
```json
{
  "totalUsers": "number",
  "totalMSMEs": "number", 
  "totalInvestors": "number",
  "verifiedUsers": "number",
  "verificationRate": "number (percentage)",
  "totalInvoices": "number",
  "approvedInvoices": "number",
  "fundedInvoices": "number", 
  "completedInvoices": "number",
  "totalInvoiceValue": "decimal",
  "totalFundingProgress": "decimal",
  "fundingRate": "number (percentage)",
  "totalInvestments": "number",
  "activeInvestments": "number",
  "totalInvestedAmount": "decimal",
  "totalExpectedReturns": "decimal",
  "averageInvestmentSize": "decimal",
  "totalTransactions": "number",
  "totalTransactionVolume": "decimal",
  "completedTransactions": "number",
  "platformHealth": "EXCELLENT | GOOD | FAIR | NEEDS_ATTENTION",
  "lastUpdated": "datetime"
}
```

### GET /dashboard/user-stats
Get detailed statistics for current user (requires authentication)

**Response (200) - For MSME:**
```json
{
  "userId": "number",
  "name": "string",
  "email": "string", 
  "userType": "MSME",
  "kycStatus": "PENDING | APPROVED | REJECTED",
  "isVerified": "boolean",
  "memberSince": "datetime",
  "totalInvoices": "number",
  "totalInvoiceValue": "decimal",
  "totalFunded": "decimal",
  "pendingInvoices": "number",
  "approvedInvoices": "number",
  "fundedInvoices": "number",
  "completedInvoices": "number",
  "totalTransactions": "number",
  "recentTransactions": "array"
}
```

**Response (200) - For INVESTOR:**
```json
{
  "userId": "number",
  "name": "string",
  "email": "string",
  "userType": "INVESTOR", 
  "kycStatus": "PENDING | APPROVED | REJECTED",
  "isVerified": "boolean",
  "memberSince": "datetime",
  "totalInvestments": "number",
  "totalInvested": "decimal",
  "totalExpectedReturns": "decimal",
  "totalActualReturns": "decimal",
  "activeInvestments": "number",
  "completedInvestments": "number",
  "overdueInvestments": "number",
  "actualROI": "decimal (percentage)",
  "totalTransactions": "number",
  "recentTransactions": "array"
}
```

### GET /dashboard/recent-activity
Get recent platform activity (public endpoint)

**Response (200):**
```json
{
  "recentUsers": [
    {
      "id": "number",
      "name": "string",
      "userType": "MSME | INVESTOR",
      "kycStatus": "PENDING | APPROVED | REJECTED",
      "createdAt": "datetime"
    }
  ],
  "recentInvoices": "array of recent invoices",
  "recentInvestments": "array of recent investments", 
  "recentTransactions": "array of recent transactions",
  "lastUpdated": "datetime"
}
```

### GET /dashboard/monthly-stats
Get current month statistics (public endpoint)

**Response (200):**
```json
{
  "monthlyRegistrations": "number",
  "monthlyInvoices": "number",
  "monthlyInvestments": "number",
  "monthlyInvestmentVolume": "decimal",
  "month": "string",
  "year": "number"
}
```

---

## 3. KYC APIs (Requires Authentication)

### POST /kyc/upload-pan
Upload PAN document

**Request:** Multipart form data
- `file`: File (required)
- `documentNumber`: string (required)

**Response (200):**
```json
{
  "message": "PAN document uploaded successfully!",
  "documentId": "number"
}
```

### POST /kyc/upload-aadhaar
Upload Aadhaar document

**Request:** Multipart form data
- `file`: File (required)
- `documentNumber`: string (required)

**Response (200):**
```json
{
  "message": "AADHAAR document uploaded successfully!",
  "documentId": "number"
}
```

### POST /kyc/upload-gstin
Upload GSTIN document

**Request:** Multipart form data
- `file`: File (required)
- `documentNumber`: string (required)

**Response (200):**
```json
{
  "message": "GSTIN document uploaded successfully!",
  "documentId": "number"
}
```

### GET /kyc/status
Get KYC status and documents

**Response (200):**
```json
{
  "kycStatus": "PENDING | APPROVED | REJECTED",
  "isVerified": "boolean",
  "documents": [
    {
      "id": "number",
      "documentType": "PAN | AADHAAR | GSTIN",
      "documentUrl": "string",
      "documentNumber": "string",
      "status": "PENDING | APPROVED | REJECTED",
      "rejectionReason": "string (nullable)",
      "uploadedAt": "datetime",
      "verifiedAt": "datetime (nullable)"
    }
  ]
}
```

### GET /kyc/documents
Get all KYC documents for current user

**Response (200):**
```json
[
  {
    "id": "number",
    "documentType": "PAN | AADHAAR | GSTIN",
    "documentUrl": "string",
    "documentNumber": "string",
    "status": "PENDING | APPROVED | REJECTED",
    "rejectionReason": "string (nullable)",
    "uploadedAt": "datetime",
    "verifiedAt": "datetime (nullable)"
  }
]
```

---

## 4. MSME APIs (Requires ROLE_MSME)

### GET /msme/dashboard
Get MSME dashboard data

**Response (200):**
```json
{
  "totalInvoices": "number",
  "pendingInvoices": "number",
  "approvedInvoices": "number",
  "fundedInvoices": "number",
  "completedInvoices": "number",
  "rejectedInvoices": "number",
  "totalInvoiceValue": "decimal",
  "totalFunded": "decimal",
  "pendingAmount": "decimal",
  "fundingRate": "number (percentage)",
  "approvalRate": "number (percentage)",
  "kycStatus": "PENDING | APPROVED | REJECTED",
  "isVerified": "boolean",
  "memberSince": "datetime",
  "recentInvoices": [
    {
      "id": "number",
      "invoiceNumber": "string",
      "clientName": "string",
      "amount": "decimal",
      "dueDate": "date",
      "status": "PENDING | APPROVED | FUNDED | COMPLETED | REJECTED",
      "fundingProgress": "decimal",
      "createdAt": "datetime"
    }
  ]
}
```

### GET /msme/invoice/{id}/progress
Get detailed progress tracking for specific invoice

**Response (200):**
```json
{
  "invoiceId": "number",
  "invoiceNumber": "string",
  "totalAmount": "decimal",
  "totalInvested": "decimal",
  "remainingAmount": "decimal",
  "progressPercentage": "number",
  "numberOfInvestors": "number",
  "status": "PENDING | APPROVED | FUNDED | COMPLETED | REJECTED",
  "daysSinceCreation": "number",
  "daysUntilDue": "number",
  "interestRate": "decimal",
  "durationDays": "number",
  "investments": [
    {
      "investmentId": "number",
      "amount": "decimal",
      "investorName": "string",
      "investmentDate": "date",
      "expectedReturn": "decimal",
      "status": "ACTIVE | COMPLETED | OVERDUE"
    }
  ],
  "lastUpdated": "datetime"
}
```

### GET /msme/analytics
Get comprehensive MSME analytics

**Response (200):**
```json
{
  "monthlyBreakdown": {
    "invoiceValue": "object (month_year: amount)",
    "funding": "object (month_year: amount)",
    "count": "object (month_year: count)"
  },
  "statusDistribution": "object (status: count)",
  "averageInvoiceValue": "decimal",
  "averageFundingTime": "number (days)",
  "totalInvoices": "number",
  "successRate": "number (percentage)"
}
```

### POST /msme/invoice
Create new invoice

**Request Body:**
```json
{
  "invoiceNumber": "string (required)",
  "clientName": "string (required)",
  "amount": "decimal (required, > 0)",
  "dueDate": "date (required, format: YYYY-MM-DD)",
  "description": "string (optional)",
  "interestRate": "decimal (required, > 0)",
  "durationDays": "number (required)"
}
```

**Response (200):**
```json
{
  "message": "Invoice created successfully!",
  "invoiceId": "number"
}
```

### GET /msme/invoices
Get all invoices for current MSME

**Response (200):**
```json
[
  {
    "id": "number",
    "invoiceNumber": "string",
    "clientName": "string",
    "amount": "decimal",
    "dueDate": "date",
    "description": "string",
    "status": "PENDING | APPROVED | FUNDED | COMPLETED | REJECTED",
    "fundingProgress": "decimal",
    "interestRate": "decimal",
    "durationDays": "number",
    "documentUrl": "string (nullable)",
    "approvedAt": "datetime (nullable)",
    "createdAt": "datetime",
    "updatedAt": "datetime"
  }
]
```

### GET /msme/invoice/{id}
Get specific invoice by ID

**Response (200):**
```json
{
  "id": "number",
  "invoiceNumber": "string",
  "clientName": "string",
  "amount": "decimal",
  "dueDate": "date",
  "description": "string",
  "status": "PENDING | APPROVED | FUNDED | COMPLETED | REJECTED",
  "fundingProgress": "decimal",
  "interestRate": "decimal",
  "durationDays": "number",
  "documentUrl": "string (nullable)",
  "approvedAt": "datetime (nullable)",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

### PUT /msme/invoice/{id}
Update invoice (only if status is PENDING)

**Request Body:**
```json
{
  "invoiceNumber": "string (required)",
  "clientName": "string (required)",
  "amount": "decimal (required, > 0)",
  "dueDate": "date (required)",
  "description": "string (optional)",
  "interestRate": "decimal (required, > 0)",
  "durationDays": "number (required)"
}
```

**Response (200):**
```json
{
  "message": "Invoice updated successfully!"
}
```

### DELETE /msme/invoice/{id}
Delete invoice (only if status is PENDING)

**Response (200):**
```json
{
  "message": "Invoice deleted successfully!"
}
```

### POST /msme/invoice/{id}/upload-document
Upload invoice document

**Request:** Multipart form data
- `file`: File (required)

**Response (200):**
```json
{
  "message": "Document uploaded successfully!",
  "documentUrl": "string"
}
```

---

## 5. Investor APIs (Requires ROLE_INVESTOR)

### GET /investor/dashboard
Get investor dashboard data

**Response (200):**
```json
{
  "totalInvestments": "number",
  "totalInvested": "decimal",
  "expectedReturns": "decimal",
  "actualReturns": "decimal",
  "averageInvestment": "decimal",
  "actualROI": "decimal (percentage)",
  "activeInvestments": "number",
  "completedInvestments": "number",
  "overdueInvestments": "number",
  "portfolioDiversification": "number",
  "successRate": "number (percentage)",
  "kycStatus": "PENDING | APPROVED | REJECTED",
  "isVerified": "boolean",
  "memberSince": "datetime",
  "recentInvestments": [
    {
      "id": "number",
      "amount": "decimal",
      "expectedReturn": "decimal",
      "status": "ACTIVE | COMPLETED | OVERDUE",
      "investmentDate": "date",
      "maturityDate": "date",
      "createdAt": "datetime"
    }
  ]
}
```

### GET /investor/investment/{id}/progress
Get detailed progress tracking for specific investment

**Response (200):**
```json
{
  "investmentId": "number",
  "amount": "decimal",
  "expectedReturn": "decimal",
  "currentExpectedReturn": "decimal",
  "actualReturn": "decimal (nullable)",
  "status": "ACTIVE | COMPLETED | OVERDUE",
  "investmentDate": "date",
  "maturityDate": "date",
  "daysInvested": "number",
  "daysUntilMaturity": "number",
  "progressPercentage": "number",
  "invoice": {
    "invoiceId": "number",
    "invoiceNumber": "string",
    "clientName": "string",
    "totalAmount": "decimal",
    "dueDate": "date",
    "status": "APPROVED | FUNDED | COMPLETED",
    "fundingPercentage": "number",
    "totalFunding": "decimal",
    "msmeCompany": "string"
  },
  "lastUpdated": "datetime"
}
```

### GET /investor/portfolio-analytics
Get comprehensive portfolio analytics for investor

**Response (200):**
```json
{
  "totalInvested": "decimal",
  "totalExpectedReturns": "decimal",
  "totalActualReturns": "decimal",
  "actualROI": "number (percentage)",
  "expectedROI": "number (percentage)",
  "successRate": "number (percentage)",
  "averageInvestment": "decimal",
  "totalInvestments": "number",
  "portfolioDiversification": "number",
  "monthlyBreakdown": {
    "investments": "object (month_year: amount)",
    "returns": "object (month_year: amount)",
    "count": "object (month_year: count)"
  },
  "statusDistribution": "object (status: count)",
  "msmeDistribution": "object (company_name: invested_amount)"
}
```

### GET /investor/invoices
Get available invoices for investment

**Response (200):**
```json
[
  {
    "id": "number",
    "invoiceNumber": "string",
    "clientName": "string",
    "amount": "decimal",
    "dueDate": "date",
    "description": "string",
    "status": "APPROVED",
    "fundingProgress": "decimal",
    "interestRate": "decimal",
    "durationDays": "number",
    "createdAt": "datetime",
    "msme": {
      "id": "number",
      "name": "string",
      "email": "string"
    }
  }
]
```

### POST /investor/invest
Make investment in an invoice

**Request Body:**
```json
{
  "invoiceId": "number (required)",
  "amount": "decimal (required, > 0)"
}
```

**Response (200):**
```json
{
  "message": "Investment made successfully!",
  "investmentId": "number",
  "expectedReturn": "decimal"
}
```

**Response (400):**
```json
{
  "message": "KYC verification required before making investments"
}
```

### GET /investor/investments
Get all investments for current investor

**Response (200):**
```json
[
  {
    "id": "number",
    "amount": "decimal",
    "expectedReturn": "decimal",
    "status": "ACTIVE | COMPLETED | OVERDUE",
    "investmentDate": "date",
    "maturityDate": "date",
    "actualReturn": "decimal (nullable)",
    "repaidAt": "datetime (nullable)",
    "createdAt": "datetime",
    "invoice": {
      "id": "number",
      "invoiceNumber": "string",
      "clientName": "string",
      "amount": "decimal",
      "interestRate": "decimal",
      "durationDays": "number"
    }
  }
]
```

### GET /investor/investment/{id}
Get specific investment by ID

**Response (200):**
```json
{
  "id": "number",
  "amount": "decimal",
  "expectedReturn": "decimal",
  "status": "ACTIVE | COMPLETED | OVERDUE",
  "investmentDate": "date",
  "maturityDate": "date",
  "actualReturn": "decimal (nullable)",
  "repaidAt": "datetime (nullable)",
  "createdAt": "datetime",
  "invoice": {
    "id": "number",
    "invoiceNumber": "string",
    "clientName": "string",
    "amount": "decimal",
    "interestRate": "decimal",
    "durationDays": "number"
  }
}
```

### POST /investor/payment-details
Save payment details

**Request Body:**
```json
{
  "upiId": "string (optional)",
  "bankAccountNumber": "string (optional)",
  "ifscCode": "string (optional)",
  "accountHolderName": "string (optional)",
  "bankName": "string (optional)"
}
```

**Response (200):**
```json
{
  "message": "Payment details saved successfully!"
}
```

### GET /investor/payment-details
Get payment details

**Response (200):**
```json
{
  "id": "number",
  "upiId": "string (nullable)",
  "bankAccountNumber": "string (nullable)",
  "ifscCode": "string (nullable)",
  "accountHolderName": "string (nullable)",
  "bankName": "string (nullable)",
  "isVerified": "boolean",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

### PUT /investor/payment-details
Update payment details

**Request Body:**
```json
{
  "upiId": "string (optional)",
  "bankAccountNumber": "string (optional)",
  "ifscCode": "string (optional)",
  "accountHolderName": "string (optional)",
  "bankName": "string (optional)"
}
```

**Response (200):**
```json
{
  "message": "Payment details updated successfully!"
}
```

---

## 6. Admin APIs (Requires ROLE_ADMIN)

### GET /admin/dashboard
Get admin dashboard statistics

**Response (200):**
```json
{
  "totalUsers": "number",
  "totalMSMEs": "number",
  "totalInvestors": "number",
  "pendingKYC": "number",
  "approvedKYC": "number",
  "rejectedKYC": "number",
  "kycApprovalRate": "number (percentage)",
  "totalInvoices": "number",
  "pendingInvoices": "number",
  "approvedInvoices": "number",
  "fundedInvoices": "number",
  "completedInvoices": "number",
  "rejectedInvoices": "number",
  "invoiceApprovalRate": "number (percentage)",
  "totalInvoiceValue": "decimal",
  "totalInvestments": "number",
  "activeInvestments": "number",
  "totalInvestedAmount": "decimal",
  "totalExpectedReturns": "decimal",
  "platformUtilization": "number (percentage)",
  "platformHealth": "EXCELLENT | GOOD | FAIR | NEEDS_ATTENTION",
  "averageInvestmentSize": "decimal",
  "lastUpdated": "datetime"
}
```

### GET /admin/kyc-pending
Get users with pending KYC

**Response (200):**
```json
[
  {
    "id": "number",
    "name": "string",
    "email": "string",
    "userType": "MSME | INVESTOR",
    "kycStatus": "PENDING",
    "isVerified": "boolean",
    "createdAt": "datetime"
  }
]
```

### GET /admin/kyc-documents/{userId}
Get KYC documents for specific user

**Response (200):**
```json
[
  {
    "id": "number",
    "documentType": "PAN | AADHAAR | GSTIN",
    "documentUrl": "string",
    "documentNumber": "string",
    "status": "PENDING | APPROVED | REJECTED",
    "rejectionReason": "string (nullable)",
    "uploadedAt": "datetime",
    "verifiedAt": "datetime (nullable)"
  }
]
```

### PUT /admin/kyc/{userId}/approve
Approve user KYC

**Response (200):**
```json
{
  "message": "KYC approved successfully!"
}
```

### PUT /admin/kyc/{userId}/reject
Reject user KYC

**Request Body:**
```json
{
  "reason": "string (required)"
}
```

**Response (200):**
```json
{
  "message": "KYC rejected successfully!"
}
```

### GET /admin/invoices/pending
Get pending invoices for approval

**Response (200):**
```json
[
  {
    "id": "number",
    "invoiceNumber": "string",
    "clientName": "string",
    "amount": "decimal",
    "dueDate": "date",
    "description": "string",
    "status": "PENDING",
    "interestRate": "decimal",
    "durationDays": "number",
    "documentUrl": "string (nullable)",
    "createdAt": "datetime",
    "msme": {
      "id": "number",
      "name": "string",
      "email": "string"
    }
  }
]
```

### PUT /admin/invoice/{invoiceId}/approve
Approve invoice

**Response (200):**
```json
{
  "message": "Invoice approved successfully!"
}
```

### PUT /admin/invoice/{invoiceId}/reject
Reject invoice

**Response (200):**
```json
{
  "message": "Invoice rejected successfully!"
}
```

### GET /admin/users
Get all users

**Response (200):**
```json
[
  {
    "id": "number",
    "name": "string",
    "email": "string",
    "userType": "MSME | INVESTOR | ADMIN",
    "kycStatus": "PENDING | APPROVED | REJECTED",
    "isVerified": "boolean",
    "isActive": "boolean",
    "createdAt": "datetime",
    "updatedAt": "datetime"
  }
]
```

### GET /admin/transactions
Get all transactions

**Response (200):**
```json
[
  {
    "id": "number",
    "transactionId": "string",
    "amount": "decimal",
    "type": "INVESTMENT | REPAYMENT | REFUND",
    "status": "PENDING | COMPLETED | FAILED | CANCELLED",
    "gatewayTransactionId": "string (nullable)",
    "createdAt": "datetime",
    "completedAt": "datetime (nullable)",
    "user": {
      "id": "number",
      "name": "string",
      "email": "string"
    }
  }
]
```

### GET /admin/logs
Get system logs (returns recent transactions as logs)

**Response (200):**
```json
[
  {
    "id": "number",
    "transactionId": "string",
    "amount": "decimal",
    "type": "INVESTMENT | REPAYMENT | REFUND",
    "status": "PENDING | COMPLETED | FAILED | CANCELLED",
    "createdAt": "datetime",
    "user": {
      "name": "string",
      "email": "string"
    }
  }
]
```

---

## 7. Notification APIs (Requires Authentication)

### GET /notifications
Get all notifications for current user

**Response (200):**
```json
[
  {
    "id": "number",
    "title": "string",
    "message": "string",
    "type": "INFO | SUCCESS | WARNING | ERROR",
    "isRead": "boolean",
    "createdAt": "datetime",
    "readAt": "datetime (nullable)"
  }
]
```

### GET /notifications/unread
Get unread notifications for current user

**Response (200):**
```json
[
  {
    "id": "number",
    "title": "string",
    "message": "string",
    "type": "INFO | SUCCESS | WARNING | ERROR",
    "isRead": "boolean",
    "createdAt": "datetime"
  }
]
```

### GET /notifications/count
Get unread notification count

**Response (200):**
```json
{
  "unreadCount": "number"
}
```

### PUT /notifications/{id}/read
Mark notification as read

**Response (200):**
```json
{
  "message": "Notification marked as read"
}
```

### PUT /notifications/read-all
Mark all notifications as read

**Response (200):**
```json
{
  "message": "All notifications marked as read"
}
```

### DELETE /notifications/{id}
Delete notification

**Response (200):**
```json
{
  "message": "Notification deleted successfully"
}
```

---

## 8. Analytics APIs (Requires Authentication)

### GET /analytics/platform
Get comprehensive platform analytics (public endpoint)

**Response (200):**
```json
{
  "totalUsers": "number",
  "totalMSMEs": "number",
  "totalInvestors": "number",
  "verifiedUsers": "number",
  "verificationRate": "number (percentage)",
  "totalInvoiceValue": "decimal",
  "totalFundedValue": "decimal",
  "fundingRate": "number (percentage)",
  "totalInvestedAmount": "decimal",
  "totalExpectedReturns": "decimal",
  "platformHealth": "EXCELLENT | GOOD | FAIR | NEEDS_ATTENTION"
}
```

### GET /analytics/user
Get analytics for current user

**Response (200) - For MSME:**
```json
{
  "totalInvoices": "number",
  "totalValue": "decimal",
  "totalFunded": "decimal",
  "approvalRate": "number (percentage)",
  "fundingRate": "number (percentage)",
  "averageFundingTime": "number (days)"
}
```

**Response (200) - For INVESTOR:**
```json
{
  "totalInvestments": "number",
  "totalInvested": "decimal",
  "totalExpectedReturns": "decimal",
  "totalActualReturns": "decimal",
  "successRate": "number (percentage)",
  "actualROI": "number (percentage)",
  "portfolioDiversification": "number"
}
```

### GET /analytics/trends
Get monthly trends and patterns

**Response (200):**
```json
{
  "monthlyRegistrations": "object (month_year: count)",
  "monthlyInvoices": "object (month_year: count)",
  "monthlyInvestmentVolume": "object (month_year: amount)"
}
```

---

## Data Types Reference

### Enums
```typescript
// User Types
type UserType = 'MSME' | 'INVESTOR' | 'ADMIN';

// KYC Status
type KYCStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

// Document Types
type DocumentType = 'PAN' | 'AADHAAR' | 'GSTIN';

// Document Status
type DocumentStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

// Invoice Status
type InvoiceStatus = 'PENDING' | 'APPROVED' | 'FUNDED' | 'COMPLETED' | 'REJECTED';

// Investment Status
type InvestmentStatus = 'ACTIVE' | 'COMPLETED' | 'OVERDUE';

// Transaction Types
type TransactionType = 'INVESTMENT' | 'REPAYMENT' | 'REFUND';

// Transaction Status
type TransactionStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

// Notification Types
type NotificationType = 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR';

// Platform Health
type PlatformHealth = 'EXCELLENT' | 'GOOD' | 'FAIR' | 'NEEDS_ATTENTION';
```

### TypeScript Interfaces for Frontend

```typescript
interface User {
  id: number;
  name: string;
  email: string;
  userType: UserType;
  kycStatus: KYCStatus;
  isVerified: boolean;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

interface KYCDocument {
  id: number;
  documentType: DocumentType;
  documentUrl: string;
  documentNumber: string;
  status: DocumentStatus;
  rejectionReason?: string;
  uploadedAt: string;
  verifiedAt?: string;
}

interface Invoice {
  id: number;
  invoiceNumber: string;
  clientName: string;
  amount: number;
  dueDate: string;
  description?: string;
  status: InvoiceStatus;
  fundingProgress: number;
  interestRate: number;
  durationDays: number;
  documentUrl?: string;
  approvedAt?: string;
  createdAt: string;
  updatedAt: string;
  msme?: {
    id: number;
    name: string;
    email: string;
  };
}

interface Investment {
  id: number;
  amount: number;
  expectedReturn: number;
  status: InvestmentStatus;
  investmentDate: string;
  maturityDate: string;
  actualReturn?: number;
  repaidAt?: string;
  createdAt: string;
  invoice?: Invoice;
}

interface PaymentDetails {
  id: number;
  upiId?: string;
  bankAccountNumber?: string;
  ifscCode?: string;
  accountHolderName?: string;
  bankName?: string;
  isVerified: boolean;
  createdAt: string;
  updatedAt: string;
}

interface Transaction {
  id: number;
  transactionId: string;
  amount: number;
  type: TransactionType;
  status: TransactionStatus;
  gatewayTransactionId?: string;
  createdAt: string;
  completedAt?: string;
  user?: {
    id: number;
    name: string;
    email: string;
  };
}

interface Notification {
  id: number;
  title: string;
  message: string;
  type: NotificationType;
  isRead: boolean;
  createdAt: string;
  readAt?: string;
}

interface PlatformSummary {
  totalUsers: number;
  totalMSMEs: number;
  totalInvestors: number;
  verifiedUsers: number;
  verificationRate: number;
  totalInvoices: number;
  approvedInvoices: number;
  fundedInvoices: number;
  completedInvoices: number;
  totalInvoiceValue: number;
  totalFundingProgress: number;
  fundingRate: number;
  totalInvestments: number;
  activeInvestments: number;
  totalInvestedAmount: number;
  totalExpectedReturns: number;
  averageInvestmentSize: number;
  totalTransactions: number;
  totalTransactionVolume: number;
  completedTransactions: number;
  platformHealth: PlatformHealth;
  lastUpdated: string;
}

interface InvoiceProgress {
  invoiceId: number;
  invoiceNumber: string;
  totalAmount: number;
  totalInvested: number;
  remainingAmount: number;
  progressPercentage: number;
  numberOfInvestors: number;
  status: InvoiceStatus;
  daysSinceCreation: number;
  daysUntilDue: number;
  interestRate: number;
  durationDays: number;
  investments: Array<{
    investmentId: number;
    amount: number;
    investorName: string;
    investmentDate: string;
    expectedReturn: number;
    status: InvestmentStatus;
  }>;
  lastUpdated: string;
}

interface InvestmentProgress {
  investmentId: number;
  amount: number;
  expectedReturn: number;
  currentExpectedReturn: number;
  actualReturn?: number;
  status: InvestmentStatus;
  investmentDate: string;
  maturityDate: string;
  daysInvested: number;
  daysUntilMaturity: number;
  progressPercentage: number;
  invoice: {
    invoiceId: number;
    invoiceNumber: string;
    clientName: string;
    totalAmount: number;
    dueDate: string;
    status: InvoiceStatus;
    fundingPercentage: number;
    totalFunding: number;
    msmeCompany: string;
  };
  lastUpdated: string;
}

interface MSMEAnalytics {
  monthlyBreakdown: {
    invoiceValue: Record<string, number>;
    funding: Record<string, number>;
    count: Record<string, number>;
  };
  statusDistribution: Record<string, number>;
  averageInvoiceValue: number;
  averageFundingTime: number;
  totalInvoices: number;
  successRate: number;
}

interface PortfolioAnalytics {
  totalInvested: number;
  totalExpectedReturns: number;
  totalActualReturns: number;
  actualROI: number;
  expectedROI: number;
  successRate: number;
  averageInvestment: number;
  totalInvestments: number;
  portfolioDiversification: number;
  monthlyBreakdown: {
    investments: Record<string, number>;
    returns: Record<string, number>;
    count: Record<string, number>;
  };
  statusDistribution: Record<string, number>;
  msmeDistribution: Record<string, number>;
}
```

---

## Error Responses

All endpoints may return these common error responses:

**401 Unauthorized:**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/endpoint"
}
```

**403 Forbidden:**
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/endpoint"
}
```

**404 Not Found:**
```json
{
  "message": "Resource not found"
}
```

**400 Bad Request:**
```json
{
  "message": "Validation error message"
}
```

**500 Internal Server Error:**
```json
{
  "message": "Internal server error message"
}
```

---

## File Upload Notes

- Maximum file size: 10MB
- Supported file types: PDF, JPG, PNG, JPEG
- Files are stored in `uploads/kyc/` and `uploads/invoices/` directories
- Use `multipart/form-data` content type for file uploads
- Include `Content-Type: multipart/form-data` header
- Use FormData object in JavaScript for file uploads

## Authentication Flow

1. Register user with POST /auth/register
2. Login with POST /auth/login to get JWT token
3. Include JWT token in Authorization header for all protected endpoints
4. Token format: `Authorization: Bearer <jwt_token>`
5. Token expires after 24 hours (86400000 ms)

## CORS Configuration

The backend is configured to accept requests from:
- `http://localhost:5173` (your frontend)
- Allowed methods: GET, POST, PUT, DELETE, OPTIONS
- Allowed headers: All headers (*)
- Credentials: Enabled

## API Design Principles

1. **No Cyclic Dependencies**: All entity relationships are properly managed with lazy loading
2. **Clear Endpoint Naming**: Each endpoint has a unique, descriptive path
3. **Consistent Response Format**: All responses follow the same JSON structure
4. **Role-based Access**: Endpoints are properly secured with role-based access control
5. **Comprehensive Error Handling**: All error scenarios are handled with appropriate HTTP status codes
6. **Progress Tracking**: Dedicated endpoints for tracking invoice and investment progress
7. **Analytics Support**: Rich analytics data for dashboard creation
8. **Real-time Notifications**: Complete notification system for user engagement
9. **File Upload Security**: Secure document handling with validation
10. **Transaction Management**: Complete audit trail for all financial operations

## Complete Feature Set

✅ **Authentication & Authorization**
- Multi-role user registration and login
- JWT-based security with role-based access control
- User profile management

✅ **KYC Verification System**
- Document upload (PAN, Aadhaar, GSTIN)
- Admin approval workflow
- Status tracking and notifications

✅ **Invoice Management**
- Complete CRUD operations for invoices
- Document upload and verification
- Progress tracking with investor details
- Status management and approval workflow

✅ **Investment System**
- Browse and invest in approved invoices
- Portfolio management and tracking
- ROI calculations and performance metrics
- Investment progress monitoring

✅ **Payment Management**
- UPI and bank account details
- Payment verification system
- Transaction logging and audit trail

✅ **Dashboard & Analytics**
- Platform-wide statistics and summaries
- User-specific dashboards for all roles
- Progress tracking for investments and invoices
- Comprehensive analytics and trends

✅ **Notification System**
- Real-time notifications for all user actions
- Notification management (read/unread/delete)
- System-wide alerts and updates

✅ **Admin Panel**
- User management and KYC approval
- Invoice approval workflow
- Platform monitoring and analytics
- System logs and audit trails

✅ **File Management**
- Secure file upload with validation
- Document storage and retrieval
- File type and size restrictions

The backend provides a complete, production-ready API with 50+ endpoints covering all aspects of the invoice financing platform.