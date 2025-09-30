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
 
package no.polaric.core.auth;
import no.polaric.core.*;
import no.polaric.core.httpd.*;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.*;
import java.util.List;
import java.util.*;


/**
 * Authorizer for devices. 
 */
public class DeviceAuthorizer implements Authorizer {

    public DeviceAuthorizer() {
    }


    /*
     * It is very simple. Just check if the device is authenticated and is given a profile
     */
    @Override    
    public boolean isAuthorized(final WebContext context, final SessionStore ss, final List<UserProfile> profile) {

        Optional<CommonProfile> prof = AuthInfo.getSessionProfile(context);
        if (prof.isPresent()) {
            var svc = prof.get().getAttribute("service") ;
            if (svc != null)
                return true;
        }
        return false;
    }

 

    @Override
    public String toString() {
        return "DeviceAuthorizer";
    }
}
