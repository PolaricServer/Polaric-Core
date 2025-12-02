/* 
 * Copyright (C) 2022-2025 by LA7ECA, Øyvind Hanssen (ohanssen@acm.org)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
 package no.polaric.core.auth; 
 import no.polaric.core.*;
 import no.polaric.core.httpd.*;
 import net.cinnom.nanocuckoo.NanoCuckooFilter;

 /**
  * Duplicate checker for nonces. 
  * Uses two Cuckoo filters. When the primary filter is full, move it to secondary and
  * start with a new empty primary filter. Check both filters when testing for duplicates. 
  * https://en.wikipedia.org/wiki/Cuckoo_filter
  */
 public class DuplicateChecker {
    private NanoCuckooFilter _cfilter1;
    private NanoCuckooFilter _cfilter2; 
    private int _maxcap;
    private int _fill;
    private int _num;
    
    public DuplicateChecker(int capacity) {
        _fill = capacity/2+1;
        _maxcap = (int) Math.round(_fill * 1.3);
        _cfilter1 = new NanoCuckooFilter.Builder( _maxcap ).withFingerprintBits(16).build();
        _cfilter2 = null;
    }
    
    public void add(String val) {
        if (_num++ >= _fill) {
            _cfilter2 = _cfilter1;
            _cfilter1 = new NanoCuckooFilter.Builder( _maxcap ).withFingerprintBits(16).build();
        }
        _cfilter1.insert(val);
    }
    
    public boolean contains(String val) {
        return (_cfilter1.contains(val) || 
          (_cfilter2 != null && _cfilter2.contains(val)));
    }
    
 }
