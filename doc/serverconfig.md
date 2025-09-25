# The Server Config interface
 
 The [*ServerConfig*](https://sarhack.no/apidocs/arctic-core/no/arctic/core/ServerConfig.html) interface in Arctic-Core defines the central contract for server configuration and management. Its purpose is to standardize how core server implementation components access configuration properties, logging, web server integration, etc. 

## Usage by Applications:

Applications typically implement or extend the *ServerConfig* interface (for example, via the [*ConfigBase*](https://sarhack.no/apidocs/arctic-core/no/arctic/core/ConfigBase.html) abstract class) to provide concrete configuration logic. Application code can then access configuration and core services through ServerConfig, such as:

- Reading/writing configuration values.
- Managing users and groups.
- Handling web server statistics and HTTP protection.
- Sending notifications to users.
- Registering shutdown handlers.
 
An application may use *ServerConfig* to retrieve configuration properties, send notifications, or interact with the web server and user database, all through a unified interface.

## Key recommendations

The recommended way to implement the methods of the *ServerConfig* interface in Arctic-Core is to use the provided abstract class *ConfigBase* as a starting point.

- **Extend ConfigBase:**  
  Most of the configuration-related methods (get/set property, *getBoolProperty*, *getIntProperty*, *getPosProperty*, *getConfig*, log, etc.) are already implemented in *ConfigBase*. Extend this class in your application to inherit these default behaviors.

- **Implement/override interface methods:**  
  For any methods in ServerConfig not fully implemented in ConfigBase (such as *properties()*, *getWebserver()*, *addShutdownHandler()*, etc.), provide concrete implementations in your subclass.

- **Integrate with application components:**  
  Connect your implementations to actual web server, user database, and pub/sub systems as needed. The WebServer class is an example implementation of the *ServerConfig.Web* interface.

- **Use dependency injection:**  
  Pass instances of your *ServerConfig* interface to application modules that need access to configuration, logging, or server services.

**Example usage:**

```java
public class Main extends ConfigBase {
    private MyWebServer webserver;

    public Main() {
        // initialize configuration, logging, etc.
        webserver = new MyWebServer(this, ...);
    }

    @Override
    public Web getWebserver() {
        return webserver;
    }

    @Override
    public void addShutdownHandler(SimpleCb cb) {
        // Store and call shutdown handlers
    }

    // Implement additional required methods...

    // A static main method could also be put her to start up the application
}
