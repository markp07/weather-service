# Security Policy

## Reporting Security Issues

If you discover a security vulnerability in this project, please report it by emailing the maintainer directly. Please do not create a public GitHub issue for security vulnerabilities.

## Security Considerations

This project integrates with an external authentication service for user management. When deploying this application:

### Required External Services

- **Authentication Service**: This application requires an external authentication service that provides JWT tokens and public key validation
  - The auth service must be accessible at the URL configured in `application.yaml` under `jwt.public-key-url`
  - For production: Configure to point to your authentication service
  - For local development: Default is `http://localhost:3000/api/auth/v1/public-key`

### Environment Variables

Always set the following environment variables:

- `POSTGRES_PASSWORD`: Secure password for PostgreSQL database (required)

### Configuration Checklist

Before deploying to production:

1. ✅ Set strong `POSTGRES_PASSWORD` in `.env` file
2. ✅ Update `jwt.public-key-url` in `application.yaml` to point to your authentication service
3. ✅ Configure `authentication.cors.allowed-origin-patterns` with your actual frontend URLs
4. ✅ Review and update logging levels (set to INFO or WARN in production)
5. ✅ Ensure PostgreSQL and Redis are properly secured
6. ✅ Use HTTPS in production for all services
7. ✅ Configure proper network isolation using Docker networks or firewall rules

### JWT Authentication

This service validates JWT tokens using public keys retrieved from the configured authentication service. Ensure:

- The authentication service's public key endpoint is accessible
- JWT tokens are transmitted securely (HTTPS in production)
- Tokens are stored in HTTP-only cookies

### Database Security

- PostgreSQL credentials should never be committed to the repository
- Use strong passwords for all database accounts
- Restrict database access to application containers only
- Regularly backup your database

## Security Best Practices

- Keep all dependencies up to date (Dependabot is configured)
- Review and merge security updates promptly
- Use environment variables for all sensitive configuration
- Never commit `.env` files or keys to the repository
- Regularly review access logs
- Monitor for unusual activity patterns

