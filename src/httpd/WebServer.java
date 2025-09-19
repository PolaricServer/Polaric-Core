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
        });
    }
    
    
    public void start() {
        _app.start(_port);
        _auth.start(_app);
        _app.after(ctx -> {_nRequests++;});
                
        /* Basic REST service */
        Services ss = new Services(_conf);
        ss.start();
        
        /* Publish-subscribe service based on websocket */
        _psub = new PubSub(_conf);
        _psub.start(_psuri);
        
        /* Register handlers for open and close of login-sessions. Note that there may be 
         * more than one login-session per user-session, but we need to ensure that there is only 
         * one instance of some info for each user-session. 
         */
        AuthInfo.init(_conf, _psub);
    }
    
    
    public void stop() throws Exception {
        _app.stop();
    }  
           
           
    public PubSub pubSub()
        { return _psub; }
        
        
    /* Statistics */
    public long nVisits() 
        { return _psub.nVisits(); }
        
        
    public long nLogins()
        { return _psub.nLogins(); }
        
                
    public int  nClients() 
        { return _psub.nClients(); }
        
    
    public int  nLoggedin()
        { return _psub.nLoggedIn(); }
    
    
    public long nHttpReq() 
        { return _nRequests; }
    
    
    public UserDb userDb()
        { return _auth.userDb(); }
    
    
    public GroupDb groupDb()
        { return _auth.groupDb(); }
        
        
    public AuthService authService() 
        { return _auth; }
        
    
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
    
    public void onLogin(UserLogin not) {
        _loginCb.add(not);
    }
    public void onLogout(UserLogin not) {
        _logoutCb.add(not);
    }
    public void createUserSes(AuthInfo.SesCreateFunc f) {
        AuthInfo.setUserSesFactory(f);
    }
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


