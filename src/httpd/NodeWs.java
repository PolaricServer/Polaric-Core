/* 
 * Copyright (C) 2022-2025 by LA7ECA, Øyvind Hanssen (ohanssen@acm.org)
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.io.IOException;
import java.net.*;
import java.util.function.*;
import io.javalin.websocket.*; 


/**
 * WebSocket communiction between Polaric Server backend nodes. 
 */

public class NodeWs extends WsNotifier 
{
    private NodeWsApi.Handler<String> _handler;
    private Timer hb = new Timer();
    
    
    public class Client extends WsNotifier.Client
    {   
        public Client(WsContext conn) { 
            super(conn); 
        }
             
        public String nodeid;
      

      
      
        @Override synchronized public void handleTextFrame(String text) {
            String[] parms = text.split(" ", 2);
            if (parms.length < 2) {
                _api.log().warn("NodeWs", "Format error in message");
                close();
            }
            else
            switch (parms[0]) {
                /* subscribe: 
                 * arguments: ident
                 */
                case "SUBSCRIBE":
                case "SUB":
                    nodeid = parms[1];
                    _subscribers.put(parms[1], this);
                    break;
                   
                 /* unsubscribe:
                 * arguments: ident
                 */
                case "UNSUBSCRIBE":
                case "UNSUB":
                    _subscribers.remove(parms[1]);
                    break;
                    
                /* post
                 * arguments: JSON-encoded content
                 */
                case "POST": 
                case "MSG":
                    if (_handler != null) _handler.recv(nodeid, parms[1]);
                    break;
            
                default: 
                    break;
            }
        }
    }
   
   
    private HashMap<String, Client> _subscribers = new HashMap<String,Client>(); 
    
            
        
    public NodeWs(ServerAPI api, NodeWsApi.Handler<String> hdl) { 
        super(api); 
        _handler = hdl;
        
        hb.schedule( new TimerTask() { 
            public void run() {
                for (Client c : _subscribers.values())
                    putText(c.nodeid, null); // PING each node every 2 minutes
            } 
        }, 120000, 120000); 
    }  
   
   
   
    public Set<String> getSubscribers() {
        return _subscribers.keySet();
    }
    
    
    public void removeSubscriber(String id) {
        _subscribers.remove(id);
    }
    
    
    public void setHandler(NodeWsApi.Handler<String> h) {
        _handler = h;
    }
    
   
    /**
     * Websocket close handler.
     */
  //  public void onClose(WsContext conn, int statusCode, String reason) {
  //     String user = _getUid(conn);
  //     closeSes(conn);
  //     Client c = (Client) _clients.get(user);
  //     _subscribers.remove(c.nodeid);
  //  }
   
    
    
    /** 
      * Post a message to node. Returns true if sending was successful.
      */
    public boolean putText(String nodeid, String msg) {
        _api.log().debug("NodeWs", "Post message to: "+nodeid);
        
        Client client = (Client) _subscribers.get(nodeid);
        if (client == null) {
            _api.log().warn("NodeWs", "Node not connected: "+nodeid);
            return false;
        }
      
        if (msg == null) 
            client.send("PING");
        else    
            client.send("POST " + msg);
        return true;
    }
        
            
    /** Post a object to a node (JSON encoded) */
    public boolean put(String nodeid, Object obj) 
        { return putText(nodeid, toJson(obj)); }
        

    
    /** Factory method. */
    @Override public WsNotifier.Client newClient(WsContext conn) 
        { return new Client(conn); }


}
