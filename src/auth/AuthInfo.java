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
import io.javalin.http.Context;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.jee.context.session.*;
import java.util.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.annotation.*;
import org.pac4j.javalin.*;


/**
 * Authorizations and service config for a given user session.
 * This is instantiated on each request!!
 * Instances of this class can be sent to the client in JSON format. 
 */
public class AuthInfo {

    /* Expire time in minutes. Set to one week */
    public static final int USERSES_EXPIRE = 60 * 24 * 7;
    
    
    /* 
     * User session info (info associated with user login sessions) wrapper with counter 
     * Can be subclassed by application to add application-specific attributes
     */
    public static class UserSessionInfo {
        public String userid;
        private long expire = 0;
        private int cnt = 0;
        private int increment() {return ++cnt;}
        private int decrement() {return --cnt;}
        
        public UserSessionInfo(String uid) {userid=uid;}
    }
    

    
    public String userid;
    public String groupid;
    public String callsign;
    public String servercall;
    public boolean admin = false, operator = false;
    public String tagsAuth;
    public String[] services = null;
    private ServerConfig _conf; 
    
    @JsonIgnore public UserSessionInfo userses = null;
    @JsonIgnore public Group group;
    
    private static List<String> _services = new ArrayList<String>();    
    private static Queue<UserSessionInfo> gcses = new LinkedList<UserSessionInfo>();
    private static Map<String, UserSessionInfo> seslist = new HashMap<String, UserSessionInfo>();
    private static ScheduledExecutorService gc = Executors.newScheduledThreadPool(5);
    private static Map<String, ScheduledFuture> closingSessions = new HashMap<String,ScheduledFuture>();
    
    public static void addService(String srv) {
       _services.add(srv);
    }
    
    
    /*
     * Allow application program to define a function (possibly lambda) that creates a 
     * user-session-info object as well as a function to clean up when closing a session.
     */
     
    public interface SesCreateFunc {
        public UserSessionInfo create(String userid);
    }
    public interface SesCloseFunc {
        public void close(UserSessionInfo ses);
    }
    
    private static SesCreateFunc _usersesfactory; 
    private static SesCloseFunc _usersesclose; 
     
    public static void setUserSesFactory(SesCreateFunc f) {
        _usersesfactory = f;
    }
    public static void setUserSesClose(SesCloseFunc f) {
        _usersesclose = f;
    }
    
    
    
    /**
     * Called by the application to register handlers for opening and closing sessions. 
     * Used when users log in or log out. A session (as defined here) is shared between 
     * logins by the same user-id. A session is opened at the first login and closed
     * after the last logout. After a short delay. After a session is closed it is kept 
     * for some time before expired and removed. Default: 1 week. 
     * 
     * Callback-functions on the webserver are called when sessions are  
     * opened and closed. These can be specified (as lambda-functions) by the 
     * application program using WebServer class: onLogin() and onLogout(), 
     */
    
    public static void init(ServerConfig conf, SesNotifier ws) {

        /*
         * Called when a client session is opened. 
         * Typically a websocket session.  
         */
        ws.onOpenSes( (c)-> {
            AuthInfo a = c.authInfo();
            if (a==null || a.userid == null) 
                return;

            a.userses = a.getUserses();
            if (a.userses.increment() > 1)
                /* Session already exists and active */
                return;
                
            var closing = closingSessions.get(a.userid); 
            if (closing != null) {
                /* 
                 * If user has recently closed session and is scheduled for removal, 
                 * cancel this removal. 
                 */
                closing.cancel(false); 
                closingSessions.remove(a.userid);
            }
            else { 
                /* Inform system and other PS instances of user login */
                ((WebServer) conf.getWebserver()).notifyLogin(a.userid);
            }
        });
           
           
        /* 
         * Called when client session is closed. 
         * FIXME: This does not happen when just logging out. 
         */
        ws.onCloseSes( (c)-> {
            AuthInfo a = c.authInfo();
            if (a==null || a.userid == null)
                return;
                
            a.userses = a.getUserses(); 
            if (a.userses != null) {
                if (a.userses.decrement() <= 0) { 
                    /* 
                     * If last session (for user) is closed, schedule for removing the user 
                     * and expiring the user session - in 30 seconds. 
                     * This is cancelled if session is re-opened within 30 seconds
                     */
                    var closing = gc.schedule( () -> {    
                        /* Inform system and other PS instances of user logout. */
                        ((WebServer) conf.getWebserver()).notifyLogout(a.userid);

                        /* Put user-session on expire. Expire after 1 week */
                        a.userses.expire = (new Date()).getTime() + 1000 * 60 * USERSES_EXPIRE; 
                        gcses.add(a.userses);
                            
                        /* Remove the future */
                        closingSessions.remove(a.userid); 
                            
                    }, 30, TimeUnit.SECONDS);
                    closingSessions.put(a.userid, closing); 
                }
            }
        }); 
            

        /* Start a periodic task that expires user sessions. */
        gc.scheduleAtFixedRate( ()-> {
            while (true) {    
                if (!gcses.isEmpty() && gcses.peek().expire < (new Date()).getTime()) {
                    conf.log().info("AuthInfo", "Expired session");
                    UserSessionInfo mb = gcses.remove();
                    seslist.remove(mb.userid);   
                    if (_usersesclose != null)
                        _usersesclose.close(mb);
                }
                else
                    break;
            }
        }, 10, 10, TimeUnit.SECONDS); // Should be longer

    }
    
    
    
    public String toString() {
       return "AuthInfo [userid="+userid+", admin="+admin+", operator="+operator+", userses="+(userses== null?"-" : userses.userid)+"]";
    }
    
    
    public boolean login() 
        { return userid != null; }
       
    
        // FIXME
    public boolean isTrackerAllowed(String tr, String chan) {
        return operator || admin; 
    }

    
    
    public static Optional<CommonProfile> getSessionProfile(Context ctx) {
        return getSessionProfile(new JavalinWebContext(ctx));
    }
    
    public static Optional<CommonProfile> getSessionProfile(WebContext context) {
        final ProfileManager manager = new ProfileManager(context, new JEESessionStore()); 
        final Optional<CommonProfile> profile = manager.getProfile(CommonProfile.class);
        return profile;
    }
    
    
    
    /**
     * Authorizations. We use a kind a role-based authorization here. 
     * where some authorizations depends on role/group membership. 
     */
    public void authorize(User u, Group grp) {
        userid = u.getIdent();
        callsign = u.getCallsign();
        if (grp == null) 
            grp = u.getGroup();
        groupid = grp.getIdent();
        tagsAuth = grp.getTags();
            
        admin = u.isAdmin();
        operator = grp.isOperator(); 
    }
    
    
    /**
     * Get the user's session info. Note that there may be multiple login sessions
     * for the same user sharing the info.
     */
    public UserSessionInfo getUserses() { 
    
        /* If user-session exists on this session, just return it */
        if (userses != null) 
            return userses; 
            
        /* 
         * Try to get it from session-info list (in this class). If not found there, 
         * create a new one. 
         */
        UserSessionInfo uses = seslist.get(userid);
        if (uses==null && _usersesfactory != null) 
            uses = _usersesfactory.create(userid); 
        if (uses==null)
            uses = new UserSessionInfo(userid);
        
        seslist.put(userid, uses);
        userses = uses; 
        return uses;
    }
    
    
       
    /**
     * Constructor. Gets userid from a user profile on request and sets authorisations. 
     * called from AuthService for each request.
     */
     
    public AuthInfo(ServerConfig conf, User u, Group g) {
        _conf = conf;
        authorize(u, g);
        var i = 0;
        services = new String[_services.size()];
        for (var x : _services)
            services[i++] = x;
    }
    
    
    
    /**
     * Constructor. Gets info from web context.
     */
    public AuthInfo(ServerConfig conf, WebContext context) 
    {
        Optional<CommonProfile> profile = getSessionProfile(context);
        _conf = conf;
        var i = 0;
        services = new String[_services.size()];
        for (var x : _services)
            services[i++] = x;
        
        /* 
         * Copy user-information from the user-profile?
         * The user profile is created by the authenticator.
         */
        if (profile.isPresent()) {
            userid = profile.get().getId();
            User u = (User) profile.get().getAttribute("userInfo");
            Group grp = (Group) profile.get().getAttribute("role");
            authorize(u, grp);
            userses = getUserses();
        }

        
        servercall=conf.getProperty("default.mycall", "NOCALL");
    }
    
}
