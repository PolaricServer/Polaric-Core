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


/**
 * Interface to group (role) database. 
 */

public interface GroupDb 
{
    public Group get(String gid); 
    public Collection<Group> getAll();
    
    /* 
     * The default implementation is read-only from file
     * so the default behaviour of add and remove is to do 
     * nothing. 
     */
    default User add (String gid) 
         {return null;}
        
    default User add (String userid, String name, boolean operator)
         {return null;}
    
    default void remove(String gid)
         {}
}

