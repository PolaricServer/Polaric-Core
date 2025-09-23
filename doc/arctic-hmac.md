 
# Arctic-HMAC Authentication Mechanism

Arctic-HMAC is a stateless, secure authentication mechanism provided by the Arctic Core framework. It is designed for both REST APIs and websocket communication, and is well-suited for authenticating users, devices, and server-to-server connections with minimal overhead. Arctic-HMAC is implemented via the `HmacAuthenticator` class and builds upon standard HMAC-SHA256 cryptography.

## Key Concepts

- **Shared Secret**: Each user or device has an associated secret key ("session key"), managed server-side.
- **Token-Based Authentication**: Requests are authenticated using a token constructed from the user ID, a random nonce, and an HMAC digest.
- **Stateless**: No session state needs to be kept on the server between requests, making it suitable for distributed and scalable systems.

## Authentication Flow

1. **Session Key Assignment**:  
   - Upon successful login (via username/password or other mechanism), the server generates and stores a session key for the user (`setUserKey`).
   - Keys for devices/servers are stored in a dedicated key file.

2. **Token Generation (Client Side)**:  
   - For each authenticated request, the client generates:
     - `userid`: The user or device identifier.
     - `nonce`: A random, base64-encoded 8-byte value (to prevent replay attacks).
     - `data`: A digest of the request body (for POST/PUT) or empty for GET.
     - `hmac`: An HMAC-SHA256 digest (base64-encoded and truncated) of `nonce + data`, using the session key.
   - The client then constructs an **Authorization header** (or URL query string for websocket):
     ```
     Authorization: Arctic-Hmac userid;nonce;hmac
     ```

3. **Token Validation (Server Side)**:  
   - The server receives the request and extracts the `userid`, `nonce`, and `hmac` from the header.
   - It verifies:
     - The `userid` exists and has a valid session key.
     - The `nonce` has not been seen before (via `DuplicateChecker`).
     - The HMAC digest matches the expected value for the given key and data.
   - If valid, authentication succeeds and a user profile is created for the request.

4. **Replay Protection**:  
   - Nonces are tracked using a Cuckoo filter to prevent replay attacks. Reuse of a nonce will lead to authentication failure.

5. **Key Expiry**:  
   - Session keys expire after a configurable period (default: 7 days). Expired keys are removed and re-authentication is required.

## Integration

- **REST APIs**: Add the Arctic-HMAC token to the Authorization header for each request.
- **Websockets**: Pass the token as a query string when connecting. The server validates the token upon connection.
- **Device/Server Authentication**: Device keys are managed separately and allow peer-to-peer communication with similar authentication.

## Security Features

- **No Plaintext Transmission**: The secret key is never sent across the network.
- **Replay Attack Mitigation**: Random nonces and server-side duplicate checking.
- **HMAC-SHA256**: Strong cryptographic integrity and authentication.
- **Role Support**: Optionally, a role can be included in the token to specify authorization context.

## Example

**Authorization header format:**
```
Arctic-Hmac userid;base64nonce;base64hmac[;role]
```
**Websocket query string:**
```
userid;base64nonce;base64hmac[;role]
```

## Summary

Arctic-HMAC provides a secure, efficient, and stateless authentication method for Arctic Core applications, suitable for both user and device authentication in REST and websocket environments, with built-in replay protection and session key management.
