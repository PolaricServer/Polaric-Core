# Security Best Practices for Arctic-Core Deployment

This document provides security guidance for deploying and configuring Arctic-Core in production environments.

## Critical Configuration Requirements

### 1. Use HTTPS Only

**Always run Arctic-Core behind HTTPS in production environments.**

The `/directLogin` endpoint returns session keys that must be protected in transit. Configure your reverse proxy (nginx, Apache, etc.) to:

- Enforce HTTPS-only connections
- Disable TLS 1.0 and TLS 1.1 
- Use strong cipher suites
- Enable HSTS (HTTP Strict Transport Security)

Example nginx configuration:
```nginx
server {
    listen 443 ssl http2;
    server_name your-server.example.com;
    
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers 'ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384';
    ssl_prefer_server_ciphers on;
    
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name your-server.example.com;
    return 301 https://$server_name$request_uri;
}
```

### 2. Password File Security

#### Upgrading from Crypt(3) to Bcrypt

If you have users with legacy Crypt(3) password hashes (13 characters), **migrate them immediately**:

1. Use `htpasswd` with bcrypt to generate new password hashes:
   ```bash
   htpasswd -nbB username password
   ```

2. The output format is `username:$2y$...` (bcrypt) or `username:$apr1$...` (MD5-based, but stronger than Crypt3)

3. Update your password file with the new hash

4. Inform users they may need to reset their passwords

#### File Permissions

Ensure password files have restrictive permissions:
```bash
chmod 600 /etc/polaric-aprsd/passwd
chown polaric-user:polaric-group /etc/polaric-aprsd/passwd
```

### 3. Key File Management

#### Device Keys (peers)
Location: `/etc/polaric-aprsd/keys/peers`

```bash
chmod 600 /etc/polaric-aprsd/keys/peers
chown polaric-user:polaric-group /etc/polaric-aprsd/keys/peers
```

#### User Login Keys
Location: `/var/lib/polaric/logins.dat`

This file is automatically managed by the application. Ensure the directory is properly secured:
```bash
chmod 700 /var/lib/polaric
chown polaric-user:polaric-group /var/lib/polaric
```

### 4. CORS Configuration

Configure `httpserver.alloworigin` carefully. **Never use `.*` in production:**

```properties
# BAD - allows any origin
httpserver.alloworigin=.*

# GOOD - specific domains only
httpserver.alloworigin=https://myapp\.example\.com|https://dashboard\.example\.com
```

The value is a regular expression. Escape dots and use anchors if needed:
```properties
# More restrictive - exact match only
httpserver.alloworigin=^https://myapp\.example\.com$
```

### 5. Session Management

#### Session Timeout

User sessions expire after 7 days of inactivity. Consider adjusting `MAX_SESSION_LENGTH` in `HmacAuthenticator.java` for your security requirements:

- High-security environments: 1-4 hours
- Normal environments: 1-7 days  
- Low-security/development: 7+ days

#### Session Key Rotation

When a user logs in via `/directLogin`, a new session key is generated. To manually invalidate a user's session:

1. Stop the application
2. Remove the user's entry from `/var/lib/polaric/logins.dat`
3. Restart the application

Or use the admin interface if available.

## Network Security

### 1. Firewall Configuration

Only expose necessary ports:
```bash
# Allow HTTPS only
iptables -A INPUT -p tcp --dport 443 -j ACCEPT

# Block direct access to application port
iptables -A INPUT -p tcp --dport 8080 -s 127.0.0.1 -j ACCEPT
iptables -A INPUT -p tcp --dport 8080 -j DROP
```

### 2. Rate Limiting

Implement rate limiting at the reverse proxy level to prevent:
- Brute force attacks on authentication endpoints
- DoS attacks
- Nonce cache exhaustion

Example nginx rate limiting:
```nginx
http {
    limit_req_zone $binary_remote_addr zone=auth:10m rate=5r/s;
    limit_req_zone $binary_remote_addr zone=api:10m rate=20r/s;
    
    server {
        # ... ssl config ...
        
        location /directLogin {
            limit_req zone=auth burst=10 nodelay;
            proxy_pass http://localhost:8080;
        }
        
        location /api/ {
            limit_req zone=api burst=50 nodelay;
            proxy_pass http://localhost:8080;
        }
    }
}
```

## Monitoring and Logging

### 1. Authentication Logs

Monitor authentication logs for suspicious activity:
```bash
tail -f /var/log/polaric/auth.log
```

Look for:
- Multiple failed login attempts from same IP
- Login attempts with invalid usernames
- Unusual login times or patterns
- Geographic anomalies (if tracked)

### 2. Alerts

Set up alerts for:
- Users still using Crypt(3) passwords (check for WARNING logs)
- High rate of authentication failures
- Session key generation spikes
- HMAC authentication mismatches

### 3. Log Rotation

Configure logrotate for authentication logs:
```bash
# /etc/logrotate.d/polaric-auth
/var/log/polaric/auth.log {
    daily
    rotate 30
    compress
    delaycompress
    notifempty
    create 0640 polaric-user polaric-group
    postrotate
        systemctl reload polaric-aprsd > /dev/null 2>&1 || true
    endscript
}
```

## Dependency Management

### 1. Keep Dependencies Updated

Regularly update dependencies to patch security vulnerabilities:
```bash
# Check for outdated dependencies
mvn versions:display-dependency-updates

# Update specific dependencies
mvn versions:use-latest-versions
```

### 2. Vulnerability Scanning

Use OWASP Dependency-Check to scan for known vulnerabilities:

Add to pom.xml:
```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>8.4.0</version>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Run checks:
```bash
mvn dependency-check:check
```

## Application Hardening

### 1. Run as Non-Root User

Never run Arctic-Core as root:
```bash
# Create dedicated user
useradd -r -s /bin/false -d /var/lib/polaric polaric-user

# Configure systemd service
[Service]
User=polaric-user
Group=polaric-group
```

### 2. Java Security Manager (Optional)

For high-security environments, consider running with Java Security Manager:
```bash
java -Djava.security.manager \
     -Djava.security.policy=/etc/polaric/security.policy \
     -jar arctic-core.jar
```

### 3. Resource Limits

Set resource limits to prevent DoS:
```ini
[Service]
LimitNOFILE=4096
LimitNPROC=512
LimitMEMLOCK=0
LimitAS=2G
```

## Incident Response

### 1. Compromised Session Key

If a session key is compromised:

1. Identify affected user
2. Remove from `/var/lib/polaric/logins.dat`
3. Force re-authentication
4. Review access logs for unauthorized activity
5. Consider rotating encryption keys

### 2. Suspicious Authentication Activity

1. Review authentication logs
2. Identify source IPs
3. Check for successful unauthorized logins
4. Block malicious IPs at firewall
5. Notify affected users if needed
6. Consider password resets

### 3. Vulnerability Disclosure

If you discover a security vulnerability:

1. Do not disclose publicly immediately
2. Report to maintainers (see SECURITY.md or GitHub Security Advisory)
3. Allow reasonable time for patch development
4. Coordinate disclosure timeline

## Security Checklist

Before deploying to production:

- [ ] HTTPS configured and enforced
- [ ] All passwords using bcrypt or stronger (no Crypt3)
- [ ] Password files have mode 600
- [ ] Key files have mode 600  
- [ ] CORS properly configured (no wildcard)
- [ ] Rate limiting enabled
- [ ] Firewall rules configured
- [ ] Running as non-root user
- [ ] Logging and monitoring enabled
- [ ] Dependencies scanned for vulnerabilities
- [ ] Backup and recovery procedures documented
- [ ] Incident response plan in place

## Additional Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [CWE/SANS Top 25](https://www.sans.org/top25-software-errors/)
- [Java Cryptography Architecture Guide](https://docs.oracle.com/en/java/javase/17/security/java-cryptography-architecture-jca-reference-guide.html)
- [NIST Cryptographic Standards](https://csrc.nist.gov/projects/cryptographic-standards-and-guidelines)

---

**Document Version:** 1.0  
**Last Updated:** 2025-10-02
