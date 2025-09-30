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

package no.polaric.core;
import java.util.*;
 

/** Interface for server configuration and management. 
 *  To be implemented by specific application-implementations.  
 */
 
public interface ServerConfig
{ 

    @FunctionalInterface 
    public interface SimpleCb {
        void cb(); 
    }
    
    /** Interface to user database */
    public interface UserDb {
        public boolean hasUser(String id); 
    } 
    
   /** 
    * Notifcation content class. 
    */
    public class Notification {
        public String type; 
        public String from;
        public String text; 
        public Date time; 
        public int ttl;
        public Notification (String t, String frm, String txt, Date tm, int tt)
            { type = t; from = frm; text=txt; time=tm; ttl = tt; }
    }
        
    /** Interface to publish-subscribe service (based on websocket) */
    public interface PubSub {
        /** Post a message to a room (text is prefixed with the room name) */
        public void putText (String rid, String msg);
        
        /** Post a object to a room (JSON encoded) */
        public void put(String rid, Object obj);
        
        /** Post a object to a room owned by a user (JSON encoded) */
        public void put(String rid, Object obj, String userid);
        
        /** Create a room */
        public void createRoom(String name, Class cl); 
        
        /** Create a room with privileges */
        public void createRoom(String name, boolean lg, boolean operator, boolean adm, boolean post, Class cl);
    }
    
    
    /** 
     * Interface to web server. 
     * FIXME: Consider subtyping this in http package. 
     */
    public interface Web {
        public long nVisits();
        public long nLogins(); 
        public int  nClients();
        public int  nLoggedin();
        public long nHttpReq(); 
        
        public UserDb userDb();
        public PubSub pubSub();
        public void notifyUser(String user, Notification not);
        public void protectUrl(String prefix);
        public void protectUrl(String prefix, String level);
        public void protectDeviceUrl(String prefix);
        public void start() throws Exception; 
        public void stop() throws Exception;
            
    }

    /** 
     * Plugin properties.
     * @return A map of properties (name,value) to be used by plugins. 
     */
     // FIXME: Rename to make it clear that it is plugin props
    public Map<String, Object> properties();
    

    
    /** 
     * Get configuration properties.
     * @return Properties object. 
     */
    public Properties config();
   
    
    /** Set string configuration property. 
     * @param pname property name.
     * @param dval default value. 
     */
    public void setProperty(String pname, String dval);
    
    
    /** Get string configuration property. 
     * @param pname property name.
     * @param dval default value. 
     * @return The value of the property (or dval if not registered)
     */
    public String getProperty (String pname, String dval); 
    
    
    /** Get boolean configuration property. 
     * @param pname property name.
     * @param dval default value. 
     * @return The value of the property (or dval if not registered)
     */
    public boolean getBoolProperty (String pname, boolean dval);
       
   
    /** Get integer configuration property. 
     * @param pname property name.
     * @param dval default value. 
     * @return The value of the property (or dval if not registered)
     */
    public int getIntProperty (String pname, int dval);
   
   
    /**
     * Get position (lat, long) configuration property.
     * @param pname property name.
     * @return [lat, long] coordinate
     */
    public double[] getPosProperty(String pname);
    
    
    /** Use logfile */
    public Logfile log();

    /** Get webserver interface */
    public Web getWebserver(); 
        
        
    /**
     * Add shutdown handler function. 
     */
    public void addShutdownHandler(SimpleCb cb);
}
