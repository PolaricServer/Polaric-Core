/* 
 * Copyright (C) 2009-2025 by LA7ECA, Øyvind Hanssen (ohanssen@acm.org)
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

/**
 * Base91 encoding/decoding implementation.
 * 
 * basE91 is an advanced method for encoding binary data as ASCII text.
 * It achieves better efficiency than Base64 by using 91 printable ASCII characters.
 * 
 * Based on the basE91 encoding scheme by Joachim Henke.
 * http://base91.sourceforge.net/
 */
public class Base91 {
    
    /**
     * Base91 alphabet - all printable ASCII characters except: - (minus), \ (backslash), and ' (apostrophe)
     */
    private static final char[] ENCODE_TABLE = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '!', '#', '$', '%', '&', '(', ')', '*', '+', ',', '.', '/', ':',
        ';', '<', '=', '>', '?', '@', '[', ']', '^', '_', '`', '{', '|',
        '}', '~', '"'
    };
    
    /**
     * Decoding table - maps ASCII characters to their Base91 values
     */
    private static final byte[] DECODE_TABLE = new byte[256];
    
    static {
        // Initialize decode table with invalid value
        for (int i = 0; i < 256; i++) {
            DECODE_TABLE[i] = -1;
        }
        // Fill in valid characters
        for (int i = 0; i < ENCODE_TABLE.length; i++) {
            DECODE_TABLE[ENCODE_TABLE[i]] = (byte) i;
        }
    }
    
    /**
     * Encode binary data to Base91 string.
     * 
     * @param input The binary data to encode
     * @return Base91 encoded string
     */
    public static String encode(byte[] input) {
        if (input == null || input.length == 0) {
            return "";
        }
        
        StringBuilder output = new StringBuilder();
        int ebq = 0;  // encoder bit queue
        int en = 0;   // number of bits in encoder bit queue
        
        for (byte b : input) {
            ebq |= (b & 0xFF) << en;
            en += 8;
            
            if (en > 13) {
                int ev = ebq & 8191;  // 13 bits
                
                if (ev > 88) {
                    ebq >>= 13;
                    en -= 13;
                } else {
                    ev = ebq & 16383;  // 14 bits
                    ebq >>= 14;
                    en -= 14;
                }
                
                output.append(ENCODE_TABLE[ev % 91]);
                output.append(ENCODE_TABLE[ev / 91]);
            }
        }
        
        // Process remaining bits
        if (en > 0) {
            output.append(ENCODE_TABLE[ebq % 91]);
            if (en > 7 || ebq > 90) {
                output.append(ENCODE_TABLE[ebq / 91]);
            }
        }
        
        return output.toString();
    }
    
    /**
     * Decode Base91 string to binary data.
     * 
     * @param input The Base91 encoded string
     * @return Decoded binary data
     */
    public static byte[] decode(String input) {
        if (input == null || input.isEmpty()) {
            return new byte[0];
        }
        
        int[] output = new int[input.length()];
        int outputLen = 0;
        int dbq = 0;  // decoder bit queue
        int dn = 0;   // number of bits in decoder bit queue
        int dv = -1;  // decoder value
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c > 255 || DECODE_TABLE[c] == -1) {
                continue;  // Skip invalid characters
            }
            
            if (dv == -1) {
                dv = DECODE_TABLE[c];
            } else {
                dv += DECODE_TABLE[c] * 91;
                dbq |= dv << dn;
                
                if ((dv & 8191) > 88) {
                    dn += 13;
                } else {
                    dn += 14;
                }
                
                do {
                    output[outputLen++] = dbq & 0xFF;
                    dbq >>= 8;
                    dn -= 8;
                } while (dn > 7);
                
                dv = -1;
            }
        }
        
        // Process remaining bits
        if (dv != -1) {
            output[outputLen++] = (dbq | dv << dn) & 0xFF;
        }
        
        // Convert to byte array
        byte[] result = new byte[outputLen];
        for (int i = 0; i < outputLen; i++) {
            result[i] = (byte) output[i];
        }
        
        return result;
    }
}
