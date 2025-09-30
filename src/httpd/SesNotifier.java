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


package no.polaric.core.httpd;
import no.polaric.core.auth.*;


public interface SesNotifier {
    public interface SHandler {
        public void handle(Client c);
    }
    
    public interface Client {
        public AuthInfo authInfo();
    }
    
    public void onOpenSes(SHandler h);
    public void onCloseSes(SHandler h);
    
    
}
