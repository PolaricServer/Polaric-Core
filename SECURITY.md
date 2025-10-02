# Security Policy

## Supported Versions

This project is currently in active development. Security updates are provided for the latest version only.

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

Instead, please report them via one of the following methods:

1. **GitHub Security Advisories** (Preferred)
   - Go to the [Security tab](https://github.com/PolaricServer/Arctic-Core/security/advisories)
   - Click "Report a vulnerability"
   - Fill in the details

2. **Email**
   - Send an email to: ohanssen@acm.org
   - Include "SECURITY" in the subject line
   - Provide detailed information about the vulnerability

### What to Include

Please include as much of the following information as possible:

- Type of vulnerability (e.g., authentication bypass, injection, XSS)
- Full paths of source file(s) related to the vulnerability
- Location of the affected source code (tag/branch/commit or direct URL)
- Step-by-step instructions to reproduce the issue
- Proof-of-concept or exploit code (if possible)
- Impact of the issue, including how an attacker might exploit it

### Response Timeline

- **Initial Response**: Within 48 hours
- **Assessment**: Within 7 days
- **Fix Development**: Depends on severity and complexity
- **Public Disclosure**: Coordinated with reporter

We aim to:
- Confirm receipt of your vulnerability report
- Assess the vulnerability and determine severity
- Develop and test a fix
- Release a security update
- Publicly disclose the vulnerability (with credit to reporter if desired)

### Disclosure Policy

- **Critical vulnerabilities**: Fixed and disclosed within 30 days
- **High severity**: Fixed and disclosed within 60 days
- **Medium/Low severity**: Fixed in next regular release

We follow coordinated vulnerability disclosure principles and will work with reporters to determine appropriate disclosure timelines.

## Known Security Considerations

### Authentication

Arctic-Core implements two authentication mechanisms:
1. **Username/Password** - Traditional form-based authentication
2. **HMAC-based** - Token-based authentication using SHA-256 HMAC

### Current Limitations

The following are known security considerations documented in [SECURITY_ANALYSIS.md](SECURITY_ANALYSIS.md):

1. **Legacy Password Support**: The system supports old Unix Crypt(3) passwords for backward compatibility. This algorithm is cryptographically weak. Administrators should migrate users to bcrypt (APR1) immediately.

2. **MD5 Usage**: MD5 hash functions are provided but deprecated. They should not be used for security-sensitive operations.

3. **Nonce Cache Size**: The HMAC nonce duplicate checker has a fixed size (2000 entries). In high-traffic scenarios, consider the cache exhaustion implications.

4. **Session Management**: User sessions expire after 7 days but are only checked on use, not proactively cleaned up.

### Recommended Mitigations

See [SECURITY_DEPLOYMENT.md](SECURITY_DEPLOYMENT.md) for comprehensive deployment security guidelines including:
- HTTPS configuration
- Password migration procedures
- CORS configuration
- Rate limiting
- Monitoring and logging
- Firewall configuration

## Security Updates

Security updates will be:
- Announced in GitHub releases with [SECURITY] tag
- Documented in CHANGELOG with CVE numbers (if assigned)
- Backported to supported versions when appropriate

## Security Contact

For security-related questions or concerns that are not vulnerabilities:
- Open a GitHub Discussion in the Security category
- Email: ohanssen@acm.org

## Acknowledgments

We appreciate the security research community's efforts to responsibly disclose vulnerabilities. Security researchers who report valid vulnerabilities will be:
- Credited in the security advisory (if desired)
- Listed in the project's acknowledgments
- Notified when fixes are released

## Security Best Practices for Users

### For Administrators

1. **Keep Updated**: Always run the latest version
2. **Use HTTPS**: Never expose the API over HTTP in production
3. **Strong Passwords**: Enforce strong password policies
4. **Migrate Passwords**: Replace any Crypt(3) passwords with bcrypt
5. **Monitor Logs**: Regularly review authentication logs
6. **Rate Limiting**: Implement rate limiting at proxy level
7. **Network Security**: Use firewalls to restrict access

### For Developers

1. **Input Validation**: Always validate and sanitize user input
2. **Output Encoding**: Properly encode output to prevent XSS
3. **SQL Injection**: Use parameterized queries (if using database auth)
4. **Authentication**: Use the provided authentication mechanisms
5. **Authorization**: Always check user permissions before operations
6. **Secrets Management**: Never commit secrets to version control
7. **Dependency Updates**: Keep dependencies up to date

## Compliance

Arctic-Core follows security best practices including:
- OWASP Top 10 guidelines
- CWE/SANS Top 25 mitigation strategies
- NIST cryptographic standards

## License

This security policy is licensed under [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/).

---

**Last Updated**: 2025-10-02
