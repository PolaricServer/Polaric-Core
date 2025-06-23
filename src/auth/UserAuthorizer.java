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
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.*;
import java.util.List;


/**
 * Authorizer for users. Instantiated with an authorization level (0=login, 1=operator, 2=admin). 
 */
public class UserAuthorizer implements Authorizer {

    private boolean admin = false;
    private boolean operator = false;

    
    public UserAuthorizer() { }

    
    public UserAuthorizer(final int lvl) {
        admin = (lvl >= 2);
        operator = (lvl >= 1);
    }
    
    
    /**
     * Authorize if user has been assigned the specified level. 
     */
    @Override
    public boolean isAuthorized(WebContext ctx, final SessionStore ss, final List<UserProfile> profile) {
        var auth = AuthService.getAuthInfo(ctx);  
        if (auth==null)
            return false;
        if (admin) return auth.admin; 
        if (operator) return auth.operator || auth.admin;
        return true;

    }

 

    @Override
    public String toString() {
        return "UserAuthorizer[" + (admin? "admin" : "operator") + "]";
    }
}
