# Arctic-Core Framework

Arctic-Core is a flexible framework for building secure servers with REST APIs and websocket communication. It originated from the Polaric-Aprsd project and is designed to support modern authentication, authorization, and real-time messaging patterns.

## Features

- **Built on Javalin & Pac4J:** Modern Java/Kotlin web stack for REST and websocket services.
- **Maven-based:** Easy to build and integrate as a dependency.
- **Pluggable Authentication:** Supports Arctic-HMAC (shared secret), username/password, and can be extended via Pac4J with OAuth, SAML, etc.
- **Role-Based Authorization:** Three built-in levels: Basic (login), Operator, and Admin, plus application-defined roles.
- **Session Management:** Shared user sessions across multiple logins with customizable open/close handlers.
- **Publish/Subscribe Websocket Service:** Application-defined "rooms", user notification, and server-to-server messaging.
- **Device & Peer Authentication:** Suitable for IoT devices and server clusters, using shared secrets.
- **Extensible & Documented:** Designed to be subclassed and extended for custom server applications.
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

3. **Documentation & API Reference**
   - See the [`doc`](doc/) directory for detailed guides and code references.
   - **Authentication & Authorization**
     - [AuthConfig.java](doc/auth/AuthConfig.md) – Security configuration and clients setup
     - [AuthService.java](doc/auth/AuthService.md) – Web services for login/authentication
     - [AuthInfo.java](doc/auth/AuthInfo.md) – User/session info and authorization logic
     - [PasswordFileAuthenticator.java](doc/auth/PasswordFileAuthenticator.md) – Username/password authentication
     - [HmacAuthenticator.java](doc/auth/HmacAuthenticator.md) – Arctic-HMAC shared secret authentication
     - [UserAuthorizer.java](doc/auth/UserAuthorizer.md), [DeviceAuthorizer.java](doc/auth/DeviceAuthorizer.md) – Role and device authorization
     - [Group.java](doc/auth/Group.md), [GroupDb.java](doc/auth/GroupDb.md), [LocalGroups.java](doc/auth/LocalGroups.md) – Role/group definitions
     - [User.java](doc/auth/User.md), [UserDb.java](doc/auth/UserDb.md), [LocalUsers.java](doc/auth/LocalUsers.md) – User database and attributes
   - **Websocket & Messaging**
     - [WsNotifier.java](doc/httpd/WsNotifier.md) – Websocket base class with session management
     - [PubSub.java](doc/httpd/PubSub.md) – Generic publish/subscribe protocol
     - [NodeWs.java](doc/httpd/NodeWs.md), [NodeWsApi.java](doc/httpd/NodeWsApi.md), [NodeWsClient.java](doc/httpd/NodeWsClient.md) – Server-to-server messaging and node management
   - **REST API & Server Base**
     - [ServerBase.java](doc/httpd/ServerBase.md) – Base class for REST services
     - [WebServer.java](doc/httpd/WebServer.md) – Main server class
     - [Services.java](doc/httpd/Services.md) – Example RESTful services
     - [RestClient.java](doc/httpd/RestClient.md) – Secure REST client
   - **Utilities**
     - [SecUtils.java](doc/util/SecUtils.md) – Security-related helper functions
     - [Base64.java](doc/util/Base64.md) – Base64 encoding/decoding
     - [LRUCache.java](doc/util/LRUCache.md) – LRU cache implementation
     - [ZeroConf.java](doc/util/ZeroConf.md) – mDNS/ZeroConf service discovery
     - [Logfile.java](doc/Logfile.md) – Logging utility
     - [ConfigBase.java](doc/ConfigBase.md), [ServerConfig.java](doc/ServerConfig.md) – Configuration interfaces

## Example Usage

See [`arctic-core-example/src/Main.java`](https://github.com/PolaricServer/arctic-core-example/blob/main/src/Main.java) for how to start an Arctic-Core based server:

```java
public static void main(String[] args) 
{
    Main setup = new Main(); 
    setup.settings();
    setup.start();        
}
```

For RESTful APIs and websocket protocols, subclass `ServerBase` or `WsNotifier` and register your endpoints/rooms.

## Contributing

Work is ongoing to port more features from Polaric-Aprsd and develop richer documentation. Contributions and feedback are welcome! See the [`doc`](doc/) directory and open issues or discussions.

## License

[GNU AGPL v3](LICENSE)

---

**See the [`doc/`](doc/) directory for extended documentation and code walkthroughs.**