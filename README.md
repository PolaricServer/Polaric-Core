# Core framework/library for servers
Arctic-Core is a general framework for creating servers for REST APIs and websocket-communication. It has a focus on security. It was originally part of the Polaric-Aprsd codebase. 
* It is based on Javalin and Pac4J
* It uses Maven to build and manage dependencies
* It supports writing applications in Java and/or Kotlin
* It supports authentication using Arctic-HMAC where logon sessions are represented with a shared secret. It supports/username password logon to establish sessions. It can be extended with other logon mechanisms supported by Pac4J.
* By default it has 3 authorisation levels: Basic (login), Operator-level (some privileges) and Admin (superuser). It also offers *role-based authorisation* that can be used by applications.
* It supports writing REST APIs and Websocket-protocols. A publish-subscribe protocol with rooms (that can be defined by application) is offered by default.
* It supports server-to-server communication (REST or Websocket). Authentication based on a shared secret. Suitable for IoT devices like e.g. Arctic tracker.
* License is AGPL 3.0. 

Work is in progress porting (the rest of) Polaric-Aprsd to this framework. 

## Getting started
To build and install it (locally) use 'mvn package' and 'mvn install'. Then it is rather straightforward to add it as a dependency in (maven) pom.xml. We plan to publish it in the maven central. 

We hope to come with some more documentation on how to create server-applications based on *arctic-core* along with a simple example. More info will come. Interested? Stay tuned. Feel free to participate. 
