# Security Analysis Report

This document outlines security vulnerabilities identified in the Arctic-Core codebase and provides recommendations for remediation.

## Executive Summary

A security review of the Arctic-Core codebase has identified several vulnerabilities ranging from critical cryptographic weaknesses to potential denial-of-service issues. The most critical issues involve the use of deprecated and weak cryptographic algorithms.

## Critical Vulnerabilities

### 1. Weak Pseudo-Random Number Generator (SHA1PRNG)

**Location:** `src/util/SecUtils.java`, lines 40-47

**Issue:** The code uses `SHA1PRNG` algorithm for generating random numbers:
```java
_rand = SecureRandom.getInstance("SHA1PRNG");
```

**Risk:** 
- SHA1PRNG is deprecated and considered weak for cryptographic purposes
- It may have predictable output patterns
- Modern alternatives provide better entropy and security guarantees

**Impact:** HIGH - Used for generating session keys and nonces for authentication

**Recommendation:**
Use the platform's default SecureRandom implementation:
```java
_rand = new SecureRandom(); // Uses platform default (NativePRNG on Linux)
```

---

### 2. Legacy Crypt(3) Password Support

**Location:** `src/auth/PasswordFileAuthenticator.java`, lines 131-135

**Issue:** The authenticator supports the old Unix Crypt(3) algorithm:
```java
/* Old Crypt(3) algorithm for compatibility. Insecure */
else if (storedPwd.length() == 13) {
    String pw = (password.length() <= 8 ? password : password.substring(0,8));
    if (!storedPwd.equals(Crypt.crypt(pw, storedPwd.substring(0,2))))
```

**Risk:**
- Crypt(3) only uses first 8 characters of password
- Uses DES encryption which is broken
- Weak salt (only 2 characters)
- Very fast to brute force with modern hardware

**Impact:** HIGH - Allows authentication with weak password hashes

**Recommendation:**
- Remove support for Crypt(3) algorithm
- Force migration to stronger algorithms (bcrypt, scrypt, or Argon2)
- Add migration warning in logs

---

### 3. MD5 Hash Usage

**Location:** `src/util/SecUtils.java`, lines 86-88

**Issue:** The code provides MD5 hashing functionality:
```java
/* Computes MD5 hash */
public final static byte[] digest( byte[] bytes, String txt )
    { return _digest(bytes, txt, "MD5"); }
```

**Risk:**
- MD5 is cryptographically broken (collision attacks exist)
- Should not be used for any security-sensitive operations
- Can be confused with secure hashing in security contexts

**Impact:** MEDIUM - May be used for non-security purposes, but presence is risky

**Recommendation:**
- Remove MD5 support or clearly deprecate it
- Add warnings in documentation
- Use SHA-256 or SHA-3 for all hashing needs

---

## Medium Severity Issues

### 4. Fixed-Size Nonce Duplicate Checker

**Location:** `src/auth/HmacAuthenticator.java`, line 61

**Issue:** Nonce duplicate checker uses fixed capacity:
```java
private final DuplicateChecker _dup = new DuplicateChecker(2000);
```

**Risk:**
- After 2000 requests, old nonces are forgotten
- Attacker could replay requests after cache eviction
- No time-based expiration of nonces

**Impact:** MEDIUM - Potential replay attack vector

**Recommendation:**
- Add time-based nonce expiration (e.g., 5 minutes)
- Increase cache size or make it configurable
- Consider using a timestamp in nonce validation

---

### 5. Thread-Safe LRUCache

**Location:** `src/util/LRUCache.java`

**Issue:** The LRUCache implementation is not thread-safe:
```java
public T get(String key) {
    T item = _cache.get(key);
    if (item != null) {
        _cache.remove(key);
        _cache.put(key, item);
    }
    return item;
}
```

**Risk:**
- Race conditions in concurrent access
- Potential cache corruption
- LinkedHashMap is not thread-safe

**Impact:** MEDIUM - May cause authentication issues under load

**Recommendation:**
- Use `Collections.synchronizedMap()` or `ConcurrentHashMap`
- Add proper synchronization blocks
- Consider using existing thread-safe cache implementations

---

### 6. Information Disclosure in Logs

**Location:** Multiple files (PasswordFileAuthenticator.java, HmacAuthenticator.java)

**Issue:** Authentication failures log detailed information:
```java
_conf.log().info("PasswordFileAuthenticator", "Auth failed: "+message);
```

**Risk:**
- Logs username enumeration attempts
- May help attackers identify valid usernames
- Could fill logs with authentication attempts

**Impact:** LOW-MEDIUM - Information disclosure

**Recommendation:**
- Use generic error messages for external responses
- Log detailed info only at DEBUG level
- Implement rate limiting for failed authentication

---

## Low Severity Issues

### 7. Session Key Expiration

**Location:** `src/auth/HmacAuthenticator.java`, lines 98-110

**Issue:** Session keys expire after 7 days but only checked on use

**Risk:**
- Keys remain valid until next use after expiration
- No automatic cleanup of expired sessions

**Impact:** LOW - Expired sessions may remain longer than intended

**Recommendation:**
- Add periodic cleanup task for expired sessions
- Implement more granular session timeout options

---

### 8. Direct Login Key Security

**Location:** `src/auth/AuthService.java`, lines 196-213

**Issue:** Comment warns about sending keys on encrypted channels:
```java
/* 
 * This returns a key, be sure that it is only sent on encrypted channels in production 
 * enviroments. 
 */
```

**Risk:**
- No enforcement of HTTPS requirement
- Relies on deployment configuration

**Impact:** LOW - Depends on proper deployment

**Recommendation:**
- Add HTTPS enforcement in code
- Reject requests on non-encrypted connections
- Add security headers

---

## Best Practice Recommendations

### 1. Add Security Headers
Add standard security headers:
- `Strict-Transport-Security`
- `X-Content-Type-Options`
- `X-Frame-Options`
- `Content-Security-Policy`

### 2. Implement Rate Limiting
Add rate limiting for:
- Login attempts
- HMAC authentication failures
- API requests

### 3. Add Dependency Scanning
- Use OWASP Dependency Check
- Keep dependencies up to date
- Monitor for security advisories

### 4. Security Testing
- Add unit tests for authentication logic
- Implement security integration tests
- Consider penetration testing

### 5. Documentation
- Document security assumptions
- Provide secure deployment guidelines
- Add security configuration examples

---

## Priority Recommendations

**Immediate (Critical):**
1. Replace SHA1PRNG with default SecureRandom
2. Deprecate or remove Crypt(3) support
3. Document MD5 usage risks

**Short Term (Medium):**
4. Make nonce cache configurable with time-based expiration
5. Add thread-safety to LRUCache
6. Improve authentication logging

**Long Term (Low):**
7. Implement periodic session cleanup
8. Add HTTPS enforcement
9. Comprehensive security testing

---

## Conclusion

While the codebase shows good security practices in many areas (HMAC authentication, nonce checking, session management), the use of deprecated cryptographic algorithms poses significant risks. Priority should be given to replacing SHA1PRNG and removing support for weak password hashing algorithms.

The authentication system is generally well-designed, but would benefit from additional hardening through rate limiting, better thread safety, and more robust nonce management.

---

**Report Date:** 2025-10-02  
**Reviewed Components:**
- Authentication System
- Cryptographic Utilities
- Session Management
- Password Handling
