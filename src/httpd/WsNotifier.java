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
import java.util.*;
import java.util.concurrent.*;
import io.javalin.websocket.*; 
import java.util.function.*;




public abstract class WsNotifier extends ServerBase implements SesNotifier {
    
   
    /************* Client *************/
    public abstract class Client implements SesNotifier.Client {
    
        protected WsContext _ctx; 
        protected Date _ctime; 
        protected AuthInfo _auth;
        protected long _nIn, _nOut; 
        private   boolean _mobile;
        
        
        public Client(WsContext ctx) {
            _ctx = ctx;
            _ctime = new Date();
        }
      
        public void setAuthInfo(AuthInfo auth) 
            { _auth = auth; }
        
        public AuthInfo authInfo() 
            { return _auth; }
        
        public final boolean login() 
            { return _auth != null && _auth.userid != null; }
            
        public String userName()
            { return (_auth == null ? null : _auth.userid); }
         
        public String group()
            { return (_auth == null ? null : _auth.groupid); }
        
        public WsContext ctx()    { return _ctx; }
        public String host()      { return _ctx.host(); }
        public String uid()       { return _ctx.sessionId(); }
        public Date created()     { return _ctime; }
        public long nIn()         { return _nIn; }
        public long nOut()        { return _nOut; }
        public boolean isMobile() { return _mobile; }
         
        public void send(String msg) {
            if (msg == null) msg="";
            _nOut++; 
            _ctx.send(msg);
        }
        
        public void close() {
            closeSes(_ctx);
        }
        
        
        /** 
         * Handler for text frame. To be defined in subclass.
         */
        public abstract void handleTextFrame(String text);
    }
    /************* class Client *************/
    
       
       
       
    /* Count number of logged in users */
    private int _nLoggedIn;
      
   
    /* Count number of visits and logins */
    private long _visits = 0;
    private long _logins = 0;
   
   
    /* Origin site. 
     * Trusted origin sites (regular expression) 
     */
    private String _origin;
    private String _trustedOrigin; 
   
   
    /* Client sessions */
    protected final Map<WsContext, Client> _clients = new ConcurrentHashMap<>();
     
     /* Callbacks for open and close of sessions */
    private List<SHandler> _sOpen = new ArrayList<SHandler>();
    private List<SHandler> _sClose = new ArrayList<SHandler>();   
   
   
   
    public WsNotifier(ServerAPI api) {
        super(api);
        _trustedOrigin = _api.getProperty("trusted.orgin", ".*");
    }
    
         
    /** Factory method */
    public abstract Client newClient(WsContext ctx);
   
    
    /** Return number of clients. */
    public int nClients() 
        { return _clients.size(); }
    
    
    /** Return number of visits */
    public long nVisits()
        { return _visits; }
    
    
    /** Return number of logins */
    public long nLogins()
     { return _logins; }
    
    
    /** Return number of logged-in users */
    public int nLoggedIn()
        { return _nLoggedIn;}
     

    /** Return collection of clients */
    public Collection<Client> clients()
        { return _clients.values(); }
     
   
             
   /**
    * Do authentication based on URL query-string. 
    * Return an AuthInfo object if success. Return null if failure.
    * The query string have 3 or 4 parameters: (1) userid, (2) nonce, (3) hmac, 
    * (4) role (optional). When computing the hmac the data field is empty. 
    */
   public AuthInfo authenticate(String qstring) {
      String[] params = null; 
      if (qstring != null) {
         params = qstring.split(";");
         if (params.length < 3 || params.length > 4) {
            _api.log().info("WsNotifier", "Authentication failed, wrong format of query string");
            return null;
         }
      }
      try { 
         HmacAuthenticator auth = wServer().authService().hmacAuth();
         String rname = (params.length == 4 ? params[3] : null);
         User ui = auth.checkAuth(params[0], params[1], params[2], "");
         Group grp = auth.getRole(ui, rname);
         return new AuthInfo(_api, ui, grp); 
      }
      catch (Exception e) {}
      return null;
   }
   
   
    /**
     * Close the client session.
     */
    protected void closeSes(WsContext ctx) {
        if (ctx==null)
            return;
        Client c = _clients.get(ctx);
        if (c==null) {
            _api.log().warn("WsNotifier", "Close session: client "+sesId(ctx)+" not found");
            return;
        }
        _api.log().warn("WsNotifier", "Close session: "+sesId(ctx)+" ok");
        if (c.login())
            _nLoggedIn--;
        _clients.remove(ctx);
        
        /* Call any functions that are registered for handling this */
        for (SHandler h : _sClose)
            h.handle(c);
    }
    
    
    
   
   /** 
    * Websocket Connect handler. 
    * Subscribe to the service (join the room). 
    */
   
    private void openSes(WsContext ctx) {
        try {
            String qstring = ctx.queryString();
            _api.log().debug("WsNotifier", "Open session - query string: "+qstring);
          
            /* Check origin */
            _origin = ctx.header("Origin");
            if (_origin == null || _origin.matches(_trustedOrigin))
            { 
                /* Create client, autenticate and set authorization info */
                Client client = newClient(ctx);
                String[] qs = null;
                if (qstring != null) {
                    qs = qstring.split("&");
                    if ("_MOBILE_".equals(qs[0]))
                        client._mobile=true;
                }       
                client.setAuthInfo( authenticate(
                    (qstring == null ? null :  (qs.length == 1 ? qstring : qs[1]))
                ));
                 
                if (subscribe(ctx, client)) {
                    _api.log().debug("WsNotifier", "Open session accepted: "+sesId(ctx));
                    _clients.put(ctx, client); 
                    _visits++;
                 
                    /* Call any functions that are registered for handling this */
                    for (SHandler c: _sOpen)
                       c.handle(client);
                    
                    if (client.login()) {
                        _nLoggedIn++;
                        _logins++;
                    }
                }
                else {
                    _api.log().info("WsNotifier", "Open session rejected: "+sesId(ctx));
                    ctx.closeSession();
                }
            }
            else
                _api.log().info("WsNotifier", "Open session rejected. Untrusted origin='"+_origin+"'");
          
        } catch(Exception e) {
            _api.log().warn("WsNotifier", "Open session failed: " + e);
            if (e instanceof NullPointerException)
                e.printStackTrace(System.out);
        }  
    }
   
        
    
    /**
     * Register a callback for open-session events
     */
    public void onOpenSes(SHandler h) {
        _sOpen.add(h);  
    }
    
    
    /**
     * Register a callback for close-session events
     */
    public void onCloseSes(SHandler h) {
        _sClose.add(h);  
    }
    
    
    
    /**
     * Subscribe a client to the service. Should be overridden in subclass.
     * This may include authorization, preferences, etc.. 
     * @return true if subscription is accepted. False if rejected.
     */
    protected boolean subscribe(WsContext ctx, Client client) 
        { return true; }
   
   
   
   
    /**
     * Distribute a text to the clients for which the 
     * predicate evaluates to true. 
     */
    public void postText(Function<Client,String> txt, Predicate<Client> pred) {
       try {          
          /* Distribute to all clients */
          for(WsContext ctx : _clients.keySet()) {
              Client client = (Client) _clients.get(ctx);      
              if (client != null && pred.test(client) && txt != null) 
                 client.send(txt.apply(client));
          }
       } 
       catch (Exception e) {
          _api.log().error("WsNotifier", "Cannot distribute string: " + e);
          e.printStackTrace(System.out);
       }
    } 
   
   
   
    public void postText(String txt, Predicate<Client> pred) {
        postText(c->txt, pred);
    }
   
   
    public static String sesId(WsContext ctx) {
        return ctx.sessionId().substring(0,8)+"@"+ctx.host();
    }
   
   
    public void start(String uri) {    
        
        a.ws(uri, ws -> {
            ws.onConnect(ctx -> {
                _api.log().debug("WsNotifier", "Websocket connection: "+sesId(ctx));
                ctx.enableAutomaticPings();
                openSes(ctx);
            });
            
            ws.onMessage(ctx -> {
                Client c = _clients.get(ctx);    
                c._nIn++;
                c.handleTextFrame(ctx.message());
            });
            
            ws.onBinaryMessage(ctx -> {
            }); 
            
            ws.onClose(ctx -> {
                _api.log().debug("WsNotifier", "onClose: "+sesId(ctx)+", "+ctx.status()+", "+ctx.reason());
                closeSes(ctx);
            });
            
            ws.onError(ctx -> {
                _api.log().warn("WsNotifier", "onError: "+sesId(ctx)+", "+ctx.error());
            });
        });
    }
    
    
    

}
