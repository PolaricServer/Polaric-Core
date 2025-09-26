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
import io.javalin.http.Context;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.io.PrintStream;
import java.util.*;
import java.util.function.*;
import java.io.*;
import java.text.*;
import com.mindprod.base64.Base64;
import java.util.concurrent.locks.*; 
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.NamedType;


/*
 * Abstract base class for REST API implementations, etc.. 
 * Contains some useful methods..
 */

public abstract class ServerBase 
{
   protected  ServerConfig   _conf;
   private    String         _timezone;
   private    String         _wfiledir;
   private    String         _icon;    
   private    WebServer      _ws;
   protected  Javalin        a; 
   
   public static final String _encoding = "UTF-8";

   static DateFormat df = new SimpleDateFormat("dd MMM. HH:mm",
            new DateFormatSymbols(new Locale("no")));
   static DateFormat tf = new SimpleDateFormat("HH:mm",
            new DateFormatSymbols(new Locale("no")));
   static DateFormat xf = new SimpleDateFormat("yyyyMMddHHmmss",
            new DateFormatSymbols(new Locale("no")));       
   static DateFormat isodf = 
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            
   public static Calendar utcTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault());
   public static Calendar localTime = Calendar.getInstance();
   
   
      /* Jackson JSON mapper */ 
   protected final static ObjectMapper mapper = new ObjectMapper();
   
   
   /** Serialize object to JSON */
   public static String toJson(Object obj) 
       { return serializeJson(obj); }
 
 
    /** Deserialize object from JSON */
    public static Object fromJson(String text, Class cls) 
       { return deserializeJson(text, cls); }
 
 
    /** Serialize object to JSON */
    public static String serializeJson(Object obj) {
        try {
            mapper.setDateFormat(isodf);
            return mapper.writeValueAsString(obj);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    
    
   /** Deserialize object from JSON */
   @SuppressWarnings("unchecked")
    public static Object deserializeJson(String text, Class cls) {
        try {
            return mapper.readValue(text, cls);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }
    
    /** Add a subtype to be used in JSON mapper. */
    public static void addSubtype(Class type, String name) {
        mapper.registerSubtypes(new NamedType(type, name));
    }
    
    
    public ServerBase(ServerConfig conf) 
    {
        _conf = conf; 
        _ws = (WebServer) conf.getWebserver(); 
        a = _ws.app();
      
        _wfiledir    = conf.getProperty("map.web.dir", "aprsd");
        _icon        = conf.getProperty("map.icon.default", "sym.gif");
        _timezone    = conf.getProperty("timezone", "");
                
        TimeZone.setDefault(null);
        if (_timezone.length() > 1) {
            TimeZone z = TimeZone.getTimeZone(_timezone);
            localTime.setTimeZone(z);
            df.setTimeZone(z);
            tf.setTimeZone(z);
        }
    }
   
    
    protected String icon() {
        return _icon;
    }
   
    /** Get interface to the server-config */
    protected ServerConfig conf() 
       { return _conf; }
   
   
    /** Get interface to the web-server */
    protected WebServer wServer() {
        return _ws;
    }
    
   
    protected static double roundDeg(double x)
       { return ((double) Math.round(x*100000)) / 100000; 
       }
  
  
    /** Protect an URL. Require login and the given authorization level */
    protected void protect(String prefix, String level) { 
        _ws.protectUrl(prefix, level); 
    }
    
    /** Protect an URL. Require login */
    protected void protect(String prefix) { 
        _ws.protectUrl(prefix); 
    }
    
    
   /**
    * Get info about logged-in user and authorization 
    * @return AuthInfo. 
    */
    protected AuthInfo getAuthInfo(Context ctx)
      { return AuthService.getAuthInfo(ctx); }

      
      
    /** 
     * Send a system notification to a user. 
     * TTL is in minutes 
     */
    public void systemNotification(String user, String txt, int ttl) {
        _conf.getWebserver().notifyUser(user, 
            new ServerConfig.Notification("system", "system", txt, new Date(), ttl) );  
    }

    
   /**
    * Sanitize text that can be used in HTML output. 
    */
   protected String fixText(String t)
   {  
        if (t==null)
           return t; 
        StringBuilder sb = new StringBuilder(t);
        int length = t.length();
        for (int i=0; i<length; i++) {
           switch (sb.charAt(i)) {
              case '&': 
                   if (sb.substring(i+1).matches("(amp|lt|gt|quot);.*")) 
                      i = sb.indexOf(";", i)+1;
                   else {
                      sb.insert(i+1,"amp;");
                      i+= 4;
                      length += 4;
                   }
                   break;
                
              case '<': 
                   sb.setCharAt(i,'&');
                   sb.insert(++i, "lt;");
                   length += 3;
                   break;
              case '>': 
                   sb.setCharAt(i,'&');
                   sb.insert(++i, "gt;");
                   length += 3;
                   break;
              case '"':
                   sb.setCharAt(i,'&');
                   sb.insert(++i, "quot;");
                   length += 5;
                   break;
           }        
           if (sb.charAt(i)<' ')
              sb.setCharAt(i, '?');
        }
        return sb.toString(); 
   }
   
   
    public static String cleanPath(String txt) { 
        if (txt==null)
            return "";
   
        return txt.replaceAll("((WIDE|TRACE|SAR|NOR)[0-9]*(\\-[0-9]+)?\\*?,?)|(qA.),?", "")
           .replaceAll("\\*", "").replaceAll(",+|(, )+", ", ");
    }
   
   
   protected String metaTag(String name, String val) 
      { return "<meta name=\""+name+"\" value=\""+val+"\"/>"; }
   

 
}
