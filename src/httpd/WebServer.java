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
 

package no.arctic.core.httpd; 
import no.arctic.core.*;
import no.arctic.core.auth.*;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.pac4j.core.config.Config;
import org.pac4j.javalin.*;
import java.util.*;

 

/*
 * Abstract base class for webserver setup. 
 * A comcrete application should subclass this.  
 */

public abstract class WebServer implements ServerConfig.Web {
    protected int _port;
    private Javalin _app; 
    private AuthService _auth;
    private PubSub _psub;
    private String _psuri;
    protected ServerConfig _conf;
    
    private long _nRequests = 0;
    
    
    public static class UserSessionInfo extends AuthInfo.UserSessionInfo {
        public UserSessionInfo(String uid) {super(uid);}
    }

    
    
    public WebServer(ServerConfig conf, int port, String psuri, String stpath, String stdir) {
        _port = port;
        _conf = conf;
        _psuri = psuri.trim();
        if (_psuri.charAt(0) != '/')
            _psuri = "/" + _psuri;
        
        _app = Javalin.create( config -> {
            _auth = new AuthService(conf);
        
            /* Serve static files. */
            if (stpath != null && stdir != null)
                config.staticFiles.add( sf -> {
                    sf.hostedPath = stpath;    
                    sf.directory = stdir;   
                    sf.location = Location.EXTERNAL;
                });
        }).start(_port);
    }
    
    /**
     * Start the webserver and services. 
     */
    public void start() {
        _auth.start(_app);
        _app.after(ctx -> {_nRequests++;});
                
        /* Basic REST service */
        Services ss = new Services(_conf);
        ss.start();
        
        /* 
         * Publish-subscribe service based on websocket. Two rooms for notifications 
         * are created by default: SYSTEM and ADMIN 
         */
        _psub = new PubSub(_conf);
        _psub.start(_psuri);
        pubSub().createRoom("notify:SYSTEM", false, false, false, true, ServerConfig.Notification.class);
        pubSub().createRoom("notify:ADMIN", false, false, false, true, ServerConfig.Notification.class);
        
        /* Register handlers for open and close of login-sessions. Note that there may be 
         * more than one login-session per user-session, but we need to ensure that there is only 
         * one instance of some info for each user-session. 
         */
        AuthInfo.init(_conf, _psub);
    }
    
    
    /** Stop the webserver and services */
    public void stop() throws Exception {
        _app.stop();
    }  
    
    
    /** return the pubsub service */
    public PubSub pubSub()
        { return _psub; }
        
        
    /* Statistics */
    /** Number of visits since startup. */
    public long nVisits() 
        { return (_psub==null ? 0 : _psub.nVisits()); }
    
    
    /** Number of logins since startup. */
    public long nLogins()
        { return (_psub==null ? 0 : _psub.nLogins()); }
    
    
    /** Number of clients. */
    public int  nClients() 
        { return (_psub==null ? 0 : _psub.nClients()); }
    
    
    /** Number of logged-in clients. */
    public int  nLoggedin()
        { return (_psub==null ? 0 : _psub.nLoggedIn()); }
    
    
    /** Number of http requests since startup. */
    public long nHttpReq() 
        { return _nRequests; }
    
    
    /** Return the user-database */
    public UserDb userDb()
        { return _auth.userDb(); }
    
    
    /** Return the group-database */
    public GroupDb groupDb()
        { return _auth.groupDb(); }
        
        
    /** Return the auth service */    
    public AuthService authService() 
        { return _auth; }
        
        
    /** Return a Javalin object */
    public Javalin app() 
        { return _app; }
    
    
    /**
     * Return a set of logged in users. The set is ordered.  
     */
    public SortedSet<String> loginUsers() {
        SortedSet<String> u = new TreeSet<String>();
        for (WsNotifier.Client c :_psub.clients())
            if (c.userName() != null)
                u.add(c.userName());
        return u;
    }

    
    /** Return true if the given userid is logged on to the system. */
    public boolean hasLoginUser(String user) {
        SortedSet<String> uu = loginUsers(); 
        return uu.contains(user);
    }
    
    
    /**
     * Callback for user logins. 
     * Suitable for lambda function. Multiple subscriptions allowed.
     */
    public static interface UserLogin {
        void notify(String uname);
    }
    
    private List<UserLogin> _loginCb = new LinkedList<UserLogin>();
    private List<UserLogin> _logoutCb = new LinkedList<UserLogin>();
    
    
    /** Register a handler-function to be called when user logs on to the system. */
    public void onLogin(UserLogin not) {
        _loginCb.add(not);
    }
    
    /** Register a handler-function to be called when user logs off the system. */           
    public void onLogout(UserLogin not) {
        _logoutCb.add(not);
    }
    
    
    /** 
     * Register a handler-function to be called when user-session is opened. 
     * Can be used to associate information to a user-session.
     */
    public void createUserSes(AuthInfo.SesCreateFunc f) {
        AuthInfo.setUserSesFactory(f);
    }
    
    /** 
     * Register a handler-function to be called when user-session is closed. 
     * Can be used to clean up user-session information. 
     */
    public void closeUserSes(AuthInfo.SesCloseFunc f) {
        AuthInfo.setUserSesClose(f);
    }
    
    

    /**
     * User login notification. To be called from AuthInfo class 
     */
    public void notifyLogin(String user) {
        for (UserLogin x: _loginCb)
            x.notify(user);
    }
    
    
    /**
     * User logout notification. To be called from AuthInfo class 
     */
    public void notifyLogout(String user) {
        for (UserLogin x: _logoutCb)
            x.notify(user);
    }
    
    
    
    
    /**
     * Send notification to a room. 
     */    
    public void notifyUser(String user, ServerConfig.Notification not) {
        _psub.put("notify:"+user, not);
    }
    
    
    /** 
     * Protect a URL prefix (require login) 
     */
    public void protectUrl( String prefix) {
        protectUrl(prefix, null);
    }
    
    
    
    /**
     * Protect a URL prefix. Require login and authorization level ('operator' or 'admin') 
     */
    public void protectUrl(String prefix, String level) {  
        var cli = "HeaderClient"; 
        String lvl = (level==null ? "isuser" : level);
        app().before(prefix, new SecurityHandler(_auth.conf(), cli, lvl)); 
        app().before(prefix, AuthService::getAuthInfo);
    }
    
    
    
    public void protectDeviceUrl(String prefix) {
        var cli = "HeaderClient"; 
        app().before(prefix, new SecurityHandler(_auth.conf(), cli, "device")); 
    }
    
}


