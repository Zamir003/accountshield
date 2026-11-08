# AccountShield — Secure User Account Management API

A RESTful backend for user account management built with **Java 17** and **Spring Boot 4**.
Users can register, verify their email, log in, and manage their own profile; administrators can manage users.
The project focuses on **authentication, authorization, validation, secure password storage, and clean REST API design**.

## Features

- **Registration** with name, email, and password (email is unique and stored in lowercase)
- **Email verification** (simulated — the verification token is written to the application log)
- **JWT authentication** — short-lived access tokens (15 min by default), stateless sessions
- **Refresh tokens** with rotation and **reuse detection** (a reused token revokes the whole token family)
- **Logout** — revokes the refresh token family
- **Account lockout** — after 5 failed login attempts the account is temporarily locked for 15 minutes
- **Profile API** — authenticated users can view and update their own profile
- **Admin user management** — list users (paginated), change status, assign roles, unlock accounts
- **Role-based access control** — `USER` and `ADMIN` roles
- **BCrypt** password hashing
- **Request validation** and **centralized error handling** with a consistent JSON error format
- **Swagger UI** for interactive API documentation

## Tech Stack

| Area | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.1.2 (Web MVC, Data JPA, Validation, Security) |
| Database | PostgreSQL |
| Authentication | JWT (jjwt 0.13.0), BCrypt |
| API docs | springdoc-openapi 3.2.0 (Swagger UI) |
| Build tool | Maven (wrapper included) |

## Project Structure

```
src/main/java/com/accountshield
├── config        # Security configuration, OpenAPI config, admin seeder
├── controller    # REST controllers (auth, profile, admin)
├── dto
│   ├── request   # Request bodies (Java records with validation)
│   └── response  # Response bodies (no password data ever returned)
├── entity        # JPA entities: User, VerificationToken, RefreshToken
├── enums         # Role, UserStatus, VerificationResult
├── exception     # Custom exceptions + GlobalExceptionHandler
├── mapper        # Entity <-> DTO mapping
├── repository    # Spring Data JPA repositories
├── security      # JWT service & filter, UserDetails, token utilities, 401/403 handlers
└── service       # Business logic
```

## Getting Started

### Prerequisites

- JDK 17+
- PostgreSQL (running locally or remotely)

### 1. Create the database

```sql
CREATE DATABASE accountshield;
```

Tables are created automatically on startup (`spring.jpa.hibernate.ddl-auto=update`).

### 2. Configure environment variables

No secrets are stored in the code. All sensitive values are provided through environment variables.

| Variable | Required | Default | Description |
|---|---|---|---|
| `DB_URL` | no | `jdbc:postgresql://localhost:5432/accountshield` | JDBC URL |
| `DB_USERNAME` | no | `postgres` | Database user |
| `DB_PASSWORD` | **yes** | — | Database password |
| `JWT_SECRET` | **yes** | — | Base64-encoded secret, at least 32 bytes after decoding |
| `JWT_EXPIRATION_SECONDS` | no | `900` | Access token lifetime (seconds) |
| `REFRESH_TOKEN_DAYS` | no | `7` | Refresh token lifetime (days, absolute) |
| `ADMIN_EMAIL` | no | — | Email of the initial admin account |
| `ADMIN_PASSWORD` | no | — | Password of the initial admin account (8–72 characters) |
| `ADMIN_NAME` | no | `Administrator` | Name of the initial admin account |
| `LOGIN_MAX_ATTEMPTS` | no | `5` | Failed logins before lockout |
| `LOGIN_LOCK_MINUTES` | no | `15` | Lockout duration (minutes) |
| `VERIFICATION_TOKEN_HOURS` | no | `24` | Email verification token lifetime (hours) |

Generate a `JWT_SECRET` (32 random bytes, Base64):

```powershell
# PowerShell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

```bash
# Linux / macOS / Git Bash
openssl rand -base64 32
```

> **Initial admin:** if `ADMIN_EMAIL` and `ADMIN_PASSWORD` are set and no admin exists in the database,
> an `ADMIN` account is created on startup. Otherwise the step is skipped with a warning.
> Self-registration always creates a `USER`.

### 3. Run the application

```bash
./mvnw spring-boot:run        # Linux / macOS / Git Bash
mvnw.cmd spring-boot:run      # Windows (cmd / PowerShell)
```

The API runs at `http://localhost:8080`.

### 4. Open Swagger UI

[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

Use the **Authorize** button to paste an access token for protected endpoints.

## API Endpoints

### Authentication — public

| Method | Endpoint | Description | Success |
|---|---|---|---|
| POST | `/api/auth/register` | Register a new user (status `PENDING_VERIFICATION`) | 201 |
| POST | `/api/auth/verify` | Verify email with a token | 200 |
| POST | `/api/auth/resend-verification` | Request a new verification token | 200 |
| POST | `/api/auth/login` | Log in, receive access + refresh tokens | 200 |
| POST | `/api/auth/refresh` | Exchange a refresh token for a new token pair | 200 |
| POST | `/api/auth/logout` | Revoke a refresh token | 204 |

### Profile — `USER`, `ADMIN`

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/users/me` | Get own profile |
| PUT | `/api/users/me` | Update own name |

### Admin — `ADMIN` only

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/admin/users?page=0&size=20&sort=createdAt,desc` | List users (max page size 100; sort by `id`, `name`, `email`, `createdAt`) |
| GET | `/api/admin/users/{id}` | Get a user |
| PATCH | `/api/admin/users/{id}/status` | Change status (`ACTIVE` / `BLOCKED`) |
| PATCH | `/api/admin/users/{id}/role` | Change role (`USER` / `ADMIN`) |
| POST | `/api/admin/users/{id}/unlock` | Remove a temporary login lockout |

## Example Flow

**1. Register**

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com","password":"StrongPass123"}'
```

**2. Verify email** — the token appears in the application log:

```
[SIMULATED EMAIL] To: john@example.com | Verification token: <token> ...
```

```bash
curl -X POST http://localhost:8080/api/auth/verify \
  -H "Content-Type: application/json" \
  -d '{"token":"<token>"}'
```

**3. Log in**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"StrongPass123"}'
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "refreshToken": "Vq3x...",
  "refreshExpiresInSeconds": 604800
}
```

**4. Call a protected endpoint**

```bash
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <accessToken>"
```

**5. Refresh the tokens** when the access token expires:

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'
```

## Error Format

All errors share the same JSON structure:

```json
{
  "timestamp": "2026-11-08T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/register",
  "validationErrors": {
    "email": "Email format is invalid",
    "password": "Password must be between 8 and 72 characters"
  }
}
```

| Status | When |
|---|---|
| 400 | Validation error, malformed JSON, invalid parameter, invalid/expired verification token |
| 401 | Missing/invalid access token, wrong credentials, invalid refresh token |
| 403 | Insufficient role, email not verified, forbidden operation (e.g. admin changing own role) |
| 404 | Resource or endpoint not found |
| 405 | HTTP method not supported |
| 409 | Email already registered |
| 423 | Account temporarily locked after too many failed logins |
| 500 | Unexpected error (details are logged, never sent to the client) |

## Security Design

- **Passwords** are hashed with BCrypt (strength 10) and never returned in any response.
- **Generic login error** — `Invalid email or password` for wrong password, unknown email, or blocked account, to prevent user enumeration.
- **Email-not-verified** message is shown only after the password has been verified.
- **JWT** is signed with HS256; the secret lives only in an environment variable. User role and status are checked
  against the database on every request, so blocking a user or changing their role takes effect immediately.
- **Refresh tokens** are random opaque strings (not JWTs); only their SHA-256 hash is stored. Each use rotates the token;
  reusing an already-rotated token revokes the entire token family (theft detection). Lifetime is absolute — rotation does not extend it.
- **Verification tokens** are also stored as SHA-256 hashes and expire after 24 hours.
- **Account lockout** uses atomic database updates so concurrent login attempts cannot bypass the limit.
- **Least privilege** — registration always assigns `USER`; users cannot change their own role or status;
  admins cannot change their own role or status.
- **Stateless** — no server-side sessions, CSRF disabled (no cookies are used for authentication).

> **Note:** after logout, an already-issued access token remains valid until it expires (max 15 minutes by default).
> This is an inherent trade-off of stateless JWT authentication.
