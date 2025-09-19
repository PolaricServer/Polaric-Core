/* 
 * Copyright (C) 2021-2025 by Øyvind Hanssen (ohanssen@acm.org)
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
 
package no.arctic.core.auth;
import no.arctic.core.*; 
import no.arctic.core.httpd.*;
import java.util.*; 
import java.io.*;


/**
 * Group (role) info is manually written in a file. Here we read this file. 
 */
public class LocalGroups implements GroupDb
{
    private Map<String, Group> _map = new HashMap<String,Group>();

    public Group get(String gid) { 
        return _map.get(gid);
    }
        
    public Collection<Group> getAll()
        { return _map.values(); }

        
    public LocalGroups(ServerConfig conf, String file) 
    {
        try {
            _map.put("DEFAULT", Group.DEFAULT);
            BufferedReader rd = new BufferedReader(new FileReader(file));
            while (rd.ready())
            {
                String line = rd.readLine();
                if (!line.startsWith("#")) 
                {               
                    String[] x = line.split(",");  
                    if (x.length < 4) 
                        continue;
                       
                    String gid = x[0].trim();
                    String name = x[1].trim();   
                    String tags = x[2].trim();
                    boolean operator = ("true".equals(x[3].trim()));
                    Group g = new Group(gid, name, tags, operator);
                    _map.put(gid, g);
                }
            }     
        }
        catch (FileNotFoundException  e) 
            { conf.log().error("LocalGroups", "No groups file present."); }
        catch (Exception  e) 
            { conf.log().error("LocalGroups", ""+e); }        
    }
    
}

