/* 
 * Copyright (C) 2025-2026 by LA7ECA, Øyvind Hanssen (ohanssen@acm.org)
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
 

package no.polaric.core.httpd; 
import no.polaric.core.*;
import no.polaric.core.auth.*;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.router.JavalinDefaultRoutingApi;
import org.pac4j.core.config.Config;
import org.pac4j.javalin.*;
import java.util.*;
import java.util.function.Consumer;


/**
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
    private String _stpath, _stdir;
    private final List<Consumer<JavalinDefaultRoutingApi>> _routeContributors = new ArrayList<>();
    
    private long _nRequests = 0;
    
    
    public static class UserSessionInfo extends AuthInfo.UserSessionInfo {
        public UserSessionInfo(String uid) {super(uid);}
    }

    
    /** 
     * Constructor for Webserver.
     * @param conf ServerConfig objecct
     * @param port Server port 
     * @param psuri URL to serve 
     * @param stpath Path for serving static files
     * @param stdir Directory where static files are to be found. 
     */
    public WebServer(ServerConfig conf, int port, String psuri, String stpath, String stdir) {
        _port = port;
        _conf = conf;
        _psuri = psuri.trim();
        if (_psuri.charAt(0) != '/')
            _psuri = "/" + _psuri;
        _stpath = stpath;
        _stdir = stdir;
        _auth = new AuthService(conf);
    }
    
    
    
    /**
     * Register a route contributor to be applied during Javalin creation.
     * Must be called before {@link #start()}.
     */
    public void addRoutes(Consumer<JavalinDefaultRoutingApi> contributor) {
        _routeContributors.add(contributor);
    }
    
    
    /**
     * Start the webserver and services. 
     */
    public void start() {
        _auth.addRoutes(this);
        addRoutes(r -> r.after(ctx -> { _nRequests++; }));
                
        /* Basic REST service */
        Services ss = new Services(_conf);
        ss.start();
        
        /* 
         * Publish-subscribe service based on websocket. Two rooms for notifications 
         * are created by default: SYSTEM and ADMIN 
         */
        _psub = new PubSub(_conf);
        _psub.start(_psuri);
        
        /* 
         * Hook for subclasses to register application routes before the Javalin app
         * is created.  Override {@link #setupRoutes()} instead of registering routes
         * after {@code super.start()} — routes added after Javalin.create() are ignored.
         */
        setupRoutes();
        
        /* Create and start Javalin with all registered route contributors. */
        _app = Javalin.create(config -> {
            if (_stpath != null && _stdir != null)
                config.staticFiles.add(sf -> {
                    sf.hostedPath = _stpath;
                    sf.directory = _stdir;
                    sf.location = Location.EXTERNAL;
                });
            _routeContributors.forEach(c -> c.accept(config.routes));
        }).start(_port);

        pubSub().createRoom("notify:SYSTEM", false, false, false, true, ServerConfig.Notification.class);
        pubSub().createRoom("notify:ADMIN", false, false, false, true, ServerConfig.Notification.class);
        
        /* Register handlers for open and close of login-sessions. Note that there may be 
         * more than one login-session per user-session, but we need to ensure that there is only 
         * one instance of some info for each user-session. 
         */
        AuthInfo.init(_conf, _psub);
    }
    
    
    /**
     * Hook called by {@link #start()} immediately before the Javalin application is
     * created.  Subclasses should override this method to register all application-level
     * HTTP routes (via {@code a.get(...)}, {@code a.post(...)}, etc.) and any
     * additional route contributors.  Do <em>not</em> register routes after
     * {@code super.start()} returns — they will be silently ignored.
     */
    protected void setupRoutes() {}
    
    
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
        for (String c :_auth.getUserLogins())
            u.add(c);
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
        addRoutes(r -> {
            r.before(prefix, new SecurityHandler(_auth.conf(), cli, lvl));
            r.before(prefix, AuthService::getAuthInfo);
        });
    }
    
    
    
    public void protectDeviceUrl(String prefix) {
        var cli = "HeaderClient"; 
        addRoutes(r -> r.before(prefix, new SecurityHandler(_auth.conf(), cli, "device")));
    }
    
}


