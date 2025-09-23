# Arctic-Core Framework

Arctic-Core is a flexible framework for building secure servers with REST APIs and websocket communication. It originated from the Polaric-Aprsd project and is designed to support modern authentication, authorization, and real-time messaging patterns.

## Features

- **Built on Javalin & Pac4J:** Modern Java/Kotlin web stack for REST and websocket services.
- **Maven-based:** Easy to build and integrate as a dependency.
- **Pluggable Authentication:** Supports Arctic-HMAC (shared secret) and username/password by default, and can be extended via Pac4J with OAuth, SAML, etc.
- **Role-Based Authorization:** Three built-in levels: Basic (login), Operator, and Admin, plus application-defined roles.
- **Session Management:** Shared user sessions across multiple logins with customizable open/close handlers.
- **Publish/Subscribe Websocket Service:** Application-defined "rooms", user notification, and server-to-server messaging.
- **Device & Peer Authentication:** Suitable for IoT devices and server clusters, using shared secrets.
- **Extensible:** Designed to be subclassed and extended for custom server applications.
- **AGPL v3 Licensed:** Free and open source.

## Getting Started

1. **Build & Install Locally**
   ```shell
   mvn package
   mvn install
   ```
   Then add as a dependency in your Maven `pom.xml`.

2. **Example Application**
   See [`arctic-core-example`](https://github.com/PolaricServer/arctic-core-example) for a minimal server implementation.


## Contributing

Contributions and feedback are welcome! It is still a bit work-in-progress. Interested? Stay tuned!

