/* 
 * Copyright (C) 2017-2025 by LA7ECA, Øyvind Hanssen (ohanssen@acm.org)
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
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.context.CallContext;

import java.io.*; 
import java.util.*; 
import org.apache.commons.codec.digest.*;
import java.security.MessageDigest;




/**
 * Use a simple password file. For small numbers of user/password pairs. 
 * For larger numbers of users, consider using a database instead. 
 */
public class PasswordFileAuthenticator implements Authenticator {

    private final Map<String, String> _pwmap = new HashMap<String, String>();
    private ServerConfig _conf; 
    private final String _file; 
    private UserDb _users;
    
    
    public PasswordFileAuthenticator(ServerConfig conf, String file, UserDb lu) {
        _conf = conf; 
        _file = file; 
        _users = lu; 
        load();
    }
       

    
    /**
     * Load passwords (hashes) from file.
     */
    public void load() {
        try {
            /* Open passwd file */
            if (_file == null) {
                _conf.log().warn("PasswordFileAuthenticator", "Password file name not set");
                return;
            }
                
            BufferedReader rd = new BufferedReader(new FileReader(_file));
            while (rd.ready() )
            {
                /* Read a line */
                String line = rd.readLine();
                if (!line.startsWith("#") && line.length() > 1) {
                    if (line.matches(".*:.*"))
                    {                 
                        String[] x = line.split(":");  
                        String username = x[0].trim();
                        String passwd = x[1].trim();
                        _pwmap.put(username, passwd);
                        if (_users != null) 
                            _users.add(username);
                    }
                    else
                        _conf.log().warn("PasswordFileAuthenticator", "Bad line in password file: "+line);
                }
            }
        }
        catch (IOException e) {
           _conf.log().warn("PasswordFileAuthenticator", "Couldn't open htpasswd file: "+e.getMessage());
        } 
    }
    
    
    
    @Override
    public Optional<Credentials> validate(CallContext context, Credentials cred) 
           throws CredentialsException 
    {
        if (cred == null) 
            throwsException("No credential");
        if (! (cred instanceof UsernamePasswordCredentials))
            throwsException("Credentials is not username/password type");
            
        UsernamePasswordCredentials credentials = (UsernamePasswordCredentials) cred;
        
        String username = credentials.getUsername();
        String password = credentials.getPassword();
        if (CommonHelper.isBlank(username)) {
            throwsException("Username cannot be blank");
        }
        if (CommonHelper.isBlank(password)) {
            throwsException("Password cannot be blank");
        }
        

        String storedPwd = _pwmap.get(username);
        if (storedPwd == null)
            throwsException("Unknown user: '"+username+"'");
           
        User u = _users.get(username);
        if (u.isSuspended())
            throwsException("User is suspended: "+username);
                  
        if (storedPwd.startsWith("$apr1$")) { 
            if (!storedPwd.equals(Md5Crypt.apr1Crypt(password, storedPwd)))
                throwsException("Invalid password");
        }
        /* Old Crypt(3) algorithm for compatibility. Insecure */
        else if (storedPwd.length() == 13) {
            _conf.log().warn("PasswordFileAuthenticator", 
                "WARNING: User '"+username+"' is using deprecated Crypt(3) password hashing. " +
                "This algorithm is cryptographically weak and should be upgraded to bcrypt (apr1) immediately.");
            String pw = (password.length() <= 8 ? password : password.substring(0,8));
            if (!storedPwd.equals(Crypt.crypt(pw, storedPwd.substring(0,2))))
            throwsException("Invalid password");
        }
        else
            throwsException("Unknown password format for user: "+username);
        
        /* Create a user profile */
        final CommonProfile profile = new CommonProfile();
        profile.setId(username);
        profile.addAttribute("userInfo", _users.get(username));
        
        credentials.setUserProfile(profile);
        return Optional.of(credentials);
    }

    
    
    
    protected void throwsException(final String message) throws CredentialsException {
        _conf.log().info("PasswordFileAuthenticator", "Auth failed: "+message);
        throw new CredentialsException(message);
    }
    
    
}
