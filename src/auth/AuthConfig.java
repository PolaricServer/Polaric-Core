/* 
 * Copyright (C) 2025 by LA7ECA, Øyvind Hanssen (ohanssen@acm.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
 
package no.arctic.core.auth;
import no.arctic.core.*;
import no.arctic.core.httpd.*;
import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.direct.AnonymousClient;
import org.pac4j.core.config.Config;
import org.pac4j.core.config.ConfigFactory;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.matching.matcher.PathMatcher;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.factory.ProfileManagerFactory;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.http.client.indirect.*;
import org.pac4j.http.client.direct.*;
import org.pac4j.javalin.JavalinContextFactory;
import org.pac4j.jee.context.session.JEESessionStoreFactory;

import java.util.Optional;



/*
 * The security configuration must be defined via a Config object.
 * Consists of: 
 * -  Clients (authentication mechanisms)
 * -  Authenticators (credentials validation)
 * -  Authorizers (authorization checks)
 * -  Matchers
*
 * A Client represents a web authentication mechanism. It performs the login process and returns 
 * (if successful) a user profile. HTTP clients require an Authenticator to validate the credentials.
 *
 * Authorizers are meant to check authorizations when accessing an URL (in the “security filter”); 
 * either on the authenticated user profile or on the web context.
 *
 * Matcher: The “security filter” is in charge of protecting URL, requesting authentication and 
 * optionally authorization.
 */

 
public class AuthConfig implements ConfigFactory {

    Authenticator _hmac; 
    Authenticator _passwds;
    
    public AuthConfig(Authenticator pw, Authenticator hmauth) {
        _passwds = pw;
        _hmac = hmauth;
    }

    
    @Override
    public Config build(Object... parameters) {

        /* Direct HMAC Auth client */         
        final HeaderClient hdrClient = new HeaderClient("Authorization", "Arctic-Hmac", _hmac);
        /* Direct Form Auth client (for username/password) */
        final DirectFormClient dformClient = new DirectFormClient(_passwds);
        
        
        Clients clients = new Clients(
            hdrClient,
            dformClient
        );

        
        /* Set up the config. 
         * Clients
         * Authorizers, matchers, 
         * etc.. 
         */
        Config config = new Config(clients);
        config.addAuthorizer("isuser",    new UserAuthorizer(0));
        config.addAuthorizer("operator",  new UserAuthorizer(1));
        config.addAuthorizer("admin",     new UserAuthorizer(2));
        config.addAuthorizer("device",    new DeviceAuthorizer());
                 
        config.setWebContextFactory(JavalinContextFactory.INSTANCE);
        config.setSessionStoreFactory(JEESessionStoreFactory.INSTANCE);
        config.setProfileManagerFactory(ProfileManagerFactory.DEFAULT);
        return config;
    }
}
