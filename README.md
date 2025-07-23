# Invoice Financing Platform - Backend

A comprehensive Java Spring Boot backend for an invoice financing marketplace platform that enables MSMEs to raise working capital by listing verified invoices for investor funding.

## Features

- **Multi-stakeholder Authentication**: Support for MSME, Investor, and Admin user types
- **KYC Verification System**: Document upload and verification for PAN, Aadhaar, and GSTIN
- **Invoice Management**: Complete CRUD operations for invoice listing and tracking
- **Investment System**: Partial and full funding capabilities with ROI calculations
- **Payment Management**: UPI and bank account details handling
- **Admin Dashboard**: Comprehensive approval and monitoring system
- **Security**: JWT-based authentication with role-based access control

## Tech Stack

- **Framework**: Spring Boot 3.2.0
- **Database**: MySQL with JPA/Hibernate
- **Security**: Spring Security with JWT
- **File Upload**: Multipart file handling
- **Documentation**: Built-in API endpoints

## API Endpoints

### Authentication APIs
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `GET /api/auth/profile` - Get user profile

### KYC APIs
- `POST /api/kyc/upload-pan` - Upload PAN document
- `POST /api/kyc/upload-aadhaar` - Upload Aadhaar document
- `POST /api/kyc/upload-gstin` - Upload GSTIN document
- `GET /api/kyc/status` - Get KYC status

### MSME APIs
- `GET /api/msme/dashboard` - MSME dashboard data
- `POST /api/msme/invoice` - Create new invoice
- `GET /api/msme/invoices` - Get all invoices
- `GET /api/msme/invoice/{id}` - Get specific invoice
- `PUT /api/msme/invoice/{id}` - Update invoice
- `DELETE /api/msme/invoice/{id}` - Delete invoice

### Investor APIs
- `GET /api/investor/dashboard` - Investor dashboard
- `GET /api/investor/invoices` - Available invoices for investment
- `POST /api/investor/invest` - Make investment
- `GET /api/investor/investments` - Get all investments
- `POST /api/investor/payment-details` - Save payment details

### Admin APIs
- `GET /api/admin/dashboard` - Admin dashboard
- `GET /api/admin/kyc-pending` - Pending KYC verifications
- `PUT /api/admin/kyc/{userId}/approve` - Approve KYC
- `PUT /api/admin/invoice/{invoiceId}/approve` - Approve invoice

## Setup Instructions

1. **Database Setup**:
   - Install MySQL
   - Create database: `invoice_financing_db`
   - Update credentials in `application.properties`

2. **Configuration**:
   - Update database credentials
   - Configure JWT secret key
   - Set up mail configuration (optional)

3. **Run Application**:
   ```bash
   mvn spring-boot:run
   ```

4. **Access**:
   - Backend API: `http://localhost:8080/api`
   - Frontend should run on: `http://localhost:5173`

## Database Schema

- **Users**: MSME, Investor, Admin user management
- **KYC Documents**: Document verification system
- **Invoices**: Invoice listing and management
- **Investments**: Investment tracking and returns
- **Transactions**: Payment and transaction logs
- **Payment Details**: UPI and bank account information
- **Notifications**: System notifications

## Security Features

- JWT token-based authentication
- Role-based access control
- Password encryption with BCrypt
- CORS configuration for frontend integration
- File upload security

## File Upload

- KYC documents stored in `uploads/kyc/`
- Invoice documents stored in `uploads/invoices/`
- Maximum file size: 10MB

## CORS Configuration

Configured to allow requests from `http://localhost:5173` for frontend integration.

## Default User Types

- **MSME**: Can list invoices and manage their portfolio
- **INVESTOR**: Can browse and invest in approved invoices
- **ADMIN**: Can verify KYC and approve invoices

## Development Notes

- Database schema auto-creates on first run
- All endpoints require authentication except `/auth/**`
- File uploads create directories automatically
- Comprehensive error handling and validation