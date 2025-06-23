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
import java.io.Serializable;


/**
 * Group class. A group represents a role. Implemented with local file or database
 */
public class Group implements Serializable {
 
    private String  _groupid; 
    private String  _name = "";
    private boolean _operator;
    private String  _tags; 
        
    public String  getIdent()             { return _groupid; }
    public void    setName(String n)      { _name = n; }
    public String  getName()              { return _name; }
    public String  getTags()              { return _tags; }
    public void    setTags(String t)      { _tags = t; }
    
    public boolean isOperator()                { return _operator; }
    public final void setOperator(boolean op)  { _operator=op; }
    
    public Group(String id, String n, String t, boolean s){
        _groupid = id; 
        _name = n;
        _tags = t; 
        _operator = s; 
    }

    public static Group DEFAULT = new Group("DEFAULT", "No group", null, false);
}
