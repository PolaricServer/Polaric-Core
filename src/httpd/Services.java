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


public class Services extends ServerBase {
    
    public Services(ServerAPI api) {
        super(api);
    }
    
    
    public static class GroupInfo {
        public String ident; 
        public String name; 
        public boolean avail;
        public GroupInfo() {}
        public GroupInfo(String id, String n, boolean av) 
            { ident=id; name=n; avail=av;}
    }
    
    
    
    protected boolean groupAllowed(Group g, User u, boolean includedef) {
        if (u==null)
            return false;
        String group =  u.getGroup().getIdent();
        String altgroup =  u.getAltGroup().getIdent();
        boolean usealt = (!includedef && altgroup.equals("DEFAULT"));
        
        return  u.isAdmin() 
                    || g.getIdent().equals(group) 
                    || usealt && g.getIdent().equals(altgroup) 
                    || (includedef && g.getIdent().equals("DEFAULT"));
    }
    
    
    
    public void start() {

        protect("/groups"); 
         
        
        /******************************************
         * Get a list of groups. 
         ******************************************/
         
        a.get("/groups",  ctx -> {
            List<GroupInfo> gl = new ArrayList<GroupInfo>();
            var uid = getAuthInfo(ctx).userid; 
            User u = wServer().userDb().get(uid);
            
            for (Group g: wServer().groupDb().getAll())  
                gl.add(new GroupInfo(g.getIdent(), g.getName(), 
                  groupAllowed(g, u,false) ));
            
            wServer().pubSub().createRoom("auth:"+uid, null);
            ctx.json(gl); 
        });
        
        
    }
    

}
