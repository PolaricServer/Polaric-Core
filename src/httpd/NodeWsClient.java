/* 
 * Copyright (C) 2022-25 by LA7ECA, Øyvind Hanssen (ohanssen@acm.org)
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
import no.arctic.core.auth.*;
import no.arctic.core.*;
import java.io.*;
import java.net.http.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.time.Duration;



/**
 * Client side of websocket. 
 */

public class NodeWsClient implements WebSocket.Listener {
    
    private ServerConfig _conf;
    private WebSocket _wsClient; 
    private URI _url;
    private HttpClient _ht;
    private String _nodeid;
    private String _subscribe;
    private String _userid;
    private boolean _connected = false;
    private NodeWsApi.Handler<String> _handler; 
    private boolean _retry = false;
    private long _retr_int = 0;
    private Timer hb = new Timer();
    
    
    public NodeWsClient(ServerConfig conf, String nodeid, String url, boolean retry) {
        _conf=conf;
        _nodeid=nodeid;
        _retry = retry;
        try {
            _url=new URI(url);
            _ht = HttpClient.newHttpClient();
            open();
        }
        catch (URISyntaxException e) {
            _conf.log().error("NodeWsClient", "Syntax error in URI: "+_url);
        }
    }
    
    
    
    public void setUserid(String id) {
        _userid = id;
    }
    
    
    
    public void setHandler(NodeWsApi.Handler<String> h) {
        _handler = h;
    }
    
    
    
    public void open() {
        try {
            HmacAuthenticator auth = ((WebServer)_conf.getWebserver()).authService().hmacAuth();
            URI u = new URI(_url.toString() + "?" + auth.authString("", _userid)); 
            _wsClient = _ht.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .buildAsync(_url, this)
                .get();
        }
        catch (Exception e) {
            _conf.log().warn("NodeWsClient", "Websocket connect exception: "+e.getMessage());
            retry();
        }
    }
    
    
    
    public void close() {
        _wsClient.sendClose​(WebSocket.NORMAL_CLOSURE, "");
        _retry = false;
    }
    
    
    
    public boolean isConnected() {
        return _connected;
    }
    
    
    
    public boolean subscribe(String nodeid) {
        _subscribe = nodeid;
        return putCommand("SUBSCRIBE ", nodeid);
    }
    
    
    
    public void unsubscribe() {
        putCommand("UNSUBSCRIBE ", _subscribe);
        _subscribe = null;
    }
    
    
    
    public boolean putCommand(String cmd, String msg) {
        _conf.log().debug("NodeWsClient", "Post message: "+cmd);

        if (!_connected) {
            _conf.log().warn("NodeWsClient", "Node not connected: "+_url);
            return false;
        }
        /* Send it on websocket */
        try {
            _wsClient.sendText(cmd + msg, true).join();
        }
        catch (CompletionException e) {
            Throwable cause = e.getCause();
            _conf.log().warn("NodeWsClient", "Message delivery failed: "+cause);
            return false;
        }
        return true;
    }
        
    
    public boolean putText(String msg) {
        return putCommand("POST ", msg);
    }
    
                
    /** Post a object to the connected node (JSON encoded) */
    public boolean put(Object obj) 
        { return putText(ServerBase.toJson(obj)); }
        
        
    
    @Override
   public void onError​(WebSocket webSocket, Throwable error) {
        _conf.log().warn("NodeWsClient", "Error: "+error);
        error.printStackTrace(System.out);
        _connected = false;
        _retr_int = 60000 * 8; // Retry after 16 minutes
        retry();
    }

    
    
    @Override
    public void onOpen(WebSocket webSocket) {       
        webSocket.request(1); 
        _retr_int = 0;
        _connected = true;
    }
    
    
    
    @Override
    public CompletionStage<?> onClose​(WebSocket webSocket, int statusCode, String reason) {
        _conf.log().debug("NodeWsClient", "Connection closed. Statuscode: "+ statusCode + " "+reason);
        _connected = false;
        retry();
        return null;
    }
    
    
    private void retry() {
        if (_retry)
            hb.schedule( new TimerTask() 
                { public void run() {       
                    if (!_connected) open();    
                    if (_connected && _subscribe != null)
                        subscribe(_subscribe);
                } 
            }, retr_delay() ); 
    }
    
    
    
    /* Semi-exponential backoff for retries */ 
    private long retr_delay() {
        if (_retr_int == 0)
            _retr_int = 60000;     // Min 60 seconds
        else
            _retr_int = _retr_int * 2;
        if (_retr_int >= 3600000) 
            _retr_int = 3600000;   // Max 1 hour
        return _retr_int;
    }
    
    
    
    @Override
    public CompletionStage<?> onText​(WebSocket webSocket, CharSequence data, boolean last) {
        _conf.log().debug("NodeWsClient", "Received message: "+_nodeid);
        if (_handler != null) {
            String[] parms = data.toString().split(" ", 2);
            if (parms.length < 2) { 
                if (parms.length == 0 || !parms[0].equals("PING"))
                    _conf.log().warn("NodeWsClient", "Format error in message");
            }
            else if (parms[0].equals("POST")) {
                _handler.recv(_nodeid, parms[1]);
            }
        }
        _retr_int = 0;
        webSocket.request(1);
        return null;
    }
    
}

































































































