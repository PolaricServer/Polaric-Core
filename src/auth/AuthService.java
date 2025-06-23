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


package no.arctic.core.auth;
import no.arctic.core.*;
import no.arctic.core.httpd.*;
import no.arctic.core.util.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.*; 
import org.pac4j.core.context.WebContext;
import org.pac4j.core.config.Config;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.jee.context.session.*;
import org.pac4j.javalin.*;
import java.util.Optional;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.NamedType;



/**
 * Web services for login, autentication and authorization. 
 */
 
public class AuthService {
    
    private static ServerAPI _api; // FIXME: This is static. 
    private static Logfile  _log;
    
    private Config _authConf; 
    private String _allowOrigin, _passwdFile, _userFile, _groupFile, _dkeyFile, _ukeyFile; 
    private LocalGroups _groups;
    private LocalUsers _users; 
    private PasswordFileAuthenticator _passwds;
    private HmacAuthenticator _hmac;
   
   
    /* 
     * Jackson JSON mapper ..
     */ 
    protected final static ObjectMapper mapper = new ObjectMapper();
   
    public static String toJson(Object obj) 
    {
      try{
         return mapper.writeValueAsString(obj);
      }
      catch (Exception e) {
            return null;
        }
    }
    
    
    public AuthService(ServerAPI api) {
       _api = api; 
       _log = new Logfile(api, "auth", "auth.log");        
       _allowOrigin = api.getProperty("httpserver.alloworigin", ".*");
       _userFile    = api.getProperty("httpserver.userfile",    "/var/lib/polaric/users.dat");
       _groupFile   = api.getProperty("httpserver.groupfile",   "/etc/polaric-aprsd/groups");
       _passwdFile  = api.getProperty("httpserver.passwdfile",  "/etc/polaric-aprsd/passwd");
       _dkeyFile    = api.getProperty("httpserver.keyfile",     "/etc/polaric-aprsd/keys/peers");
       _ukeyFile    = api.getProperty("httpserver.loginkeyfile","/var/lib/polaric/logins.dat");
       
       _groups = new LocalGroups(api, _groupFile);
       _users = new LocalUsers(api, _userFile, _groups, this);
       _passwds = new PasswordFileAuthenticator(api, _passwdFile, _users);
       _hmac = new HmacAuthenticator(api, _dkeyFile, _ukeyFile, _users);
       _authConf = new AuthConfig(_passwds, _hmac).build();
    }
   
   
   
    /** Return the configuration */
    public Config conf() 
       { return _authConf; }
       
       
    public HmacAuthenticator hmacAuth() 
       { return _hmac; }
       
       
    public void reloadPasswds() {
      _passwds.load();
    }
       

       
    /** Set up the services. */
    public void start(Javalin a) {
      
      /* 
       * OPTIONS requests (CORS preflight) are not sent with cookies and should not go 
       * through the auth check. 
       * Maybe we do this only for REST APIs and return info more exactly what options
       * are available? Move it inside the corsEnable method? 
       */
      a.before("*", ctx -> {
            if (ctx.method().name() == "OPTIONS") {
                corsHeaders(ctx); 
                ctx.status(200);
                ctx.skipRemainingHandlers();
            }
        });
        
      /* Set CORS headers. */
      a.before ("*", ctx -> { corsHeaders(ctx); } ); 
              
      /* Login with username and password */
      a.before("/directLogin", new SecurityHandler(_authConf, "DirectFormClient")); 
              
      /* SHA256 Hash of body */
      a.before("*", AuthService::genBodyDigest);
      
      a.before("/authStatus",  new SecurityHandler(_authConf, "HeaderClient"));


       /* 
        * For all routes, put an AuthInfo object on the request. Here we rely on sessions to remember 
        * user-profiles between requests. IF we use direct clients (stateless server) 
        * this will not work for paths without authenticators!! 
        */
      a.before("/hmacTest",   AuthService::getAuthInfo);
      a.before("/authStatus", AuthService::getAuthInfo);

      a.post("/directLogin", ctx -> { directLogin(ctx); });   // Indicate login success
      a.get("/authStatus",   AuthService::authStatus);        // Return authorisation status
      a.get("/authStatus2",  AuthService::authStatus);        // Return authorisation status without authentication

    }
    

    
    
    /**
     * Allowed origin (for CORS). If Origin header from request matches allowed origins regex. 
     * return it. Otherwise, null.
     */
    public String getAllowOrigin(Context ctx) {
        String origin = ctx.header("Origin");
        if (origin != null && origin.matches(_allowOrigin))
           return origin;
        else
           return "";
    }
    
    
    
    /**
     * Produce CORS headers. If Origin header from request matches allowed origins regex. 
     * add it to the Allow-Origin response header. 
     * FIXME: How is CORS done in Javalin???
     */
    public void corsHeaders(Context ctx) {
        ctx.header("Access-Control-Allow-Headers", "Authorization"); 
        ctx.header("Access-Control-Allow-Credentials", "true"); 
        ctx.header("Access-Control-Allow-Origin", getAllowOrigin(ctx)); 
        ctx.header("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE, OPTIONS");
    }
    
    
        
   public UserDb userDb() {
      return _users; 
   }
   public GroupDb groupDb() {
      return _groups; 
   }
   
   
    /**
     * Return authorization status (as JSON)
     */
    public static void authStatus(Context ctx) {
        AuthInfo auth = new AuthInfo(_api, new JavalinWebContext(ctx));
        ctx.json(auth);
    }

    
    /* 
     * This returns a key, be sure that it is only sent on encrypted channels in production 
     * enviroments. 
     */
    public void directLogin(Context ctx) {
      Optional<CommonProfile> profile = AuthInfo.getSessionProfile(ctx); 
         
      String userid = profile.get().getId();
      String key =  _hmac.getUserKey(userid); 
      
      /* If key not found, generate a new one */
      if (key == null)
         key = SecUtils.b64encode(SecUtils.getRandom(48)); // Gives 64 bytes when encoded 
      
      _hmac.setUserKey(userid, key);
      _log.log("Successful login from: "+ctx.ip()+", userid="+ userid);
      ctx.result(key);
    }
    

    
   /** 
    * Generate a SHA256 digest of the request body. 
    * The resulting digiest is added as an attribute "bodyHash" on the request
    */
   public static void genBodyDigest(Context ctx) {
      String body = ctx.body();
      String digest = (body==null || body.length() == 0 ? "" : SecUtils.xDigestB64(body, 44));
      ctx.attribute("bodyHash", digest);
   }
    

    
    /** 
     * Create an AuthInfo object from the user profile and add it as an 
     * attribute on the request. 
     * This is not the same as the user profile generated by Pac4J. The AuthInfo 
     * will be available for application and contains more information about 
     * authorizations etc
     */
   public static AuthInfo getAuthInfo(WebContext context)
   {
      Optional<AuthInfo> ainfo = context.getRequestAttribute("authinfo");
      if (ainfo.isPresent()) 
         return ainfo.get();
      
      AuthInfo auth = new AuthInfo(_api, context); 
      context.setRequestAttribute("authinfo", auth);
      return auth;
   }
   
   
   
   public static AuthInfo getAuthInfo(Context ctx)
   {
      return getAuthInfo(new JavalinWebContext(ctx));
   }
    
}
