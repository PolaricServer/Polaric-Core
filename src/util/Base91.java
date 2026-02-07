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
 * APRS Base91 is a simple encoding method used in APRS (Automatic Packet Reporting System)
 * for encoding binary data as ASCII text. It uses 91 consecutive ASCII characters
 * from '!' (ASCII 33) to '{' (ASCII 123).
 * 
 * This encoding is used in APRS for compressed position data, telemetry, and other
 * numeric values. Unlike the standard basE91 encoding scheme, APRS Base91 uses
 * straightforward base conversion rather than variable-length encoding.
 * 
 * Compatible with APRS specification.
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
     * Converts the input byte array to a base-91 number representation.
     * The encoding treats the input as a big-endian unsigned integer and
     * converts it to base 91 using the APRS character set.
     * 
     * Leading zero bytes are preserved by encoding them as '!' characters.
     * 
     * @param input The binary data to encode
     * @return APRS Base91 encoded string
     */
    public static String encode(byte[] input) {
        if (input == null || input.length == 0) {
            return "";
        }
        
        // Find the first non-zero byte
        int firstNonZero = 0;
        while (firstNonZero < input.length && input[firstNonZero] == 0) {
            firstNonZero++;
        }
        
        // If all bytes are zero, return appropriate number of '!' characters
        if (firstNonZero == input.length) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < input.length; i++) {
                result.append('!');
            }
            return result.toString();
        }
        
        // Encode leading zeros as '!' characters
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < firstNonZero; i++) {
            result.append('!');
        }
        
        // Create a working copy of the non-zero portion
        int nonZeroLength = input.length - firstNonZero;
        byte[] value = new byte[nonZeroLength];
        System.arraycopy(input, firstNonZero, value, 0, nonZeroLength);
        
        // Convert to base 91 using repeated division
        StringBuilder digits = new StringBuilder();
        boolean isZero = false;
        while (!isZero) {
            int remainder = 0;
            isZero = true;
            
            // Divide the entire byte array by 91
            for (int i = 0; i < value.length; i++) {
                int current = (remainder * 256) + (value[i] & 0xFF);
                value[i] = (byte) (current / BASE);
                remainder = current % BASE;
                
                if (value[i] != 0) {
                    isZero = false;
                }
            }
            
            // The remainder is the next base-91 digit (from right to left)
            digits.append((char) (remainder + ASCII_OFFSET));
        }
        
        // The digits are built in reverse order, so reverse and append
        result.append(digits.reverse());
        return result.toString();
    }
    
    /**
     * Decode APRS Base91 string to binary data.
     * 
     * Converts an APRS Base91 encoded string back to its original binary form.
     * The decoding treats the input as a base-91 number and converts it to bytes.
     * 
     * Leading '!' characters are decoded as zero bytes, preserving the original length.
     * 
     * @param input The APRS Base91 encoded string
     * @return Decoded binary data
     */
    public static byte[] decode(String input) {
        if (input == null || input.isEmpty()) {
            return new byte[0];
        }
        
        // Count leading '!' characters (which represent leading zero bytes)
        int leadingZeros = 0;
        while (leadingZeros < input.length() && input.charAt(leadingZeros) == '!') {
            leadingZeros++;
        }
        
        // If the entire string is '!' characters, return that many zero bytes
        if (leadingZeros == input.length()) {
            return new byte[leadingZeros];
        }
        
        // Process the non-zero portion
        String nonZeroPart = input.substring(leadingZeros);
        
        // Validate all characters and convert to base-91 digits
        int[] digits = new int[nonZeroPart.length()];
        for (int i = 0; i < nonZeroPart.length(); i++) {
            char c = nonZeroPart.charAt(i);
            if (c > 255 || DECODE_TABLE[c] == -1) {
                throw new IllegalArgumentException("Invalid Base91 character: " + c);
            }
            digits[i] = DECODE_TABLE[c];
        }
        
        // Calculate the size of the result
        int maxBytes = (int) Math.ceil(nonZeroPart.length() * 1.88 / 2) + 1;
        byte[] result = new byte[maxBytes];
        int resultLen = 0;
        
        // Convert from base 91 to base 256 (bytes)
        for (int digit : digits) {
            int carry = digit;
            
            for (int i = resultLen - 1; i >= 0; i--) {
                int val = (result[i] & 0xFF) * BASE + carry;
                result[i] = (byte) (val & 0xFF);
                carry = val >>> 8;
            }
            
            // Add any remaining carry as new leading bytes
            while (carry > 0) {
                if (resultLen >= maxBytes) {
                    byte[] newResult = new byte[maxBytes * 2];
                    System.arraycopy(result, 0, newResult, 0, resultLen);
                    result = newResult;
                    maxBytes *= 2;
                }
                
                for (int i = resultLen; i > 0; i--) {
                    result[i] = result[i - 1];
                }
                
                result[0] = (byte) (carry & 0xFF);
                carry >>>= 8;
                resultLen++;
            }
        }
        
        // Create final result with leading zeros
        byte[] finalResult = new byte[leadingZeros + resultLen];
        // Leading zeros are already 0 in Java's default initialization
        System.arraycopy(result, 0, finalResult, leadingZeros, resultLen);
        
        return finalResult;
    }
}
