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


package no.polaric.core.util;
import java.util.*;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;
import java.net.*;


public class ZeroConf 
{ 

    private List<JmDNS> _ifaces  = new ArrayList<JmDNS>();;
    
    public ZeroConf()
    {
        try {
            Enumeration<NetworkInterface> b = NetworkInterface.getNetworkInterfaces();
            while( b.hasMoreElements()) {
                NetworkInterface nif = b.nextElement(); 
                if (!nif.isUp() || nif.isLoopback() || nif.isPointToPoint() 
                      || nif.isVirtual() || nif.getName().matches("docker.*"))
                    continue;

                for ( InterfaceAddress f : nif.getInterfaceAddresses()) {
                    InetAddress addr = f.getAddress();
                    if ( addr.isSiteLocalAddress() && !addr.isMulticastAddress() && !addr.isLinkLocalAddress() ) {
                        JmDNS dd = JmDNS.create(addr);
                        _ifaces.add(dd);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    public void registerMdns(String type, String name, int port, String attrs) 
    {
        try {
            for (JmDNS d : _ifaces) {
                ServiceInfo serviceInfo = ServiceInfo.create(type, name, port, attrs);
                d.registerService(serviceInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }        
    }
    
    public void unregisterMdns() {
        try {
            for (JmDNS d : _ifaces)
                d.unregisterAllServices();
        } catch (Exception e) {
            e.printStackTrace();
        }      
    }
}
