/* 
 * Copyright (C) 2025-2026 by LA7ECA, Øyvind Hanssen (ohanssen@acm.org)
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
import java.util.Date;


/**
 * Minimal {@link User} implementation for authenticated devices (IoT / peer
 * Polaric instances).  Devices authenticate via Arctic-HMAC but have no
 * persistent user record, so most user-management operations are no-ops.
 */
public class Device extends User {

    public Device(String id) {
        super(id);
    }

    @Override
    public Date getLastUsed() { return null; }

    @Override
    public void setLastUsed(Date d) {}

    @Override
    public void updateTime() {}

    @Override
    public void setPasswd(String pw) {}
}
