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
 * APRS Base91 encoding/decoding implementation.
 * 
 * APRS Base91 (basE91) is an encoding method used in APRS (Automatic Packet Reporting System)
 * for encoding binary data as ASCII text. It uses 91 consecutive ASCII characters
 * from '!' (ASCII 33) to '{' (ASCII 123).
 * 
 * This implementation uses the basE91 streaming algorithm which encodes binary data
 * efficiently using variable-length encoding with a bit queue. It processes 13 bits
 * at a time, encoding them as two base-91 characters.
 * 
 * This encoding is used in APRS for compressed position data, telemetry, and other
 * binary data that needs efficient ASCII representation.
 * 
 * Compatible with the basE91 specification and APRS requirements.
 */
public class Base91 {
    
    /**
     * APRS Base91 alphabet - ASCII characters from 33 ('!') to 123 ('{')
     * Characters: !"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvwxyz{
     */
    private static final int BASE = 91;
    private static final int ASCII_OFFSET = 33;  // Start at '!' (ASCII 33)
    
    /**
     * Decoding table - maps ASCII characters to their Base91 values
     */
    private static final byte[] DECODE_TABLE = new byte[256];
    
    static {
        // Initialize decode table with invalid value
        for (int i = 0; i < 256; i++) {
            DECODE_TABLE[i] = -1;
        }
        // Fill in valid characters (ASCII 33-123)
        for (int i = 0; i < BASE; i++) {
            DECODE_TABLE[ASCII_OFFSET + i] = (byte) i;
        }
    }
    
    /**
     * Encode binary data to APRS Base91 string.
     * 
     * Uses the basE91 streaming algorithm which processes input bytes through
     * a bit queue. When enough bits are accumulated (>12), it extracts 13 bits
     * and encodes them as two base-91 characters. This provides efficient
     * variable-length encoding of binary data.
     * 
     * @param input The binary data to encode
     * @return APRS Base91 encoded string
     */
    public static String encode(byte[] input) {
        if (input == null || input.length == 0) {
            return "";
        }
        
        StringBuilder output = new StringBuilder();
        int nbits = 0;
        long bqueue = 0;
        
        for (byte b : input) {
            nbits += 8;
            bqueue |= ((long)(b & 0xFF)) << (32 - nbits);
            
            if (nbits > 12) {  // enough bits in queue
                int val = (int)((bqueue >> (32 - 13)) & 0x1FFF);
                bqueue <<= 13;
                nbits -= 13;
                output.append((char)(val / BASE + ASCII_OFFSET));
                output.append((char)(val % BASE + ASCII_OFFSET));
            }
        }
        
        // Finish the queue with remaining bits
        if (nbits > 6) { // put remaining into 2 more bytes
            int val = (int)((bqueue >> (32 - 13)) & 0x1FFF);
            output.append((char)(val / BASE + ASCII_OFFSET));
            output.append((char)(val % BASE + ASCII_OFFSET));
        } else if (nbits > 0) { // need 1 more byte
            int val = (int)((bqueue >> (32 - 6)) & 0x3F);
            output.append((char)(val % BASE + ASCII_OFFSET));
        }
        
        return output.toString();
    }
    
    /**
     * Decode APRS Base91 string to binary data.
     * 
     * Uses the basE91 streaming algorithm to decode. Processes characters in pairs,
     * converting each pair back to 13 bits of data. Handles final odd character
     * by decoding it as 6 bits.
     * 
     * @param input The APRS Base91 encoded string
     * @return Decoded binary data
     */
    public static byte[] decode(String input) {
        if (input == null || input.isEmpty()) {
            return new byte[0];
        }
        
        // Allocate output buffer - decoded data is typically smaller than encoded
        byte[] output = new byte[input.length()];
        int n = 0;
        int val = -1;
        int nbits = 0;
        long bqueue = 0;
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            // Validate and decode character
            if (c > 255 || DECODE_TABLE[c] == -1) {
                throw new IllegalArgumentException("Invalid Base91 character: " + c);
            }
            int d = DECODE_TABLE[c];
            
            if (val == -1) {
                val = d; // start next value
            } else {
                val = val * BASE + d;
                nbits += 13;
                bqueue |= ((long)val) << (32 - nbits);
                
                // Extract complete bytes from the queue
                while (nbits > 7) {
                    output[n++] = (byte)((bqueue >> (32 - 8)) & 0xFF);
                    bqueue <<= 8;
                    nbits -= 8;
                }
                val = -1; // mark value complete
            }
        }
        
        // Handle final odd character if present
        if (val != -1) {
            nbits += 6;
            bqueue |= ((long)val) << (32 - nbits);
            output[n++] = (byte)((bqueue >> (32 - 8)) & 0xFF);
        }
        
        // Return properly sized result array
        byte[] result = new byte[n];
        System.arraycopy(output, 0, result, 0, n);
        return result;
    }
}
