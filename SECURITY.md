# BuckPal Security Configuration

## Required Environment Variables

### Critical Security Variables (REQUIRED)

```bash
# JWT Secret - Generate a strong 512-bit secret
JWT_SECRET=your-very-secure-512-bit-secret-key-here

# Database credentials
DB_USERNAME=your_db_username
DB_PASSWORD=your_secure_db_password

# Plaid API credentials (if using bank integration)
PLAID_CLIENT_ID=your_plaid_client_id
PLAID_SECRET=your_plaid_secret
PLAID_ENVIRONMENT=sandbox  # sandbox, development, or production
```

### Optional Security Variables

```bash
# CORS allowed origins (comma-separated, no spaces)
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173,https://yourdomain.com

# Logging level
LOG_LEVEL=INFO  # DEBUG, INFO, WARN, ERROR

# Rate limiting
RATE_LIMIT_ENABLED=true
RATE_LIMIT_RPM=60
```

## Security Configuration Steps

### 1. Generate JWT Secret

**CRITICAL:** Never use the default secret in production!

```bash
# Generate a secure 512-bit secret
openssl rand -base64 64

# Alternative using Node.js
node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"
```

### 2. Database Security

- Use strong, unique passwords
- Create dedicated database user with minimum required privileges
- Enable SSL/TLS for database connections in production

### 3. CORS Configuration

- **Development:** Use specific localhost ports
- **Production:** Use exact domain names, never wildcards
- **Never use:** `*` or wildcard patterns in production

### 4. File Upload Security

The application now includes:
- File size limits (10MB max)
- Content type validation
- CSV injection protection
- Path traversal protection

### 5. Authentication Security

- JWT tokens should be stored in httpOnly cookies (not localStorage)
- Implement token refresh mechanism
- Set appropriate token expiration (currently 24 hours)

## Security Headers (Recommended)

Add these headers in your reverse proxy (nginx/Apache) or load balancer:

```
# Security headers
add_header X-Content-Type-Options nosniff;
add_header X-Frame-Options DENY;
add_header X-XSS-Protection "1; mode=block";
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains";
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';";
```

## Rate Limiting

The application supports rate limiting configuration:
- Default: 60 requests per minute per IP
- Can be configured via `RATE_LIMIT_RPM` environment variable
- Can be disabled with `RATE_LIMIT_ENABLED=false`

## Security Audit Results

### Fixed Issues ✅
1. **JWT Secret Exposure** - Removed default secret from config
2. **CORS Wildcard** - Replaced with configurable specific origins  
3. **Logging Levels** - Made environment-configurable
4. **File Upload Security** - Added comprehensive validation
5. **Exception Handling** - Centralized with secure error messages
6. **Code Duplication** - Reduced authentication and error handling duplication

### Remaining Considerations 🔄
1. **JWT Storage** - Consider moving to httpOnly cookies (requires frontend changes)
2. **CSRF Protection** - Currently disabled, should be enabled for production
3. **Rate Limiting** - Framework in place, implementation needed
4. **Security Headers** - Should be added at proxy/server level

## Production Deployment Checklist

- [ ] Generate unique JWT secret
- [ ] Configure specific CORS origins
- [ ] Set LOG_LEVEL=WARN or ERROR
- [ ] Enable SSL/TLS for all connections  
- [ ] Configure security headers
- [ ] Set up monitoring and alerting
- [ ] Review and audit all environment variables
- [ ] Test authentication and authorization flows
- [ ] Verify file upload restrictions
- [ ] Enable rate limiting if needed

## Security Monitoring

Consider implementing:
- Failed login attempt tracking
- Unusual activity detection
- File upload monitoring
- API rate limit monitoring
- Security event logging