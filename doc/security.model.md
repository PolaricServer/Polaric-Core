 
# Arctic Core Security Model

Arctic Core provides a robust, flexible security model for Java-based server applications, focusing on authentication, authorization, and session management. The framework leverages [Javalin](https://javalin.io/) for web endpoints and [Pac4J](https://www.pac4j.org/) for security integration, allowing developers to build REST and websocket services with fine-grained access control.

## Authentication

Arctic Core supports multiple authentication mechanisms, configured via Pac4J "Clients":

- **[Arctic-HMAC Authentication](https://github.com/PolaricServer/Arctic-Core/blob/main/doc/arctic-hmac.md):** This mechanism uses a shared secret for each user (session-key) or device (static key). The client includes a username, a nonce, and a HMAC digest in the `Authorization` header (see [`HmacAuthenticator`](https://sarhack.no/apidocs/arctic-core/no/arctic/core/auth/HmacAuthenticator.html)). This enables stateless authentication suitable for REST APIs, websocket connections, and server-to-server communication.
- **Username/Password Authentication:** Supports standard login forms using a local password file ([`PasswordFileAuthenticator`](https://sarhack.no/apidocs/arctic-core/no/arctic/core/auth/PasswordFileAuthenticator.html)). Passwords are securely hashed and checked at login.
- **Extensible Mechanisms:** Additional Pac4J-supported mechanisms (OAuth, SAML, etc.) can be added as needed.

## Authorization

Authorization is role-based and enforced at the route or websocket room level:

- **Roles:** The framework defines three default roles: **user** (basic login), **operator** (elevated permissions), and **admin** (superuser).
- **Authorizers:** Custom authorizer classes (e.g., `UserAuthorizer`, `DeviceAuthorizer`) check user roles and group membership, enabling route protection and granular access control. URLs and websocket rooms can require specific roles or group membership.
- **Groups:** User roles and group affiliations are managed via local files (`LocalGroups`, `LocalUsers`) but can be adapted to databases.

## Session Management

Sessions are handled in two layers:

- **Web Sessions:** Pac4J and Javalin manage HTTP sessions, maintaining user profiles and authentication state across requests.
- **User Sessions:** Arctic Core tracks user login sessions (`AuthInfo.UserSessionInfo`) independently of HTTP sessions. This enables features like shared state for multiple logins by the same user, delayed logout cleanup, and session expiry (default: 1 week).

## Security for Websockets

Websocket endpoints are protected using the same authentication mechanisms:

- Websocket clients provide authentication info (HMAC or session key) as a query string.
- Upon connection, the server verifies credentials and sets up session/authorization state.
- Room-based access control is enforced in services like `PubSub`, enabling secure chat, notifications, or publish/subscribe patterns.

## Other Features

- **CORS:** Configurable CORS headers allow secure cross-origin access.
- **Notifications:** The framework supports system and user notifications, delivered securely via websocket rooms.
- **Extensibility:** The security model is designed to be extensible, supporting plugins, custom session handling, and integration with external identity providers.

## Summary

Arctic Core’s security model combines stateless and session-based authentication, role-based authorization, and flexible session management. It aims to provide secure building blocks for modern, distributed server applications, with emphasis on simplicity, extensibility, and strong defaults.
