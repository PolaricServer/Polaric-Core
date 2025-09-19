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

package no.arctic.core;
import java.util.*;
 
 
public interface ServerConfig
{ 

    @FunctionalInterface 
    public interface SimpleCb {
        void cb(); 
    }
    
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
        
    
    public interface PubSub {
        /** Post a message to a room (text is prefixed with the room name) */
        public void putText (String rid, String msg);
        
        /** Post a object to a room (JSON encoded) */
        public void put(String rid, Object obj);
        public void put(String rid, Object obj, String userid);
        
        /** Create a room */
        public void createRoom(String name, Class cl); 
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

    /** Plugin properties */
    public Map<String, Object> properties();
    
    
    /** Get string configuration property. 
     * @param pn property name.
     * @param dval default value. 
     */
    public String getProperty (String pn, String dval); 
    
    
    /** Get boolean configuration property. 
     * @param pn property name.
     * @param dval default value. 
     */
    public boolean getBoolProperty (String pn, boolean dval);
       
   
    /** Get integer configuration property. 
     * @param pn property name.
     * @param dval default value. 
     */
    public int getIntProperty (String pn, int dval);
   
   
    /**
     * Get position (lat, long) configuration property.
     */
    public double[] getPosProperty(String pname);
    
    
    public Logfile log();
    public Web getWebserver(); 
        
    /**
     * Add shutdown handler function. 
     */
    public void addShutdownHandler(SimpleCb cb);
}
