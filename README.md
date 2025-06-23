# Core framework/library for servers
Polaric-aprsd is being split into two artifacts. This is a general framework for creating servers for REST APIs and websockets. It has a focus on security.
* It is based on Javalin and Pac4J
* It uses Maven to build and manage dependencies
* It supports writing applications in Java and/or Kotlin
* It supports authentication using Arctic-HMAC where logon sessions are represented with a shared secret. It supports username password logon. It can be extended with other logon mechanisms supported by Pac4J.
* By default it has 3 authorisation levels: Basic (login), Operator and Admin (superuser). It supports role-based authorisation that can be used by applications.
* It supports writing REST APIs and Websocket-protocols. A publish-subscribe protocol with rooms (that can be defined by application) is supplied by default.
* It supports server-to-server communication (REST or Websocket). Authentication based on a shared secret. Suitable for IoT devices like e.g. Arctic tracker.
* License is AGPL 3.0. 

Work is in progress porting Polaric-Aprsd to this framework. More info will come. Interested? Stay tuned. 

