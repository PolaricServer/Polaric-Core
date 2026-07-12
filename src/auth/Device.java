/* 
 * Copyright (C) 2026 by Øyvind Hanssen (ohanssen@acm.org)
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
 
package no.polaric.core.auth;
import no.polaric.core.*; 
import java.util.*; 



    
public class Device extends User {
 
    private Date lastused; 

    /* We don't need these */
    @Override public Date getLastUsed()        { return lastused; }
    @Override public void setLastUsed(Date d)  { lastused = d;}
    @Override public void setPasswd(String pw) {  }
    
    @Override public void updateTime() { 
        lastused = new Date(); 
    }
    
    public Device(String id) {
        super(id); 
        updateTime();
    } 
        
}
